package io.github.ovyx.farm.application.formula;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.domain.model.FeedFormula;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.farm.domain.port.FeedFormulaRepository;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;

import java.time.Clock;
import java.util.Optional;

/**
 * Inativa uma formula (FR-004 da 004). Nada e apagado, e a inativacao nao tem recusa: a formula inativa
 * so continua inativa, e nao e gravada de novo.
 */
public class DeactivateFeedFormulaCommandHandler implements CommandHandler<DeactivateFeedFormulaCommand, FeedFormulaId> {

    private final FeedFormulaRepository formulaRepository;
    private final Clock clock;

    public DeactivateFeedFormulaCommandHandler(FeedFormulaRepository formulaRepository, Clock clock) {
        this.formulaRepository = formulaRepository;
        this.clock = clock;
    }

    @Override
    public Result<FeedFormulaId> handle(DeactivateFeedFormulaCommand command) {
        Optional<FeedFormula> found = FeedFormulaId.parse(command.formulaId()).flatMap(formulaRepository::findById);
        if (found.isEmpty()) {
            return Result.failure(FarmRefusals.feedFormulaNotFound());
        }

        FeedFormula formula = found.get();
        if (formula.deactivate(clock)) {
            formulaRepository.save(formula);
        }
        return Result.success(formula.id());
    }
}
