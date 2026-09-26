package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.application.common.ProductionRefusals;
import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Lista os relatorios do setor, com o setor junto (FR-016). */
public class ListDailyReportsQueryHandler implements QueryHandler<ListDailyReportsQuery, DailyReportPage> {

    private static final int MAXIMUM_SIZE = 100;

    private final DailyReportDirectory directory;

    public ListDailyReportsQueryHandler(DailyReportDirectory directory) {
        this.directory = directory;
    }

    @Override
    public Result<DailyReportPage> handle(ListDailyReportsQuery query) {
        Map<String, String> violations = new LinkedHashMap<>();
        if (query.page() < 0) {
            violations.put("page", "A página começa em 0.");
        }
        if (query.size() < 1 || query.size() > MAXIMUM_SIZE) {
            violations.put("size", "O tamanho da página deve estar entre 1 e 100.");
        }
        if (!violations.isEmpty()) {
            return Result.failure(new ApplicationError(
                    ErrorType.VALIDATION,
                    ProductionErrorCode.VALIDATION_FAILED.code(),
                    "A requisição contém campos inválidos.",
                    violations));
        }
        Optional<SectorId> sectorId = SectorId.parse(query.sectorId());
        Optional<ReportingSector> sector = sectorId.flatMap(directory::sectorOf);
        if (sector.isEmpty()) {
            return Result.failure(ProductionRefusals.sectorNotFound());
        }
        return Result.success(new DailyReportPage(
                sector.get(),
                directory.list(sectorId.get(), query.collectionDate(), query.page(), query.size())));
    }
}
