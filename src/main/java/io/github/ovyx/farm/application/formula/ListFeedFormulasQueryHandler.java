package io.github.ovyx.farm.application.formula;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;

import java.util.List;

public class ListFeedFormulasQueryHandler implements QueryHandler<ListFeedFormulasQuery, List<FeedFormulaSummary>> {

    private final FeedFormulaDirectory formulaDirectory;

    public ListFeedFormulasQueryHandler(FeedFormulaDirectory formulaDirectory) {
        this.formulaDirectory = formulaDirectory;
    }

    @Override
    public Result<List<FeedFormulaSummary>> handle(ListFeedFormulasQuery query) {
        StatusFilter status = query.status() == null ? StatusFilter.ACTIVE : query.status();
        return Result.success(formulaDirectory.list(status));
    }
}
