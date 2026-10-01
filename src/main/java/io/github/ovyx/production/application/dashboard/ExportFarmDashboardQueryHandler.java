package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetFile;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetWriter;

/**
 * A planilha da granja toda num periodo (US4 da 009; R-009). Pede o painel da granja a mesma consulta da tela, e por
 * isso os numeros sao os dela por construcao (FR-018).
 */
public class ExportFarmDashboardQueryHandler implements QueryHandler<ExportFarmDashboardQuery, SpreadsheetFile> {

    private final GetFarmDashboardQueryHandler farms;
    private final SpreadsheetWriter writer;
    private final FarmCalendar calendar;

    public ExportFarmDashboardQueryHandler(
            GetFarmDashboardQueryHandler farms, SpreadsheetWriter writer, FarmCalendar calendar) {
        this.farms = farms;
        this.writer = writer;
        this.calendar = calendar;
    }

    @Override
    public Result<SpreadsheetFile> handle(ExportFarmDashboardQuery query) {
        return farms.handle(new GetFarmDashboardQuery(query.period())).map(farm -> {
            Spreadsheet spreadsheet = FarmDashboardSpreadsheet.of(farm, calendar.today(), calendar.now());
            return new SpreadsheetFile(spreadsheet.fileName(), writer.write(spreadsheet));
        });
    }
}
