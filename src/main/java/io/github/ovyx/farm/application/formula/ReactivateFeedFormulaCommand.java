package io.github.ovyx.farm.application.formula;

import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.shared.application.Command;

/** Reativacao de uma formula pelo administrador (FR-004 da 004). */
public record ReactivateFeedFormulaCommand(String formulaId) implements Command<FeedFormulaId> {}
