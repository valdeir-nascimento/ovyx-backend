package io.github.ovyx.identity.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O link pendente (FR-005 da 012): vale 30 minutos a partir do pedido, e só para o resumo do código que o emitiu.
 */
@DisplayName("PasswordRecovery")
class PasswordRecoveryTest {

    private static final Instant REQUESTED_AT = Instant.parse("2026-10-02T12:00:00Z");
    private static final RecoveryToken TOKEN = RecoveryToken.of("3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx");
    private static final RecoveryToken OTHER = RecoveryToken.of("Zz9-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx");

    @Test
    @DisplayName("expires thirty minutes after the request")
    void givenRequestInstant_whenIssuing_thenExpireThirtyMinutesLater() {
        // given
        Instant now = REQUESTED_AT;

        // when
        PasswordRecovery recovery = PasswordRecovery.issue(TOKEN, now);

        // then
        assertThat(PasswordRecovery.VALIDITY).isEqualTo(Duration.ofMinutes(30));
        assertThat(recovery.tokenHash()).isEqualTo(TOKEN.hash());
        assertThat(recovery.expiresAt()).isEqualTo(Instant.parse("2026-10-02T12:30:00Z"));
    }

    @Test
    @DisplayName("is valid for the same code until just before it expires")
    void givenSameCodeBeforeTheExpiry_whenChecking_thenAcceptIt() {
        // given
        PasswordRecovery recovery = PasswordRecovery.issue(TOKEN, REQUESTED_AT);

        // when / then
        assertThat(recovery.isValidFor(TOKEN.hash(), REQUESTED_AT)).isTrue();
        assertThat(recovery.isValidFor(TOKEN.hash(), Instant.parse("2026-10-02T12:29:59.999Z"))).isTrue();
    }

    @Test
    @DisplayName("is not valid at the exact expiry nor after it")
    void givenSameCodeAtOrAfterTheExpiry_whenChecking_thenRefuseIt() {
        // given
        PasswordRecovery recovery = PasswordRecovery.issue(TOKEN, REQUESTED_AT);

        // when / then
        assertThat(recovery.isValidFor(TOKEN.hash(), Instant.parse("2026-10-02T12:30:00Z"))).isFalse();
        assertThat(recovery.isValidFor(TOKEN.hash(), Instant.parse("2026-10-02T13:00:00Z"))).isFalse();
    }

    @Test
    @DisplayName("is not valid for another code")
    void givenAnotherCode_whenChecking_thenRefuseIt() {
        // given
        PasswordRecovery recovery = PasswordRecovery.issue(TOKEN, REQUESTED_AT);

        // when / then
        assertThat(recovery.isValidFor(OTHER.hash(), REQUESTED_AT)).isFalse();
        assertThat(recovery.isValidFor(null, REQUESTED_AT)).isFalse();
    }
}
