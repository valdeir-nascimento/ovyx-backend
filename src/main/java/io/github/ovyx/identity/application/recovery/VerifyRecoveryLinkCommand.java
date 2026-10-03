package io.github.ovyx.identity.application.recovery;

import io.github.ovyx.shared.application.Command;

/**
 * A conferencia do link de recuperacao (US2 da 012), quando a tela de redefinicao abre.
 *
 * <p>E comando, e nao consulta: registra a recusa na auditoria e, com os limites da US3, conta a tentativa da origem
 * (principio V).
 *
 * @param token o codigo do link, como chegou
 * @param origin o endereco de origem da requisicao
 */
public record VerifyRecoveryLinkCommand(String token, String origin) implements Command<Void> {

    /** Nunca mostra o codigo. */
    @Override
    public String toString() {
        return "VerifyRecoveryLinkCommand[token=****, origin=" + origin + "]";
    }
}
