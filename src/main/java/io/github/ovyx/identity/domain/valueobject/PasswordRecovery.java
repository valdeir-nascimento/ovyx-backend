package io.github.ovyx.identity.domain.valueobject;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * O link de recuperacao pendente de um responsavel (R-001 da 012): o resumo do codigo e o fim da validade.
 *
 * <p>O responsavel tem no maximo um. O pedido novo o substitui, e toda troca de senha e a inativacao o anulam: e o
 * que faz valer so o link mais recente (FR-006, FR-007).
 *
 * @param tokenHash o resumo SHA-256 do codigo ({@link RecoveryToken#hash()})
 * @param expiresAt o fim da validade: a partir deste instante, o link nao vale
 */
public record PasswordRecovery(String tokenHash, Instant expiresAt) {

    /** Quanto o link vale a partir do pedido (FR-005). */
    public static final Duration VALIDITY = Duration.ofMinutes(30);

    public PasswordRecovery {
        Objects.requireNonNull(tokenHash, "tokenHash");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    /** O link do codigo, pedido agora: vence daqui a {@link #VALIDITY}. */
    public static PasswordRecovery issue(RecoveryToken token, Instant now) {
        return new PasswordRecovery(token.hash(), now.plus(VALIDITY));
    }

    /** Se o resumo e o deste link e ele ainda nao venceu. */
    public boolean isValidFor(String hash, Instant now) {
        return tokenHash.equals(hash) && now.isBefore(expiresAt);
    }
}
