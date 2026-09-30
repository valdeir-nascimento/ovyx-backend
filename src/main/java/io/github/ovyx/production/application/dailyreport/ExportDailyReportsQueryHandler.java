package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.application.common.ProductionRefusals;
import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetFile;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetWriter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A planilha dos relatorios de um setor num intervalo (US1 da 007; FR-012 a FR-018, R-006).
 *
 * <p>O intervalo e recusado com todas as falhas de uma vez, antes de perguntar pelo setor, como os filtros da
 * lista de relatorios: as duas datas obrigatorias, a final igual ou depois da inicial, e no maximo 366 dias
 * contando as duas pontas. O setor inativo e exportado, porque os relatorios dele continuam consultaveis.
 */
public class ExportDailyReportsQueryHandler implements QueryHandler<ExportDailyReportsQuery, SpreadsheetFile> {

    /** O fechamento do mes e o do ano, e a planilha de um setor grande dentro do que o Excel abre (R-006). */
    private static final int LONGEST_INTERVAL = 366;

    private final DailyReportDirectory directory;
    private final SpreadsheetWriter writer;
    private final FarmCalendar calendar;

    public ExportDailyReportsQueryHandler(DailyReportDirectory directory, SpreadsheetWriter writer, FarmCalendar calendar) {
        this.directory = directory;
        this.writer = writer;
        this.calendar = calendar;
    }

    @Override
    public Result<SpreadsheetFile> handle(ExportDailyReportsQuery query) {
        Map<String, String> violations = violationsOf(query);
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
        Spreadsheet spreadsheet = DailyReportsSpreadsheet.of(
                sector.get(),
                query.from(),
                query.to(),
                directory.detailsBetween(sectorId.get(), query.from(), query.to()),
                calendar.today(),
                calendar.now());
        return Result.success(new SpreadsheetFile(spreadsheet.fileName(), writer.write(spreadsheet)));
    }

    private static Map<String, String> violationsOf(ExportDailyReportsQuery query) {
        Map<String, String> violations = new LinkedHashMap<>();
        if (query.from() == null) {
            violations.put("from", "Informe a data inicial.");
        }
        if (query.to() == null) {
            violations.put("to", "Informe a data final.");
        }
        if (!violations.isEmpty()) {
            return violations;
        }
        if (query.to().isBefore(query.from())) {
            violations.put("to", "A data final deve ser igual ou posterior à inicial.");
        } else if (ChronoUnit.DAYS.between(query.from(), query.to()) + 1 > LONGEST_INTERVAL) {
            violations.put("to", "O intervalo deve ter no máximo 366 dias.");
        }
        return violations;
    }
}
