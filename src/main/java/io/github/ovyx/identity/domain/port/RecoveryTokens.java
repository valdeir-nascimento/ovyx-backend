package io.github.ovyx.identity.domain.port;

import io.github.ovyx.identity.domain.valueobject.RecoveryToken;

/**
 * Porta de saida para gerar o codigo do link de recuperacao (R-002 da 012).
 *
 * <p>E porta, e nao um {@code new SecureRandom()} no dominio, para os testes do agregado e dos tratadores saberem qual
 * codigo foi emitido. A implementacao usa um gerador criptografico: 32 bytes, em Base64 URL sem preenchimento.
 */
public interface RecoveryTokens {

    /** Um codigo novo, imprevisivel. */
    RecoveryToken issue();
}
