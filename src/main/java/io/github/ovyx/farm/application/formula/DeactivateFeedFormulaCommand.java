package io.github.ovyx.farm.application.formula;

import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.shared.application.Command;

/** Inativacao de uma formula pelo administrador (FR-004 da 004). */
public record DeactivateFeedFormulaCommand(String formulaId) implements Command<FeedFormulaId> {}
