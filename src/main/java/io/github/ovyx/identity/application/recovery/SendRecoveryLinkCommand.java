package io.github.ovyx.identity.application.recovery;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.valueobject.RecoveryToken;
import io.github.ovyx.shared.application.Command;

/**
 * O envio do e-mail do link (US1 da 012), adiado pela emissao para depois da confirmacao dela.
 *
 * <p>Leva o codigo do link em memoria, e so ate o e-mail: ele nunca e guardado (FR-009).
 *
 * @param caretakerId o responsavel do link
 * @param token o codigo do link
 * @param origin o endereco de origem do pedido, para a auditoria
 */
public record SendRecoveryLinkCommand(CaretakerId caretakerId, RecoveryToken token, String origin)
        implements Command<Void> {

    /** Nunca mostra o codigo. */
    @Override
    public String toString() {
        return "SendRecoveryLinkCommand[caretakerId=" + caretakerId + ", token=****]";
    }
}
