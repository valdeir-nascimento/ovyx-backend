package io.github.ovyx.identity.application.recovery;

import static io.github.ovyx.identity.application.recovery.RecoveryFakes.FIRST_TOKEN;
import static io.github.ovyx.identity.application.recovery.RecoveryFakes.SECOND_TOKEN;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.application.RecordingAccessEventRecorder;
import io.github.ovyx.identity.application.recovery.RecoveryFakes.RecordingDeferredCommands;
import io.github.ovyx.identity.application.recovery.RecoveryFakes.RecordingRecoveryThrottle;
import io.github.ovyx.identity.domain.FakePasswordHasher;
import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.model.AccessOutcome;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.fixtures.InMemoryCaretakerRepository;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A redefinição da senha pelo link (US2 da 012): troca a senha, registra na auditoria e adia o aviso por e-mail para
 * depois da confirmação. O link que não vale recebe sempre a mesma recusa; a política recusa no campo.
 */
@DisplayName("RecoverPasswordCommandHandler")
class RecoverPasswordCommandHandlerTest {

    private static final String ORIGIN = "203.0.113.42";
    private static final String EMAIL = "marina.costa@ovyx.com.br";
    private static final String NEW_PASSWORD = "PosturaAviario2027";

    private final InMemoryCaretakerRepository repository = new InMemoryCaretakerRepository();
    private final PasswordHasher hasher = new FakePasswordHasher();
    private final RecordingAccessEventRecorder recorder = new RecordingAccessEventRecorder();
    private final RecordingDeferredCommands deferred = new RecordingDeferredCommands();
    private final FixedClock clock = FixedClock.at("2026-10-02T12:00:00Z");
    private final RecordingRecoveryThrottle throttle = new RecordingRecoveryThrottle();
    private final RecoverPasswordCommandHandler handler =
            new RecoverPasswordCommandHandler(repository, hasher, throttle, recorder, deferred, clock);

    private Caretaker withLink() {
        Caretaker marina = aCaretaker().withEmail(EMAIL).withHasher(hasher).build();
        marina.issuePasswordRecovery(FIRST_TOKEN, clock);
        repository.save(marina);
        return marina;
    }

    @Test
    @DisplayName("changes the password, records it and defers the notice by email")
    void givenValidLink_whenRecovering_thenChangeThePasswordRecordItAndDeferTheNotice() {
        // given
        Caretaker marina = withLink();

        // when
        Result<CaretakerId> result =
                handler.handle(new RecoverPasswordCommand(FIRST_TOKEN.value(), NEW_PASSWORD, ORIGIN));

        // then
        assertThat(result.value()).isEqualTo(marina.id());
        Caretaker stored = repository.findById(marina.id()).orElseThrow();
        assertThat(stored.authenticate(NEW_PASSWORD, hasher)).isTrue();
        assertThat(stored.sessionGeneration()).isEqualTo(1);
        AccessEvent event = recorder.recorded().getFirst();
        assertThat(event.outcome()).isEqualTo(AccessOutcome.PASSWORD_RECOVERED);
        assertThat(event.attemptedIdentifier()).isEqualTo(EMAIL);
        assertThat(event.caretakerId()).isEqualTo(marina.id());
        assertThat(deferred.submitted()).singleElement().isInstanceOfSatisfying(NotifyPasswordRecoveredCommand.class, notify -> {
            assertThat(notify.caretakerId()).isEqualTo(marina.id());
            assertThat(notify.recoveredAt()).isEqualTo(clock.instant());
        });
    }

