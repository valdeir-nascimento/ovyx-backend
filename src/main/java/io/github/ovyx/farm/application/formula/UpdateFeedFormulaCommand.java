package io.github.ovyx.farm.application.formula;

import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.shared.application.Command;

public record UpdateFeedFormulaCommand(
    String formulaId,
    String name,
    String pricePerKg,
    String expectedIntake,
    String description
) implements Command<FeedFormulaId> {
}
