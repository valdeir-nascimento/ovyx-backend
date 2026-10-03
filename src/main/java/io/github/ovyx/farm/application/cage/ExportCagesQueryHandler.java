package io.github.ovyx.farm.application.cage;

import java.util.List;
import java.time.LocalDate;
import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.application.sector.SectorDetail;
import io.github.ovyx.farm.application.sector.SectorDirectory;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetFile;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetWriter;
import java.util.Locale;
import java.util.Optional;

/**
 * A planilha das gaiolas de um setor (US3 da 007; FR-022 a FR-024, R-009): as gaiolas que a lista mostra com a
 * busca e os filtros, de todas as paginas. Os filtros sao lidos como na pesquisa: a busca e a bateria em branco
 * nao filtram, a bateria vai em maiusculas, e sem situacao valem as ativas. O filtro "Pesagem pendente" e a
 * situacao de cada gaiola na agenda sao os da pesquisa (feature 010).
 */
public class ExportCagesQueryHandler implements QueryHandler<ExportCagesQuery, SpreadsheetFile> {

    private final CageDirectory cages;
    private final SectorDirectory sectors;
    private final SpreadsheetWriter writer;
    private final FarmCalendar calendar;

    public ExportCagesQueryHandler(
            CageDirectory cages, SectorDirectory sectors, SpreadsheetWriter writer, FarmCalendar calendar) {
        this.cages = cages;
        this.sectors = sectors;
        this.writer = writer;
        this.calendar = calendar;
    }

    @Override
    public Result<SpreadsheetFile> handle(ExportCagesQuery query) {
        Optional<SectorId> sectorId = SectorId.parse(query.sectorId());
        Optional<SectorDetail> sector = sectorId.flatMap(sectors::findDetail);
        if (sector.isEmpty()) {
            return Result.failure(FarmRefusals.sectorNotFound());
        }
        String code = blankToNull(query.code());
        String battery = blankToNull(query.battery());
        String upperBattery = battery == null ? null : battery.toUpperCase(Locale.ROOT);
        StatusFilter status = query.status() == null ? StatusFilter.ACTIVE : query.status();
        LocalDate today = calendar.today();
        SectorSchedule schedule = new SectorSchedule(sector.get().status(), sector.get().weighingDay());
        boolean pending = query.weighing() == WeighingFilter.PENDING;
        CageFilter filter =
                new CageFilter(code, upperBattery, status, pending ? schedule.schedule().requiredSince(today) : null);
        // Setor inativo nao tem gaiola a pesar.
        List<CageSummary> found = pending && !schedule.isActive() ? List.of() : cages.searchAll(sectorId.get(), filter);
        Spreadsheet spreadsheet = CagesSpreadsheet.of(
                sector.get(),
                filter,
                found.stream().map(cage -> schedule.standingOf(cage, today)).toList(),
                today,
                calendar.now());
        return Result.success(new SpreadsheetFile(spreadsheet.fileName(), writer.write(spreadsheet)));
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
