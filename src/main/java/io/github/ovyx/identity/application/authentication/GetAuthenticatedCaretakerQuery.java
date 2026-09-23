package io.github.ovyx.identity.application.authentication;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.shared.application.Query;

/** Consulta "quem esta autenticado", a partir da identidade guardada na sessao. */
public record GetAuthenticatedCaretakerQuery(CaretakerId caretakerId) implements Query<AuthenticatedCaretaker> {}
