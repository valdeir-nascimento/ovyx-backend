package io.github.ovyx.production.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.application.dailyreport.ReportingSector;
import io.github.ovyx.production.fixtures.InMemoryDashboardDirectory;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetFile;
import io.github.ovyx.shared.domain.FixedClock;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A exportação do painel (US2 da 007; FR-019 a FR-021, R-008): a mesma consulta do painel, entregue ao montador
 * da planilha, com a recusa do painel passada adiante como está.
 */
@DisplayName("ExportSectorDashboardQueryHandler")
class ExportSectorDashboardQueryHandlerTest {

    private static final byte[] CONTENT = "PK planilha".getBytes(StandardCharsets.UTF_8);
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

    private final InMemoryDashboardDirectory directory = new InMemoryDashboardDirectory();
    /** 28/09/2026 às 21:40 na granja. */
    private final FarmCalendar calendar =
            new FarmCalendar(FixedClock.at("2026-09-29T00:40:00Z"), ZoneId.of("America/Sao_Paulo"));
    private Spreadsheet written;
    private final ExportSectorDashboardQueryHandler handler = new ExportSectorDashboardQueryHandler(
            new GetSectorDashboardQueryHandler(directory, calendar),
            spreadsheet -> {
                written = spreadsheet;
                return CONTENT;
            },
            calendar);

    private final ReportingSector codornas = directory.put(
            new ReportingSector(UUID.fromString("3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11"), "Codornas — Galpão 1", "ACTIVE"));

    private static ReportDay day(LocalDate date, int eggs) {
        return new ReportDay(
                UUID.randomUUID(), date, 2000, 4, 4, 4, eggs, 0, 0, 0, 0, 0, 0, new BigDecimal("159600.00"), 0, true);
    }

    @Test
    @DisplayName("exports the dashboard of the period asked, as the dashboard reads it")
    void givenSectorWithReports_whenExporting_thenWriteTheDashboardOfThePeriod() {
        // given
        directory.add(day(TODAY, 1740));
        directory.add(day(TODAY.minusDays(1), 1700));

        // when
        Result<SpreadsheetFile> result = handler.handle(
                new ExportSectorDashboardQuery(codornas.id().toString(), DashboardPeriod.LAST_7_DAYS));

        // then
        assertThat(result.value()).isEqualTo(new SpreadsheetFile("painel-codornas-galpao-1-28-09-2026.xlsx", CONTENT));
        assertThat(written.sheets().getFirst().heading())
                .contains(
                        "Período: 7 dias, de 22/09/2026 a 28/09/2026, comparado com 15/09/2026 a 21/09/2026",
                        "Gerada em 28/09/2026 às 21:40 (horário da granja)");
    }

    @Test
    @DisplayName("exports today when no period is asked")
    void givenNoPeriod_whenExporting_thenExportToday() {
        // given
        ExportSectorDashboardQuery query = new ExportSectorDashboardQuery(codornas.id().toString(), null);

        // when
        handler.handle(query);

        // then
        assertThat(written.sheets().getFirst().heading()).contains("Período: hoje, 28/09/2026, comparado com 27/09/2026");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"7e9a1c3e-5b7d-4f9a-8c1e-3b5d7f9a1c55", "galpao-9"})
    @DisplayName("passes on the refusal of the dashboard for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenExporting_thenFailAsSectorNotFound(String sector) {
        // given
        ExportSectorDashboardQuery query = new ExportSectorDashboardQuery(sector, DashboardPeriod.TODAY);

        // when
        Result<SpreadsheetFile> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
        assertThat(written).isNull();
    }
}
