package io.github.ovyx.farm.application.formula;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;

/**
 * Consulta uma formula, ativa ou inativa, com o custo por ave ao dia.
 */
public class FindFeedFormulaQueryHandler implements QueryHandler<FindFeedFormulaQuery, FeedFormulaSummary> {

    private final FeedFormulaDirectory formulaDirectory;

    public FindFeedFormulaQueryHandler(FeedFormulaDirectory formulaDirectory) {
        this.formulaDirectory = formulaDirectory;
    }

    @Override
    public Result<FeedFormulaSummary> handle(FindFeedFormulaQuery query) {
        return FeedFormulaId.parse(query.formulaId())
            .flatMap(formulaDirectory::findById)
            .map(Result::success)
            .orElseGet(() -> Result.failure(FarmRefusals.feedFormulaNotFound()));
    }
}
