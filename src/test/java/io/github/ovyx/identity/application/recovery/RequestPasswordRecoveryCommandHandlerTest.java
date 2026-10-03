package io.github.ovyx.identity.application.recovery;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.application.RecordingAccessEventRecorder;
import io.github.ovyx.identity.application.recovery.RecoveryFakes.RecordingDeferredCommands;
import io.github.ovyx.identity.application.recovery.RecoveryFakes.RecordingRecoveryThrottle;
import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.model.AccessOutcome;
import io.github.ovyx.shared.domain.FixedClock;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * O pedido do link (US1 da 012), na requisição: confere só o formato do e-mail e adia todo o resto. O tratador não
 * recebe o repositório de responsáveis, de propósito: sem consulta à conta, a resposta não tem como depender dela
 * (FR-002).
 */
@DisplayName("RequestPasswordRecoveryCommandHandler")
class RequestPasswordRecoveryCommandHandlerTest {

    private static final String ORIGIN = "203.0.113.42";

    private final RecordingDeferredCommands deferred = new RecordingDeferredCommands();
    private final RecordingRecoveryThrottle throttle = new RecordingRecoveryThrottle();
    private final RecordingAccessEventRecorder recorder = new RecordingAccessEventRecorder();
    private final RequestPasswordRecoveryCommandHandler handler = new RequestPasswordRecoveryCommandHandler(
            throttle, recorder, deferred, FixedClock.at("2026-10-02T12:00:00Z"));

    @Test
    @DisplayName("defers the issue of the link with the canonical email and the origin")
    void givenEmailWithCapitalsAndSpaces_whenRequesting_thenDeferTheIssueWithTheCanonicalEmailAndTheOrigin() {
        // given
        RequestPasswordRecoveryCommand command =
                new RequestPasswordRecoveryCommand("  Marina.Costa@OVYX.com.br ", ORIGIN);

        // when
        Result<Void> result = handler.handle(command);

        // then
        assertThat(result.isFailure()).isFalse();
        assertThat(deferred.submitted()).singleElement().isInstanceOfSatisfying(IssuePasswordRecoveryCommand.class, issue -> {
            assertThat(issue.email().value()).isEqualTo("marina.costa@ovyx.com.br");
            assertThat(issue.origin()).isEqualTo(ORIGIN);
        });
    }

    @ParameterizedTest(name = "[{0}]")
    @CsvSource(value = {"'', Informe o e-mail.", "NULL, Informe o e-mail.", "marina, Informe um e-mail em formato válido."}, nullValues = "NULL")
    @DisplayName("refuses a missing or malformed email in its field, deferring nothing")
    void givenMissingOrMalformedEmail_whenRequesting_thenFailAsValidationInTheEmailFieldAndDeferNothing(
            String email, String message) {
        // given
        RequestPasswordRecoveryCommand command = new RequestPasswordRecoveryCommand(email, ORIGIN);

        // when
        Result<Void> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.error().details()).containsOnly(Map.entry("email", message));
        assertThat(deferred.submitted()).isEmpty();
    }

    // ---------------------------------------------------------------- contenção (US3)

    @Test
    @DisplayName("counts the attempt of the origin before deferring the issue")
    void givenFreeOrigin_whenRequesting_thenCountTheAttemptAndDefer() {
        // given
        RequestPasswordRecoveryCommand command = new RequestPasswordRecoveryCommand("marina.costa@ovyx.com.br", ORIGIN);

        // when
        handler.handle(command);

        // then
        assertThat(throttle.attempts()).containsExactly(ORIGIN);
        assertThat(deferred.submitted()).hasSize(1);
        assertThat(recorder.recorded()).isEmpty();
    }

    @Test
    @DisplayName("answers a blocked origin as any other, recording it and deferring nothing")
    void givenBlockedOrigin_whenRequesting_thenSucceedRecordTheThrottleAndDeferNothing() {
        // given
        throttle.block();

        // when
        Result<Void> result = handler.handle(new RequestPasswordRecoveryCommand("marina.costa@ovyx.com.br", ORIGIN));

        // then
        assertThat(result.isFailure()).isFalse();
        assertThat(deferred.submitted()).isEmpty();
        AccessEvent event = recorder.recorded().getFirst();
        assertThat(event.outcome()).isEqualTo(AccessOutcome.RECOVERY_THROTTLED);
        assertThat(event.attemptedIdentifier()).isEqualTo("marina.costa@ovyx.com.br");
        assertThat(event.origin()).isEqualTo(ORIGIN);
    }
}
