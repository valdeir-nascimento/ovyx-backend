package io.github.ovyx.identity.application.recovery;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.shared.application.Command;

/**
 * A redefinicao da senha pelo link de recuperacao (US2 da 012).
 *
 * @param token o codigo do link, como chegou
 * @param newPassword a nova senha, em texto claro
 * @param origin o endereco de origem da requisicao
 */
public record RecoverPasswordCommand(String token, String newPassword, String origin) implements Command<CaretakerId> {

    /** Nunca mostra o codigo nem a senha. */
    @Override
    public String toString() {
        return "RecoverPasswordCommand[token=****, newPassword=****, origin=" + origin + "]";
    }
}
