package io.github.ovyx.identity.application.authentication;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.shared.application.Command;

/**
 * Pedido de encerramento de sessao.
 *
 * @param caretakerId responsavel que esta saindo, vindo da sessao autenticada
 * @param origin endereco de origem, para a auditoria
 */
public record SignOutCommand(CaretakerId caretakerId, String origin) implements Command<CaretakerId> {}
