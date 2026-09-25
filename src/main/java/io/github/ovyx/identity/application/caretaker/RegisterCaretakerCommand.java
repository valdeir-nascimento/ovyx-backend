package io.github.ovyx.identity.application.caretaker;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.shared.application.Command;

/**
 * Cadastro de um responsavel pelo administrador (FR-013).
 *
 * <p>Os campos chegam como foram digitados: quem valida, e devolve todas as violacoes de uma vez, e
 * o agregado (FR-017).
 */
public record RegisterCaretakerCommand(
        String fullName, String cpf, String email, String mobilePhone, String password, String role)
        implements Command<CaretakerId> {

    /** Nunca mostra a senha: o comando passa por log de erro e por depurador. */
    @Override
    public String toString() {
        return "RegisterCaretakerCommand[fullName=" + fullName + ", cpf=" + cpf + ", email=" + email
                + ", mobilePhone=" + mobilePhone + ", password=****, role=" + role + "]";
    }
}
