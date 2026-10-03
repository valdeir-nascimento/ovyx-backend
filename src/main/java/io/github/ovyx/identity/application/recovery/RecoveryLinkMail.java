package io.github.ovyx.identity.application.recovery;

import io.github.ovyx.identity.domain.valueobject.RecoveryToken;
import java.time.Instant;
import java.util.Objects;

/**
 * O e-mail do link de recuperacao (FR-004 da 012): para quem, o nome, o codigo e o fim da validade. Nunca a senha.
 *
 * @param to o e-mail da conta
 * @param fullName o nome do responsavel, como o cumprimento o mostra
 * @param token o codigo do link
 * @param expiresAt o fim da validade do link
 */
public record RecoveryLinkMail(String to, String fullName, RecoveryToken token, Instant expiresAt) {

    public RecoveryLinkMail {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    /** Sem o codigo: o {@link RecoveryToken} ja o esconde, e o e-mail identifica a pessoa so pelo endereco. */
    @Override
    public String toString() {
        return "RecoveryLinkMail[to=" + to + "]";
    }
}
