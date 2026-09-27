package io.github.ovyx.production.integration;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.MARINA;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.production.application.dailyreport.ConfirmNoMortalityCommand;
import io.github.ovyx.production.application.dailyreport.CorrectDailyReportCommand;
import io.github.ovyx.production.application.dailyreport.OpenDailyReportCommand;
import io.github.ovyx.production.application.dailyreport.RecordMortalityCommand;
import io.github.ovyx.production.application.dailyreport.RecordProductionCommand;
import io.github.ovyx.shared.application.Dispatcher;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Nada é apagado (FR-021): o relatório, as gaiolas dele e os lançamentos continuam no banco depois de
 * abrir, lançar, corrigir e inativar o setor. A inativação do setor tira o relatório da escrita, e não
 * da consulta.
 */
@DisplayName("Daily reports kept")
class DailyReportsKeptIT extends IntegrationTestSupport {

    @Autowired
    private Dispatcher dispatcher;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("keeps every row of the report and of its cages, with the entries, after the sector is deactivated")
    void givenReportWithEntries_whenTheSectorIsDeactivated_thenKeepEveryRow() {
        // given
        ProductionFixtures fixtures = new ProductionFixtures(jdbc);
        UUID sectorId = fixtures.activeSector();
        UUID a01 = fixtures.activeCage(sectorId, "A", 1, 48);
        UUID b07 = fixtures.activeCage(sectorId, "B", 7, 50);
        String sector = sectorId.toString();
        String report = dispatcher
                .dispatch(new OpenDailyReportCommand(sector, "2026-09-24", "06:30", "98", "20", null, MARINA))
                .value()
                .toString();
        dispatcher.dispatch(new RecordProductionCommand(
                sector, report, a01.toString(), "44", null, null, null, "1", null, null, MARINA));
        dispatcher.dispatch(new RecordMortalityCommand(sector, report, b07.toString(), "1", "1", "Prostração.", MARINA));
        dispatcher.dispatch(new CorrectDailyReportCommand(sector, report, "2026-09-24", "06:45", "97", "21", null, JOAO));
        dispatcher.dispatch(new OpenDailyReportCommand(sector, "2026-09-23", "06:30", "98", "20", null, MARINA));
        dispatcher.dispatch(new ConfirmNoMortalityCommand(sector, report, JOAO));

        // when
        fixtures.deactivate(sectorId);

        // then
        assertThat(jdbc.queryForObject("select count(*) from daily_report where sector_id = ?", Integer.class, sectorId))
                .isEqualTo(2);
        List<Map<String, Object>> cages = jdbc.queryForList(
                "select eggs, cracked, deaths, culls, mortality_note from report_cage where report_id = ?"
                        + " order by battery, number",
                UUID.fromString(report));
        assertThat(cages).hasSize(2);
        assertThat(cages.get(0)).containsEntry("eggs", 44).containsEntry("cracked", 1);
        assertThat(cages.get(1))
                .containsEntry("deaths", 1)
                .containsEntry("culls", 1)
                .containsEntry("mortality_note", "Prostração.");
        assertThat(jdbc.queryForMap(
                        "select opening_bird_count, flock_age, last_corrected_by_name from daily_report where id = ?",
                        UUID.fromString(report)))
                .containsEntry("opening_bird_count", 97)
                .containsEntry("flock_age", 21)
                .containsEntry("last_corrected_by_name", "João Pereira");
    }
}
