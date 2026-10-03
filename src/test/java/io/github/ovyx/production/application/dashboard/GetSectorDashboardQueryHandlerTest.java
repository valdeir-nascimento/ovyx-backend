package io.github.ovyx.production.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.application.dailyreport.ReportingSector;
import io.github.ovyx.production.fixtures.InMemoryDashboardDirectory;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.production.domain.model.SectorId;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** O painel de um setor (US1 da 006; FR-003, FR-005, R-003): cada caminho do {@link Result}. */
@DisplayName("GetSectorDashboardQueryHandler")
class GetSectorDashboardQueryHandlerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);

    private final InMemoryDashboardDirectory directory = new InMemoryDashboardDirectory();
    private final GetSectorDashboardQueryHandler handler = new GetSectorDashboardQueryHandler(
            directory, new FarmCalendar(FixedClock.at("2026-09-24T10:12:40Z"), ZoneId.of("America/Sao_Paulo")));

    private final ReportingSector codornas = directory.put(
            new ReportingSector(UUID.fromString("3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11"), "Codornas — Galpão 1", "ACTIVE"));

    private static ReportDay day(LocalDate date, int eggs) {
        return new ReportDay(
                UUID.randomUUID(), date, 2000, 4, 4, 4, eggs, 0, 0, 0, 0, 0, 0, new BigDecimal("159600.00"), 0, true);
    }

    @Test
    @DisplayName("gives the dashboard of the sector, reading the 14 days up to today")
    void givenSectorWithReports_whenReadingTheDashboard_thenGiveItAndReadTwoWeeks() {
        // given
        directory.add(day(TODAY, 1740));
        directory.add(day(TODAY.minusDays(1), 1700));

        // when
        Result<SectorDashboard> result =
                handler.handle(new GetSectorDashboardQuery(codornas.id().toString(), DashboardPeriod.TODAY));

        // then
        assertThat(result.value().sector()).isEqualTo(codornas);
        assertThat(result.value().indicators().production().value()).isEqualByComparingTo("1740");
        assertThat(directory.askedFrom()).isEqualTo(LocalDate.of(2026, 9, 11));
        assertThat(directory.askedTo()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("gives the alerts of today, from the report of today and the cages of the sector")
    void givenCageOutOfTheRange_whenReadingTheDashboard_thenGiveItsAlertAndCountIt() {
        // given
        directory.add(day(TODAY, 1740));
        CageWatch a02 = new CageWatch(UUID.randomUUID(), "A-02", 0, 135, 150, 3, TODAY, new BigDecimal("150.8"));
        directory.answerCageWatch(
                new CageWatchReading(List.of(a02), new ReferenceWeight(155, 175), new MortalityBaseline(0, 0), null));

        // when
        SectorDashboard dashboard = handler.handle(
                        new GetSectorDashboardQuery(codornas.id().toString(), DashboardPeriod.TODAY))
                .value();

        // then
        assertThat(dashboard.alerts()).extracting(DashboardAlert::kind).containsExactly(AlertKind.WEIGHT_OUT_OF_RANGE);
        assertThat(dashboard.openAlerts()).isEqualTo(1);
    }

    @Test
    @DisplayName("reads today when no period is asked")
    void givenNoPeriod_whenReadingTheDashboard_thenUseToday() {
        // given
        GetSectorDashboardQuery query = new GetSectorDashboardQuery(codornas.id().toString(), null);

        // when
        SectorDashboard dashboard = handler.handle(query).value();

        // then
        assertThat(dashboard.period()).isEqualTo(DashboardPeriod.TODAY);
    }

    @Test
    @DisplayName("keeps an inactive sector readable")
    void givenInactiveSector_whenReadingTheDashboard_thenGiveIt() {
        // given
        ReportingSector inactive = directory.put(
                new ReportingSector(UUID.fromString("7e9a1c3e-5b7d-4f9a-8c1e-3b5d7f9a1c55"), "Setor Inativo", "INACTIVE"));

        // when
        Result<SectorDashboard> result =
                handler.handle(new GetSectorDashboardQuery(inactive.id().toString(), DashboardPeriod.LAST_7_DAYS));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().sector().status()).isEqualTo("INACTIVE");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"4e6a8c0e-2b4d-4f6a-8c0e-2b4d6f8a0c44", "galpao-9"})
    @DisplayName("fails as not found for a sector that does not exist or is malformed")
    void givenUnknownSector_whenReadingTheDashboard_thenFailAsSectorNotFound(String sectorId) {
        // given
        GetSectorDashboardQuery query = new GetSectorDashboardQuery(sectorId, DashboardPeriod.TODAY);

        // when
        Result<SectorDashboard> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
        assertThat(directory.askedTargets()).isZero();
    }

    @Test
    @DisplayName("gives the 4 latest reports of the sector, asking the port for 4")
    void givenReports_whenReadingTheDashboard_thenAskForTheFourLatest() {
        // given
        GetSectorDashboardQuery query = new GetSectorDashboardQuery(codornas.id().toString(), DashboardPeriod.TODAY);

        // when
        handler.handle(query);

        // then
        assertThat(directory.askedLatest()).isEqualTo(4);
    }

    // ---------------------------------------------------------------- meta do setor (008)

    @Test
    @DisplayName("uses the target of the sector in the chart and in the low laying alert")
    void givenSectorWithATargetOf72_whenReadingTheDashboard_thenUseItInTheChartAndInTheAlerts() {
        // given
        directory.answerLayingRateTarget(SectorId.of(codornas.id()), "72");
        directory.add(day(TODAY, 1580));
        CageWatch c03 = new CageWatch(UUID.randomUUID(), "C-03", 0, 225, 300, 3, null, null);
        directory.answerCageWatch(new CageWatchReading(List.of(c03), null, new MortalityBaseline(0, 0), null));

        // when
        SectorDashboard dashboard = handler.handle(
                        new GetSectorDashboardQuery(codornas.id().toString(), DashboardPeriod.TODAY))
                .value();

        // then
        assertThat(dashboard.target()).isEqualByComparingTo("72.00");
        assertThat(dashboard.targetStatus()).isEqualTo(TargetStatus.ABOVE);
        assertThat(dashboard.alerts()).extracting(DashboardAlert::kind).doesNotContain(AlertKind.LOW_LAYING);
    }
}
