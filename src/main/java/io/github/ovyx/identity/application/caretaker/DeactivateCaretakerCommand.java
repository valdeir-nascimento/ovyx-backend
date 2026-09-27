package io.github.ovyx.identity.application.caretaker;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.shared.application.Command;

/** Inativacao de um responsavel (FR-014, FR-018). Nao ha exclusao fisica. */
public record DeactivateCaretakerCommand(CaretakerId caretakerId) implements Command<CaretakerId> {}
