package io.github.ovyx.identity.application.recovery;

import static io.github.ovyx.identity.application.recovery.RecoveryFakes.FIRST_TOKEN;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.application.RecordingAccessEventRecorder;
import io.github.ovyx.identity.application.recovery.RecoveryFakes.RecordingDeferredCommands;
import io.github.ovyx.identity.application.recovery.RecoveryFakes.SequenceRecoveryTokens;
import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.model.AccessOutcome;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.valueobject.Email;
import io.github.ovyx.identity.fixtures.InMemoryCaretakerRepository;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A emissão do link (US1 da 012), depois da resposta ao pedido: procura a conta do e-mail, emite o link no agregado,
 * grava, e adia o envio do e-mail para depois da confirmação. A causa real fica só na auditoria; o resultado é
 * sempre sucesso, porque não há a quem responder.
 */
@DisplayName("IssuePasswordRecoveryCommandHandler")
class IssuePasswordRecoveryCommandHandlerTest {

    private static final String ORIGIN = "203.0.113.42";
    private static final String EMAIL = "marina.costa@ovyx.com.br";

    private final InMemoryCaretakerRepository repository = new InMemoryCaretakerRepository();
    private final RecordingAccessEventRecorder recorder = new RecordingAccessEventRecorder();
    private final RecordingDeferredCommands deferred = new RecordingDeferredCommands();
    private final FixedClock clock = FixedClock.at("2026-10-02T12:00:00Z");
    private final IssuePasswordRecoveryCommandHandler handler = new IssuePasswordRecoveryCommandHandler(
            repository, new SequenceRecoveryTokens(FIRST_TOKEN), recorder, deferred, clock);

    private Caretaker saved(Caretaker caretaker) {
        repository.save(caretaker);
        return caretaker;
    }

    private Result<Void> issueFor(String email) {
        return handler.handle(new IssuePasswordRecoveryCommand(Email.of(email), ORIGIN));
    }

    @Test
    @DisplayName("issues the link to the active caretaker, saves it and defers the email")
    void givenActiveCaretaker_whenIssuing_thenSaveTheLinkAndDeferTheEmail() {
        // given
        Caretaker marina = saved(aCaretaker().withEmail(EMAIL).build());

        // when
        Result<Void> result = issueFor(EMAIL);

        // then
        assertThat(result.isFailure()).isFalse();
        Caretaker stored = repository.findById(marina.id()).orElseThrow();
        assertThat(stored.passwordRecovery().tokenHash()).isEqualTo(FIRST_TOKEN.hash());
        assertThat(stored.passwordRecovery().expiresAt()).isEqualTo(Instant.parse("2026-10-02T12:30:00Z"));
        assertThat(deferred.submitted()).singleElement().isInstanceOfSatisfying(SendRecoveryLinkCommand.class, send -> {
            assertThat(send.caretakerId()).isEqualTo(marina.id());
            assertThat(send.token()).isEqualTo(FIRST_TOKEN);
            assertThat(send.origin()).isEqualTo(ORIGIN);
        });
        assertThat(recorder.recorded()).isEmpty();
    }

    @Test
    @DisplayName("records an unknown email in the audit trail only, sending nothing")
    void givenEmailOfNobody_whenIssuing_thenRecordUnknownEmailAndSendNothing() {
        // given
        saved(aCaretaker().withEmail("outra.pessoa@ovyx.com.br").build());

        // when
        Result<Void> result = issueFor(EMAIL);

        // then
        assertThat(result.isFailure()).isFalse();
        assertThat(deferred.submitted()).isEmpty();
        AccessEvent event = recorder.recorded().getFirst();
        assertThat(event.outcome()).isEqualTo(AccessOutcome.RECOVERY_UNKNOWN_EMAIL);
        assertThat(event.attemptedIdentifier()).isEqualTo(EMAIL);
        assertThat(event.caretakerId()).isNull();
        assertThat(event.origin()).isEqualTo(ORIGIN);
    }

    @Test
    @DisplayName("records an inactive caretaker in the audit trail only, issuing and sending nothing")
    void givenInactiveCaretaker_whenIssuing_thenRecordInactiveAndIssueNothing() {
        // given
        Caretaker marina = saved(aCaretaker().withEmail(EMAIL).buildInactive());

        // when
        Result<Void> result = issueFor(EMAIL);

        // then
        assertThat(result.isFailure()).isFalse();
        assertThat(repository.findById(marina.id()).orElseThrow().passwordRecovery()).isNull();
        assertThat(deferred.submitted()).isEmpty();
        assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.RECOVERY_INACTIVE);
        assertThat(recorder.recorded().getFirst().caretakerId()).isEqualTo(marina.id());
    }

    // ---------------------------------------------------------------- limite da conta (US3)

    @Test
    @DisplayName("records the fourth request of the hour as limited, sending nothing")
    void givenThreeLinksInTheHour_whenIssuingTheFourth_thenRecordLimitedAndSendNothing() {
        // given
        Caretaker marina = saved(aCaretaker().withEmail(EMAIL).build());
        IssuePasswordRecoveryCommandHandler fourTimes = new IssuePasswordRecoveryCommandHandler(
                repository,
                new SequenceRecoveryTokens(FIRST_TOKEN, RecoveryFakes.SECOND_TOKEN, FIRST_TOKEN, RecoveryFakes.SECOND_TOKEN),
                recorder,
                deferred,
                clock);
        for (int request = 0; request < 3; request++) {
            fourTimes.handle(new IssuePasswordRecoveryCommand(Email.of(EMAIL), ORIGIN));
        }

        // when
        Result<Void> result = fourTimes.handle(new IssuePasswordRecoveryCommand(Email.of(EMAIL), ORIGIN));

        // then
        assertThat(result.isFailure()).isFalse();
        assertThat(deferred.submitted()).hasSize(3);
        assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.RECOVERY_LIMITED);
        assertThat(recorder.recorded().getLast().caretakerId()).isEqualTo(marina.id());
    }
}
