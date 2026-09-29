package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import java.util.UUID;

/** O relatorio de hoje do setor e a situacao de cada lancamento. */
public record TodayReport(
        UUID id, ProductionStatus productionStatus, FeedStatus feedStatus, MortalityStatus mortalityStatus) {

    static TodayReport of(ReportDay day) {
        return new TodayReport(day.reportId(), day.productionStatus(), day.feedStatus(), day.mortalityStatus());
    }
}
