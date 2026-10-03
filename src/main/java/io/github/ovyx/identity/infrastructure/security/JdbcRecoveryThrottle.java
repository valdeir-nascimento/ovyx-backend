package io.github.ovyx.identity.infrastructure.security;

import io.github.ovyx.identity.domain.port.RecoveryThrottle;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contencao da recuperacao de senha por origem, persistida em PostgreSQL (FR-015 da 012), no molde da contencao da
 * entrada: sobrevive a reinicio e funciona com mais de uma instancia.
 *
 * <p>Cada escrita entra na transacao do comando que a produziu, e fica contada porque o despachante confirma a
 * transacao tambem quando o comando e recusado.
 */
@Component
public class JdbcRecoveryThrottle implements RecoveryThrottle {

    private final JdbcClient jdbcClient;
    private final Clock clock;
    private final RecoveryThrottleProperties properties;

    JdbcRecoveryThrottle(JdbcClient jdbcClient, Clock clock, RecoveryThrottleProperties properties) {
        this.jdbcClient = jdbcClient;
        this.clock = clock;
        this.properties = properties;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(String origin) {
        return jdbcClient
                .sql("select blocked_until from recovery_attempt where origin = :origin")
                .param("origin", origin)
                .query((rs, rowNumber) -> rs.getTimestamp("blocked_until"))
                .optional()
                .map(blockedUntil -> blockedUntil != null && blockedUntil.toInstant().isAfter(clock.instant()))
                .orElse(false);
    }

    @Override
    @Transactional
    public void registerAttempt(String origin) {
        Instant now = clock.instant();
        // Um comando so abre, reabre, incrementa e bloqueia, para tentativas simultaneas nao escaparem da contagem.
        // O bloqueio em curso nao e encurtado por tentativas novas.
        jdbcClient
                .sql(
                        """
                        insert into recovery_attempt (origin, attempt_count, window_started_at, blocked_until)
                        values (:origin, 1, :now, null)
                        on conflict (origin) do update set
                            attempt_count = case
                                when recovery_attempt.window_started_at < :windowStart then 1
                                else recovery_attempt.attempt_count + 1
                            end,
                            window_started_at = case
                                when recovery_attempt.window_started_at < :windowStart then :now
                                else recovery_attempt.window_started_at
                            end,
                            blocked_until = case
                                when recovery_attempt.blocked_until > :now then recovery_attempt.blocked_until
                                when recovery_attempt.window_started_at >= :windowStart
                                     and recovery_attempt.attempt_count + 1 > :maxAttempts
                                then cast(:blockedUntil as timestamptz)
                                else cast(null as timestamptz)
                            end
                        """)
                .param("origin", origin)
                .param("now", Timestamp.from(now))
                .param("windowStart", Timestamp.from(now.minus(properties.window())))
                .param("maxAttempts", properties.maxAttempts())
                .param("blockedUntil", Timestamp.from(now.plus(properties.blockDuration())))
                .update();
    }
}
