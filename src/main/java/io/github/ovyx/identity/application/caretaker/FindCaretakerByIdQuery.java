package io.github.ovyx.identity.application.caretaker;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.shared.application.Query;

/** Consulta de um responsavel pelo administrador (FR-014). */
public record FindCaretakerByIdQuery(CaretakerId caretakerId) implements Query<CaretakerDetail> {}
