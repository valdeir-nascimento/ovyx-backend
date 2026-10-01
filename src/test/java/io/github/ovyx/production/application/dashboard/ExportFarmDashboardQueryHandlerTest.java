package io.github.ovyx.production.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.fixtures.InMemoryDashboardDirectory;
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

/**
 * A exportação da granja toda (US4 da 009): a mesma consulta do painel da granja, entregue ao montador da planilha,
 * para os números serem os da tela por construção.
 */
@DisplayName("ExportFarmDashboardQueryHandler")
class ExportFarmDashboardQueryHandlerTest {

    private static final byte[] CONTENT = "PK planilha".getBytes(StandardCharsets.UTF_8);
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

    private final InMemoryDashboardDirectory directory = new InMemoryDashboardDirectory();
    /** 28/09/2026 às 21:40 na granja. */
    private final FarmCalendar calendar =
            new FarmCalendar(FixedClock.at("2026-09-29T00:40:00Z"), ZoneId.of("America/Sao_Paulo"));
    private Spreadsheet written;
    private final ExportFarmDashboardQueryHandler handler = new ExportFarmDashboardQueryHandler(
            new GetFarmDashboardQueryHandler(directory, calendar),
            spreadsheet -> {
                written = spreadsheet;
                return CONTENT;
            },
            calendar);

    private final ActiveSector codornas = directory.putActive(new ActiveSector(
            UUID.fromString("3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11"),
            "Codornas — Galpão 1",
            new LayingRateTarget(new BigDecimal("85"))));

    private static ReportDay day(LocalDate date, int eggs) {
        return new ReportDay(
                UUID.randomUUID(), date, 2000, 4, 4, 4, eggs, 0, 0, 0, 0, 0, 0, new BigDecimal("159600.00"), 0, true);
    }

    @Test
    @DisplayName("exports the farm of the period asked, as the dashboard of the farm reads it")
    void givenSectorsWithReports_whenExporting_thenWriteTheFarmOfThePeriod() {
        // given
        directory.addToFarm(codornas.id(), day(TODAY, 1740));

        // when
        Result<SpreadsheetFile> result = handler.handle(new ExportFarmDashboardQuery(DashboardPeriod.LAST_7_DAYS));

        // then
        assertThat(result.value()).isEqualTo(new SpreadsheetFile("painel-granja-28-09-2026.xlsx", CONTENT));
        assertThat(written.sheets().getFirst().heading())
                .contains(
                        "Ovyx — Painel da granja toda",
                        "Período: 7 dias, de 22/09/2026 a 28/09/2026, comparado com 15/09/2026 a 21/09/2026",
                        "Gerada em 28/09/2026 às 21:40 (horário da granja)");
    }

    @Test
    @DisplayName("exports today when no period is asked")
    void givenNoPeriod_whenExporting_thenExportToday() {
        // given
        ExportFarmDashboardQuery query = new ExportFarmDashboardQuery(null);

        // when
        handler.handle(query);

        // then
        assertThat(written.sheets().getFirst().heading())
                .contains("Período: hoje, 28/09/2026, comparado com 27/09/2026");
    }
}
