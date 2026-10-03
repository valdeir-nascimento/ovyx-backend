package io.github.ovyx.identity.domain.valueobject;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.domain.DomainException;
import java.time.Duration;
import java.time.Instant;

/**
 * Os links de recuperacao que um responsavel recebeu na hora contada (R-004 da 012): o limite de 3 por hora protege
 * a caixa de entrada da pessoa (FR-014).
 *
 * @param windowStartedAt o primeiro pedido da hora contada, ou {@code null} quando nenhum foi contado
 * @param requestsInWindow os links emitidos nessa hora, de 0 a 3
 */
public record RecoveryAllowance(Instant windowStartedAt, int requestsInWindow) {

    /** Nenhum pedido contado: o estado de quem nunca pediu. */
    public static final RecoveryAllowance NONE = new RecoveryAllowance(null, 0);

    /** Os links que uma conta recebe numa hora (FR-014). */
    public static final int LIMIT = 3;

    /** A hora contada, a partir do primeiro pedido dela. */
    public static final Duration WINDOW = Duration.ofHours(1);

    public RecoveryAllowance {
        if (requestsInWindow < 0) {
            throw new IllegalArgumentException("os pedidos contados não podem ser negativos");
        }
    }

    /**
     * Conta mais um link emitido agora.
     *
     * <p>A hora comeca no primeiro pedido, e nao no relogio cheio. Passada ela, o pedido seguinte abre outra hora e
     * conta 1.
     *
     * @throws DomainException com {@code RECOVERY_LIMIT_REACHED}, quando a conta ja recebeu {@link #LIMIT} links na
     *     hora contada
     */
    public RecoveryAllowance count(Instant now) {
        if (windowStartedAt == null || !now.isBefore(windowStartedAt.plus(WINDOW))) {
            return new RecoveryAllowance(now, 1);
        }
        if (requestsInWindow >= LIMIT) {
            throw new DomainException(
                    IdentityErrorCode.RECOVERY_LIMIT_REACHED, IdentityErrorCode.RECOVERY_LIMIT_REACHED_MESSAGE);
        }
        return new RecoveryAllowance(windowStartedAt, requestsInWindow + 1);
    }
}
