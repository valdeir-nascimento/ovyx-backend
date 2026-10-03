package io.github.ovyx.identity.application.recovery;

import static io.github.ovyx.identity.application.recovery.RecoveryFakes.FIRST_TOKEN;
import static io.github.ovyx.identity.application.recovery.RecoveryFakes.SECOND_TOKEN;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.application.RecordingAccessEventRecorder;
import io.github.ovyx.identity.application.recovery.RecoveryFakes.RecordingRecoveryThrottle;
import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.model.AccessOutcome;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.fixtures.InMemoryCaretakerRepository;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A conferência do link (US2 da 012), quando a tela de redefinição abre: diz se o link vale, sem gastá-lo. Todo link
 * que não vale recebe a mesma recusa, e a causa fica na auditoria.
 */
@DisplayName("VerifyRecoveryLinkCommandHandler")
class VerifyRecoveryLinkCommandHandlerTest {

    private static final String ORIGIN = "203.0.113.42";
    private static final String EMAIL = "marina.costa@ovyx.com.br";

    private final InMemoryCaretakerRepository repository = new InMemoryCaretakerRepository();
    private final RecordingAccessEventRecorder recorder = new RecordingAccessEventRecorder();
    private final FixedClock clock = FixedClock.at("2026-10-02T12:00:00Z");
    private final RecordingRecoveryThrottle throttle = new RecordingRecoveryThrottle();
    private final VerifyRecoveryLinkCommandHandler handler =
            new VerifyRecoveryLinkCommandHandler(repository, throttle, recorder, clock);

    private Caretaker withLink() {
        Caretaker marina = aCaretaker().withEmail(EMAIL).build();
        marina.issuePasswordRecovery(FIRST_TOKEN, clock);
        repository.save(marina);
        return marina;
    }

    private static void assertInvalidLink(Result<Void> result) {
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().code()).isEqualTo("RECOVERY_LINK_INVALID");
        assertThat(result.error().message())
                .isEqualTo("Este link de recuperação não vale mais. Peça um novo na tela de entrada.");
        assertThat(result.error().details()).isEmpty();
    }

    @Test
    @DisplayName("accepts a valid link without recording anything nor spending it")
    void givenValidLink_whenVerifying_thenSucceedWithoutRecordingAnything() {
        // given
        Caretaker marina = withLink();

        // when
        Result<Void> result = handler.handle(new VerifyRecoveryLinkCommand(FIRST_TOKEN.value(), ORIGIN));

        // then
        assertThat(result.isFailure()).isFalse();
        assertThat(recorder.recorded()).isEmpty();
        assertThat(repository.findById(marina.id()).orElseThrow().passwordRecovery()).isNotNull();
    }

    @Test
    @DisplayName("refuses an expired link and records the refusal against the account")
    void givenExpiredLink_whenVerifying_thenRefuseAndRecordItAgainstTheAccount() {
        // given
        Caretaker marina = withLink();
        clock.advance(Duration.ofMinutes(30));

        // when
        Result<Void> result = handler.handle(new VerifyRecoveryLinkCommand(FIRST_TOKEN.value(), ORIGIN));

        // then
        assertInvalidLink(result);
        AccessEvent event = recorder.recorded().getFirst();
        assertThat(event.outcome()).isEqualTo(AccessOutcome.RECOVERY_LINK_REFUSED);
        assertThat(event.attemptedIdentifier()).isEqualTo(EMAIL);
        assertThat(event.caretakerId()).isEqualTo(marina.id());
        assertThat(event.origin()).isEqualTo(ORIGIN);
    }

    @Test
    @DisplayName("refuses a link that matches nobody and records it as an unknown link")
    void givenLinkOfNobody_whenVerifying_thenRefuseAndRecordAnUnknownLink() {
        // given
        withLink();

        // when
        Result<Void> result = handler.handle(new VerifyRecoveryLinkCommand(SECOND_TOKEN.value(), ORIGIN));

        // then
        assertInvalidLink(result);
        AccessEvent event = recorder.recorded().getFirst();
        assertThat(event.attemptedIdentifier()).isEqualTo(AccessEvent.UNKNOWN_RECOVERY_LINK);
        assertThat(event.caretakerId()).isNull();
    }

    @ParameterizedTest(name = "[{0}]")
    @NullSource
    @ValueSource(strings = {"", "curto", "3q2+7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx"})
    @DisplayName("refuses a code outside the format the same way")
    void givenCodeOutsideTheFormat_whenVerifying_thenRefuseTheSameWay(String token) {
        // given
        withLink();

        // when
        Result<Void> result = handler.handle(new VerifyRecoveryLinkCommand(token, ORIGIN));

        // then
        assertInvalidLink(result);
        assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.RECOVERY_LINK_REFUSED);
    }

    // ---------------------------------------------------------------- contenção (US3)

    @Test
    @DisplayName("refuses even a valid link from a blocked origin, recording the throttle")
    void givenBlockedOrigin_whenVerifyingAValidLink_thenRefuseAsInvalidLinkAndRecordTheThrottle() {
        // given
        withLink();
        throttle.block();

        // when
        Result<Void> result = handler.handle(new VerifyRecoveryLinkCommand(FIRST_TOKEN.value(), ORIGIN));

        // then
        assertInvalidLink(result);
        assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.RECOVERY_THROTTLED);
    }

    @Test
    @DisplayName("counts an attempt of the origin for each link refused, and none for a valid one")
    void givenInvalidAndValidLinks_whenVerifying_thenCountOnlyTheInvalidOnes() {
        // given
        withLink();

        // when
        handler.handle(new VerifyRecoveryLinkCommand(SECOND_TOKEN.value(), ORIGIN));
        handler.handle(new VerifyRecoveryLinkCommand("curto", ORIGIN));
        handler.handle(new VerifyRecoveryLinkCommand(FIRST_TOKEN.value(), ORIGIN));

        // then
        assertThat(throttle.attempts()).containsExactly(ORIGIN, ORIGIN);
    }

    @Test
    @DisplayName("never shows the code of the link in the text form of the command")
    void givenCommand_whenPrinting_thenHideTheCode() {
        // given
        VerifyRecoveryLinkCommand command = new VerifyRecoveryLinkCommand(FIRST_TOKEN.value(), ORIGIN);

        // when / then
        assertThat(command.toString()).doesNotContain(FIRST_TOKEN.value());
    }
}
