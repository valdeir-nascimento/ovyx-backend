package io.github.ovyx.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * A contenção da recuperação por origem (FR-015 da 012), contra o PostgreSQL de verdade: 10 tentativas por origem a
 * cada 15 minutos; a décima primeira bloqueia a origem por 15 minutos.
 */
@DisplayName("JdbcRecoveryThrottle")
class JdbcRecoveryThrottleIT extends IntegrationTestSupport {

    @Autowired
    private JdbcClient jdbcClient;

    private final FixedClock clock = FixedClock.at("2026-10-02T12:00:00Z");

    private JdbcRecoveryThrottle throttle;

    private String origin;

    @BeforeEach
    void setUp() {
        throttle = new JdbcRecoveryThrottle(
                jdbcClient, clock, new RecoveryThrottleProperties(10, Duration.ofMinutes(15), Duration.ofMinutes(15)));
        origin = "10.0." + UUID.randomUUID().toString().substring(0, 8);
    }

    private void attempts(String from, int times) {
        for (int attempt = 0; attempt < times; attempt++) {
            throttle.registerAttempt(from);
        }
    }

    @Test
    @DisplayName("lets ten attempts through and blocks the origin at the eleventh")
    void givenTenAttempts_whenRegisteringTheEleventh_thenBlockTheOrigin() {
        // given
        attempts(origin, 10);
        assertThat(throttle.isBlocked(origin)).isFalse();

        // when
        throttle.registerAttempt(origin);

        // then
        assertThat(throttle.isBlocked(origin)).isTrue();
    }

    @Test
    @DisplayName("does not block another origin")
    void givenBlockedOrigin_whenAskingAboutAnother_thenLeaveItFree() {
        // given
        attempts(origin, 11);

        // when
        boolean other = throttle.isBlocked(origin + ".2");

        // then
        assertThat(other).isFalse();
    }

    @Test
    @DisplayName("starts counting again once the fifteen minutes of the window are over")
    void givenTenAttemptsInAnOldWindow_whenAttemptingAfterIt_thenCountAgainFromOne() {
        // given
        attempts(origin, 10);
        clock.advance(Duration.ofMinutes(16));

        // when
        attempts(origin, 10);

        // then
        assertThat(throttle.isBlocked(origin)).isFalse();
    }

    @Test
    @DisplayName("releases the origin when the block is over")
    void givenBlockedOrigin_whenTheBlockIsOver_thenReleaseIt() {
        // given
        attempts(origin, 11);

        // when
        clock.advance(Duration.ofMinutes(15));

        // then
        assertThat(throttle.isBlocked(origin)).isFalse();
    }

    @Test
    @DisplayName("keeps the origin blocked while it keeps trying during the block")
    void givenBlockedOrigin_whenTryingAgainDuringTheBlock_thenKeepItBlocked() {
        // given
        attempts(origin, 11);
        clock.advance(Duration.ofMinutes(14));

        // when
        throttle.registerAttempt(origin);

        // then
        assertThat(throttle.isBlocked(origin)).isTrue();
    }
}
