package io.github.ovyx.identity.application.recovery;

import io.github.ovyx.identity.domain.valueobject.Email;
import io.github.ovyx.shared.application.Command;

/**
 * A emissao do link de recuperacao (US1 da 012), adiada pelo pedido: roda depois da resposta.
 *
 * @param email o e-mail do pedido, na forma canonica
 * @param origin o endereco de origem do pedido
 */
public record IssuePasswordRecoveryCommand(Email email, String origin) implements Command<Void> {}