    @Test
    @DisplayName("refuses a password outside the policy in its field, recording and deferring nothing")
    void givenPasswordOutsideThePolicy_whenRecovering_thenFailAsValidationInTheField() {
        // given
        Caretaker marina = withLink();

        // when
        Result<CaretakerId> result = handler.handle(new RecoverPasswordCommand(FIRST_TOKEN.value(), "curta", ORIGIN));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.error().details()).containsOnlyKeys("newPassword");
        assertThat(recorder.recorded()).isEmpty();
        assertThat(deferred.submitted()).isEmpty();
        assertThat(repository.findById(marina.id()).orElseThrow().passwordRecovery()).isNotNull();
    }

    @Test
    @DisplayName("refuses a link that matches nobody, recording an unknown link")
    void givenLinkOfNobody_whenRecovering_thenRefuseAsInvalidLinkAndRecordIt() {
        // given
        withLink();

        // when
        Result<CaretakerId> result =
                handler.handle(new RecoverPasswordCommand(SECOND_TOKEN.value(), NEW_PASSWORD, ORIGIN));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().code()).isEqualTo("RECOVERY_LINK_INVALID");
        assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.RECOVERY_LINK_REFUSED);
        assertThat(recorder.recorded().getFirst().attemptedIdentifier()).isEqualTo(AccessEvent.UNKNOWN_RECOVERY_LINK);
        assertThat(deferred.submitted()).isEmpty();
    }

    @Test
    @DisplayName("refuses a link already used, recording it against the account")
    void givenLinkAlreadyUsed_whenRecoveringAgain_thenRefuseAndRecordItAgainstTheAccount() {
        // given
        Caretaker marina = withLink();
        handler.handle(new RecoverPasswordCommand(FIRST_TOKEN.value(), NEW_PASSWORD, ORIGIN));

        // when
        Result<CaretakerId> result =
                handler.handle(new RecoverPasswordCommand(FIRST_TOKEN.value(), "OutraSenha2028", ORIGIN));

        // then
        assertThat(result.error().code()).isEqualTo("RECOVERY_LINK_INVALID");
        assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.RECOVERY_LINK_REFUSED);
        assertThat(repository.findById(marina.id()).orElseThrow().authenticate(NEW_PASSWORD, hasher)).isTrue();
    }

    @Test
    @DisplayName("refuses a code outside the format as an invalid link")
    void givenCodeOutsideTheFormat_whenRecovering_thenRefuseAsInvalidLink() {
        // given
        withLink();

        // when
        Result<CaretakerId> result = handler.handle(new RecoverPasswordCommand("curto", NEW_PASSWORD, ORIGIN));

        // then
        assertThat(result.error().code()).isEqualTo("RECOVERY_LINK_INVALID");
    }

    @Test
    @DisplayName("never shows the code nor the password in the text form of the command")
    void givenCommand_whenPrinting_thenHideTheCodeAndThePassword() {
        // given
        RecoverPasswordCommand command = new RecoverPasswordCommand(FIRST_TOKEN.value(), NEW_PASSWORD, ORIGIN);

        // when / then
        assertThat(command.toString()).doesNotContain(FIRST_TOKEN.value()).doesNotContain(NEW_PASSWORD);
    }

    // ---------------------------------------------------------------- contenção (US3)

    @Test
    @DisplayName("refuses even a valid link from a blocked origin, changing nothing")
    void givenBlockedOrigin_whenRecoveringWithAValidLink_thenRefuseAndChangeNothing() {
        // given
        Caretaker marina = withLink();
        throttle.block();

        // when
        Result<CaretakerId> result =
                handler.handle(new RecoverPasswordCommand(FIRST_TOKEN.value(), NEW_PASSWORD, ORIGIN));

        // then
        assertThat(result.error().code()).isEqualTo("RECOVERY_LINK_INVALID");
        assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.RECOVERY_THROTTLED);
        assertThat(repository.findById(marina.id()).orElseThrow().authenticate(NEW_PASSWORD, hasher)).isFalse();
    }

    @Test
    @DisplayName("counts an attempt of the origin for an invalid link, and none for the policy")
    void givenInvalidLinkAndPolicyRefusal_whenRecovering_thenCountOnlyTheInvalidLink() {
        // given
        withLink();

        // when
        handler.handle(new RecoverPasswordCommand(SECOND_TOKEN.value(), NEW_PASSWORD, ORIGIN));
        handler.handle(new RecoverPasswordCommand(FIRST_TOKEN.value(), "curta", ORIGIN));

        // then
        assertThat(throttle.attempts()).containsExactly(ORIGIN);
    }
}
