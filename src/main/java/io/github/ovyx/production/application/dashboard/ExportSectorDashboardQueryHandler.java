package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetFile;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetWriter;

/**
 * A planilha do painel de um setor num periodo (US2 da 007; FR-019 a FR-021, R-008).
 *
 * <p>Pede o painel a mesma consulta da tela, e por isso os numeros sao os dela por construcao (FR-003). A recusa
 * do painel, como o setor inexistente, passa adiante como esta.
 */
public class ExportSectorDashboardQueryHandler implements QueryHandler<ExportSectorDashboardQuery, SpreadsheetFile> {

    private final GetSectorDashboardQueryHandler dashboards;
    private final SpreadsheetWriter writer;
    private final FarmCalendar calendar;

    public ExportSectorDashboardQueryHandler(
            GetSectorDashboardQueryHandler dashboards, SpreadsheetWriter writer, FarmCalendar calendar) {
        this.dashboards = dashboards;
        this.writer = writer;
        this.calendar = calendar;
    }

    @Override
    public Result<SpreadsheetFile> handle(ExportSectorDashboardQuery query) {
        Result<SectorDashboard> dashboard =
                dashboards.handle(new GetSectorDashboardQuery(query.sectorId(), query.period()));
        if (dashboard.isFailure()) {
            return Result.failure(dashboard.error());
        }
        Spreadsheet spreadsheet = DashboardSpreadsheet.of(dashboard.value(), calendar.today(), calendar.now());
        return Result.success(new SpreadsheetFile(spreadsheet.fileName(), writer.write(spreadsheet)));
    }
}
