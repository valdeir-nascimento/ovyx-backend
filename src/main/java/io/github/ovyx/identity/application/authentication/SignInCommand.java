package io.github.ovyx.identity.application.authentication;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.shared.application.Command;

/**
 * Pedido de entrada no sistema.
 *
 * @param identifier e-mail <strong>ou</strong> celular, indistintamente (FR-001), como digitado
 * @param password   senha em texto claro; vive apenas em memoria, durante a requisicao
 * @param origin     endereco de origem, usado pela contencao de tentativas e pela auditoria
 */
public record SignInCommand(String identifier, String password, String origin) implements Command<CaretakerId> {

    @Override
    public String toString() {
        return "SignInCommand[identifier=" + identifier + ", password=****, origin=" + origin + "]";
    }
}
