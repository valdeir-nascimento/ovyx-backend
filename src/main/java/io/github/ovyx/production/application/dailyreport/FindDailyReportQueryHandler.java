package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.application.common.ProductionRefusals;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import java.util.Optional;

/**
 * O relatorio com as gaiolas, os lancamentos e os totais do dia. O setor que nao existe e "setor nao
 * encontrado"; o relatorio que nao existe, malformado ou de outro setor, "relatorio nao encontrado".
 */
public class FindDailyReportQueryHandler implements QueryHandler<FindDailyReportQuery, DailyReportDetail> {

    private final DailyReportDirectory directory;

    public FindDailyReportQueryHandler(DailyReportDirectory directory) {
        this.directory = directory;
    }

    @Override
    public Result<DailyReportDetail> handle(FindDailyReportQuery query) {
        Optional<SectorId> sectorId = SectorId.parse(query.sectorId());
        if (sectorId.isEmpty()) {
            return Result.failure(ProductionRefusals.sectorNotFound());
        }
        Optional<DailyReportDetail> detail =
                DailyReportId.parse(query.reportId()).flatMap(reportId -> directory.findDetail(sectorId.get(), reportId));
        if (detail.isPresent()) {
            return Result.success(detail.get());
        }
        // So sem o relatorio se pergunta pelo setor: o detalhe ja o traz, e a leitura comum nao o consulta
        // duas vezes.
        return Result.failure(
                directory.sectorOf(sectorId.get()).isPresent()
                        ? ProductionRefusals.dailyReportNotFound()
                        : ProductionRefusals.sectorNotFound());
    }
}
