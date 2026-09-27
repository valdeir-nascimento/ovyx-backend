package io.github.ovyx.farm.application.formula;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.domain.model.FeedFormula;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.farm.domain.port.FeedFormulaRepository;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;

import java.time.Clock;

public class RegisterFeedFormulaCommandHandler implements CommandHandler<RegisterFeedFormulaCommand, FeedFormulaId> {

    private final FeedFormulaRepository formulaRepository;
    private final Clock clock;

    public RegisterFeedFormulaCommandHandler(FeedFormulaRepository formulaRepository, Clock clock) {
        this.formulaRepository = formulaRepository;
        this.clock = clock;
    }

    @Override
    public Result<FeedFormulaId> handle(RegisterFeedFormulaCommand command) {
        FeedFormula formula;
        try {
            formula = FeedFormula.register(
                command.name(),
                command.pricePerKg(),
                command.expectedIntake(),
                command.description(),
                formulaRepository,
                clock
            );
        } catch (DomainException refusal) {
            return Result.failure(FarmRefusals.from(refusal));
        }

        formulaRepository.save(formula);
        return Result.success(formula.id());
    }
}
