package io.github.ovyx.identity.application.recovery;

/**
 * Porta de saida para os e-mails do identity (R-006 da 012): o link de recuperacao da senha e o aviso da
 * redefinicao.
 *
 * <p>Os dois devolvem se o servidor de envio aceitou a mensagem, e nunca lancam por falha dele: um servidor fora do
 * ar nao pode impedir o resto do Ovyx (FR-020). Quem chama registra o resultado na auditoria.
 */
public interface IdentityMailer {

    /** Envia o link de recuperacao; {@code false} quando o servidor recusou ou nao respondeu. */
    boolean sendRecoveryLink(RecoveryLinkMail mail);

    /** Envia o aviso de que a senha foi redefinida; {@code false} quando o servidor recusou ou nao respondeu. */
    boolean sendPasswordRecoveredNotice(PasswordRecoveredMail mail);
}
