package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.application.common.ProductionRefusals;
import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import java.util.Optional;

/**
 * Uma gaiola do relatorio, como estava na abertura, com os lancamentos: e o que o dialogo de lancamento
 * mostra. Cada identificador que nao aponta nada tem a sua recusa: o setor, o relatorio (inexistente ou
 * de outro setor) e a gaiola (inexistente ou fora do relatorio).
 */
public class FindReportCageQueryHandler implements QueryHandler<FindReportCageQuery, ReportCageDetail> {

    private final DailyReportDirectory directory;

    public FindReportCageQueryHandler(DailyReportDirectory directory) {
        this.directory = directory;
    }

    @Override
    public Result<ReportCageDetail> handle(FindReportCageQuery query) {
        Optional<SectorId> sectorId = SectorId.parse(query.sectorId()).filter(id -> directory.sectorOf(id).isPresent());
        if (sectorId.isEmpty()) {
            return Result.failure(ProductionRefusals.sectorNotFound());
        }
        Optional<DailyReportId> reportId =
                DailyReportId.parse(query.reportId()).filter(id -> directory.reportExists(sectorId.get(), id));
        if (reportId.isEmpty()) {
            return Result.failure(ProductionRefusals.dailyReportNotFound());
        }
        return CageId.parse(query.cageId())
                .flatMap(cageId -> directory.findCage(sectorId.get(), reportId.get(), cageId))
                .map(Result::success)
                .orElseGet(() -> Result.failure(ProductionRefusals.cageNotFound()));
    }
}
