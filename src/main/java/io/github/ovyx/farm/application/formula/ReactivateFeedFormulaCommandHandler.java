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
 * Reativa uma formula (FR-004 da 004). Sem recusa: o nome dela nao pode ter sido tomado, porque a
 * unicidade vale tambem entre as inativas.
 */
public class ReactivateFeedFormulaCommandHandler implements CommandHandler<ReactivateFeedFormulaCommand, FeedFormulaId> {

    private final FeedFormulaRepository formulaRepository;
    private final Clock clock;

    public ReactivateFeedFormulaCommandHandler(FeedFormulaRepository formulaRepository, Clock clock) {
        this.formulaRepository = formulaRepository;
        this.clock = clock;
    }

    @Override
    public Result<FeedFormulaId> handle(ReactivateFeedFormulaCommand command) {
        Optional<FeedFormula> found = FeedFormulaId.parse(command.formulaId()).flatMap(formulaRepository::findById);
        if (found.isEmpty()) {
            return Result.failure(FarmRefusals.feedFormulaNotFound());
        }

        FeedFormula formula = found.get();
        // O que ja estava ativo nao muda, e gravar de novo so subiria a versao.
        if (formula.reactivate(clock)) {
            formulaRepository.save(formula);
        }
        return Result.success(formula.id());
    }
}
