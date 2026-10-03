package io.github.ovyx.identity.application.recovery;

import io.github.ovyx.shared.application.Command;

/**
 * O pedido do link de recuperacao da senha (US1 da 012), pelo e-mail da conta, feito por quem nao esta conectado.
 *
 * @param email o e-mail como foi digitado
 * @param origin o endereco de origem da requisicao
 */
public record RequestPasswordRecoveryCommand(String email, String origin) implements Command<Void> {}
