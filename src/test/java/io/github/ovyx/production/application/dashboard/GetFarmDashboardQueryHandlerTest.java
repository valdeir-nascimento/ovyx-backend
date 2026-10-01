package io.github.ovyx.production.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.fixtures.InMemoryDashboardDirectory;
import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O painel da granja toda (US1 da 009): cada caminho do {@link Result}, com a porta em memoria. */
@DisplayName("GetFarmDashboardQueryHandler")
class GetFarmDashboardQueryHandlerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);

    private final InMemoryDashboardDirectory directory = new InMemoryDashboardDirectory();
    private final GetFarmDashboardQueryHandler handler = new GetFarmDashboardQueryHandler(
            directory, new FarmCalendar(FixedClock.at("2026-09-24T10:12:40Z"), ZoneId.of("America/Sao_Paulo")));

    private final ActiveSector codornas = directory.putActive(new ActiveSector(
            UUID.fromString("3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11"),
            "Codornas — Galpão 1",
            new LayingRateTarget(new BigDecimal("85"))));
    private final ActiveSector poedeiras = directory.putActive(new ActiveSector(
            UUID.fromString("7e9a1c3e-5b7d-4f9a-8c1e-3b5d7f9a1c55"),
            "Poedeiras — Galpão 2",
            new LayingRateTarget(new BigDecimal("72"))));

    private static ReportDay day(LocalDate date, int birds, int eggs) {
        return new ReportDay(
                UUID.randomUUID(), date, birds, 4, 4, 4, eggs, 0, 0, 0, 0, 0, 0, new BigDecimal("159600.00"), 0, true);
    }

    @Test
    @DisplayName("gives the farm summed, reading the 14 days up to today")
    void givenTwoSectorsWithReports_whenReadingTheFarm_thenSumThemAndReadTwoWeeks() {
        // given
        directory.addToFarm(codornas.id(), day(TODAY, 2000, 1740));
        directory.addToFarm(poedeiras.id(), day(TODAY, 1200, 1160));

        // when
        Result<FarmDashboard> result = handler.handle(new GetFarmDashboardQuery(DashboardPeriod.TODAY));

        // then
        assertThat(result.value().indicators().production().value()).isEqualByComparingTo("2900");
        assertThat(result.value().activeSectors()).isEqualTo(2);
        assertThat(directory.askedFrom()).isEqualTo(LocalDate.of(2026, 9, 11));
        assertThat(directory.askedTo()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("reads today when no period is asked")
    void givenNoPeriod_whenReadingTheFarm_thenUseToday() {
        // given
        GetFarmDashboardQuery query = new GetFarmDashboardQuery(null);

        // when
        FarmDashboard farm = handler.handle(query).value();

        // then
        assertThat(farm.period()).isEqualTo(DashboardPeriod.TODAY);
    }

    @Test
    @DisplayName("gives the farm without data, and does not fail, when there is no active sector")
    void givenNoActiveSector_whenReadingTheFarm_thenGiveItEmpty() {
        // given
        GetFarmDashboardQueryHandler empty = new GetFarmDashboardQueryHandler(
                new InMemoryDashboardDirectory(),
                new FarmCalendar(FixedClock.at("2026-09-24T10:12:40Z"), ZoneId.of("America/Sao_Paulo")));

        // when
        Result<FarmDashboard> result = empty.handle(new GetFarmDashboardQuery(DashboardPeriod.TODAY));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().activeSectors()).isZero();
        assertThat(result.value().indicators().production().value()).isNull();
    }

    // ---------------------------------------------------------------- comparacao dos setores (US2)

    @Test
    @DisplayName("counts the open alerts of each sector with its report of today, its cages and its own target")
    void givenALowLayingCageAndAFeedPendingSector_whenReadingTheFarm_thenCountTheAlertsOfEachSector() {
        // given
        directory.addToFarm(codornas.id(), day(TODAY, 2000, 1740));
        directory.addToFarm(poedeiras.id(), new ReportDay(
                UUID.randomUUID(), TODAY, 1200, 4, 4, 3, 1160, 0, 0, 0, 0, 0, 0, new BigDecimal("120400.00"), 0, true));
        CageWatch c03 = new CageWatch(UUID.randomUUID(), "C-03", 0, 225, 300, 3, null, null);
        directory.answerCageWatch(new CageWatchReading(List.of(c03), null, new MortalityBaseline(0, 0)));

        // when
        List<FarmSectorRow> rows = handler.handle(new GetFarmDashboardQuery(DashboardPeriod.TODAY))
                .value()
                .sectors();

        // then
        assertThat(rows).extracting(FarmSectorRow::openAlerts).containsExactly(1, 1);
        assertThat(rows.get(0).sector().name()).isEqualTo("Codornas — Galpão 1");
    }
}
