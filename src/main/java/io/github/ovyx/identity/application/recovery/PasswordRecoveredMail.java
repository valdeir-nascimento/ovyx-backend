package io.github.ovyx.identity.application.recovery;

import java.time.Instant;
import java.util.Objects;

/**
 * O aviso de que a senha foi redefinida pelo link (FR-012 da 012).
 *
 * @param to o e-mail da conta
 * @param fullName o nome do responsavel
 * @param recoveredAt o instante da redefinicao
 */
public record PasswordRecoveredMail(String to, String fullName, Instant recoveredAt) {

    public PasswordRecoveredMail {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(recoveredAt, "recoveredAt");
    }
}
