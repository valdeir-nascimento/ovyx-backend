package io.github.ovyx.identity.infrastructure.security;

import io.github.ovyx.identity.domain.port.SignInThrottle;
import java.time.Clock;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contencao de tentativas repetidas de acesso, persistida em PostgreSQL (FR-023).
 *
 * <p>Em banco, e nao em memoria, por dois motivos: a contagem sobrevive a reinicio da aplicacao, e
 * funciona com mais de uma instancia — um contador em memoria seria zerado por um restart e
 * contornado por qualquer balanceamento de carga.
 *
 * <p>Cada escrita roda em transacao propria ({@code REQUIRES_NEW}): a falha precisa ficar contada
 * mesmo quando a operacao que a produziu termina em erro.
 *
 * <p>O identificador chega ja na forma canonica de {@code AccessIdentifier}, a mesma usada na busca
 * da conta, e e gravado como veio. Uma segunda normalizacao aqui, com regra propria, era o que
 * permitia a busca e a contencao divergirem (FR-023).
 */
@Component
public class JdbcSignInThrottle implements SignInThrottle {

    private final JdbcClient jdbcClient;
    private final Clock clock;
    private final SignInThrottleProperties properties;

    JdbcSignInThrottle(JdbcClient jdbcClient, Clock clock, SignInThrottleProperties properties) {
        this.jdbcClient = jdbcClient;
        this.clock = clock;
        this.properties = properties;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(String attemptedIdentifier, String origin) {
        return jdbcClient
                .sql(
                        """
                        select blocked_until from sign_in_attempt
                        where attempted_identifier = :identifier and origin = :origin
                        """)
                .param("identifier", attemptedIdentifier)
                .param("origin", origin)
                .query((rs, rowNumber) -> rs.getTimestamp("blocked_until"))
                .optional()
                .map(blockedUntil -> blockedUntil != null && blockedUntil.toInstant().isAfter(clock.instant()))
                .orElse(false);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailure(String attemptedIdentifier, String origin) {
        Instant now = clock.instant();
        Instant windowStart = now.minus(properties.window());

        // Um unico comando resolve abrir janela, reabrir janela expirada, incrementar e bloquear.
        // Fazer isso em passos separados abriria espaco para corrida entre tentativas simultaneas,
        // que e exatamente o cenario de um ataque de forca bruta.
        jdbcClient
                .sql(
                        """
                        insert into sign_in_attempt
                            (attempted_identifier, origin, failure_count, window_started_at, blocked_until)
                        values
                            (:identifier, :origin, 1, :now, null)
                        on conflict (attempted_identifier, origin) do update set
                            failure_count = case
                                when sign_in_attempt.window_started_at < :windowStart then 1
                                else sign_in_attempt.failure_count + 1
                            end,
                            window_started_at = case
                                when sign_in_attempt.window_started_at < :windowStart then :now
                                else sign_in_attempt.window_started_at
                            end,
                            -- Os dois ramos do CASE precisam de tipo explicito: com o null sem
                            -- anotacao, o PostgreSQL infere text para a expressao inteira e
                            -- recusa a atribuicao a uma coluna timestamptz.
                            blocked_until = case
                                when sign_in_attempt.window_started_at >= :windowStart
                                     and sign_in_attempt.failure_count + 1 >= :maxFailures
                                then cast(:blockedUntil as timestamptz)
                                else cast(null as timestamptz)
                            end
                        """)
                .param("identifier", attemptedIdentifier)
                .param("origin", origin)
                .param("now", Timestamp.from(now))
                .param("windowStart", Timestamp.from(windowStart))
                .param("maxFailures", properties.maxFailures())
                .param("blockedUntil", Timestamp.from(now.plus(properties.blockDuration())))
                .update();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clear(String attemptedIdentifier, String origin) {
        jdbcClient
                .sql(
                        """
                        delete from sign_in_attempt
                        where attempted_identifier = :identifier and origin = :origin
                        """)
                .param("identifier", attemptedIdentifier)
                .param("origin", origin)
                .update();
    }
}
