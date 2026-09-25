package io.github.ovyx.identity.application.caretaker;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.shared.application.Command;

/** Edicao dos dados cadastrais e do perfil de um responsavel (FR-014). A senha nao muda por aqui. */
public record UpdateCaretakerCommand(
        CaretakerId caretakerId, String fullName, String cpf, String email, String mobilePhone, String role)
        implements Command<CaretakerId> {
}
