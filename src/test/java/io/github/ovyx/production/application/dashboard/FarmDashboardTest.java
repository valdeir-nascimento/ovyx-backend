package io.github.ovyx.production.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O painel da granja toda (feature 009): os setores ativos somados, com as contas do painel de um setor, sem banco.
 * Os numeros conferem com a conta feita a mao a partir das abas dos setores (FR-019, SC-002).
 */
@DisplayName("FarmDashboard")
class FarmDashboardTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);

    private static final ActiveSector GALPAO_1 = new ActiveSector(
            UUID.fromString("3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11"),
            "Codornas — Galpão 1",
            new LayingRateTarget(new BigDecimal("85")));
    private static final ActiveSector GALPAO_4 = new ActiveSector(
            UUID.fromString("5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33"),
            "Codornas — Galpão 4",
            new LayingRateTarget(new BigDecimal("85")));
    private static final ActiveSector GALPAO_2 = new ActiveSector(
            UUID.fromString("7e9a1c3e-5b7d-4f9a-8c1e-3b5d7f9a1c55"),
            "Poedeiras — Galpão 2",
            new LayingRateTarget(new BigDecimal("72")));

    /** Um relatorio completo de quatro gaiolas, com o custo da racao do dia em reais. */
    private static ReportDay day(LocalDate date, int birds, int eggs, String feedCost) {
        return new ReportDay(
                UUID.randomUUID(),
                date,
                birds,
                4,
                4,
                4,
                eggs,
                0,
                0,
                0,
                0,
                0,
                0,
                new BigDecimal(feedCost).multiply(BigDecimal.valueOf(1000)),
                0,
                true);
    }

    /** O mesmo relatorio, com a racao lancada em so uma das quatro gaiolas. */
    private static ReportDay feedPending(ReportDay day) {
        return new ReportDay(
                day.reportId(),
                day.date(),
                day.openingBirdCount(),
                4,
                day.cagesWithProduction(),
                1,
                day.eggs(),
                0,
                0,
                0,
                0,
                0,
                0,
                day.exactFeedCost(),
                0,
                true);
    }

    /** O Galpao 1 e o Galpao 2 com os relatorios de hoje e de ontem; o Galpao 4 sem nenhum. */
    private static Map<UUID, List<ReportDay>> twoSectorsTodayAndYesterday() {
        Map<UUID, List<ReportDay>> days = new LinkedHashMap<>();
        days.put(GALPAO_1.id(), List.of(day(TODAY.minusDays(1), 2000, 1700, "157.25"), day(TODAY, 2000, 1740, "159.60")));
        days.put(GALPAO_2.id(), List.of(day(TODAY.minusDays(1), 1200, 1160, "120.40"), day(TODAY, 1200, 1160, "120.40")));
        return days;
    }

    private static FarmDashboard farm(DashboardPeriod period, Map<UUID, List<ReportDay>> days) {
        return FarmDashboard.of(period, TODAY, List.of(GALPAO_1, GALPAO_4, GALPAO_2), days, Map.of());
    }

    @Test
    @DisplayName("sums the reports of today of the active sectors, with the sum of eggs over the sum of birds")
    void givenTwoSectorsWithTheReportOfToday_whenBuildingToday_thenSumThemAsTheTabsWould() {
        // given
        Map<UUID, List<ReportDay>> days = twoSectorsTodayAndYesterday();

        // when
        FarmDashboard farm = farm(DashboardPeriod.TODAY, days);

        // then
        Indicators indicators = farm.indicators();
        assertThat(indicators.production().value()).isEqualByComparingTo("2900");
        assertThat(indicators.production().previous()).isEqualByComparingTo("2860");
        assertThat(indicators.production().change()).isEqualByComparingTo("1.4");
        assertThat(indicators.layingRate().value()).isEqualByComparingTo("90.63");
        assertThat(indicators.feedCost().value()).isEqualByComparingTo("280.00");
        assertThat(indicators.costPerEgg().value()).isEqualByComparingTo("0.097");
        assertThat(farm.from()).isEqualTo(TODAY);
        assertThat(farm.to()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("counts the active sectors and the ones with a report in the period, without counting the others as zero")
    void givenThreeActiveSectorsAndTwoWithReports_whenBuilding_thenCountTwoOfThree() {
        // given
        Map<UUID, List<ReportDay>> days = twoSectorsTodayAndYesterday();

        // when
        FarmDashboard farm = farm(DashboardPeriod.TODAY, days);

        // then
        assertThat(farm.activeSectors()).isEqualTo(3);
        assertThat(farm.reportingSectors()).isEqualTo(2);
    }

    @Test
    @DisplayName("leaves a sector with the feed pending out of the cost, counting one report out")
    void givenOneSectorWithTheFeedPendingToday_whenBuilding_thenCostOnlyTheOther() {
        // given
        Map<UUID, List<ReportDay>> days = new LinkedHashMap<>(twoSectorsTodayAndYesterday());
        days.put(GALPAO_2.id(), List.of(feedPending(day(TODAY, 1200, 1160, "120.40"))));

        // when
        FarmDashboard farm = farm(DashboardPeriod.TODAY, days);

        // then
        assertThat(farm.indicators().production().value()).isEqualByComparingTo("2900");
        assertThat(farm.indicators().feedCost().value()).isEqualByComparingTo("159.60");
        assertThat(farm.indicators().feedCost().incompleteDays()).isEqualTo(1);
        assertThat(farm.indicators().costPerEgg().value()).isEqualByComparingTo("0.092");
    }

    @Test
    @DisplayName("reads yesterday against the day before, and the 7 days against the 7 before")
    void givenReportsOfTwoWeeks_whenBuildingYesterdayAndTheSevenDays_thenCompareWithThePeriodBefore() {
        // given
        Map<UUID, List<ReportDay>> days = new LinkedHashMap<>();
        days.put(GALPAO_1.id(), List.of(
                day(TODAY.minusDays(13), 2000, 1600, "150.00"),
                day(TODAY.minusDays(2), 2000, 1650, "155.00"),
                day(TODAY.minusDays(1), 2000, 1700, "157.25")));
        days.put(GALPAO_2.id(), List.of(day(TODAY.minusDays(1), 1200, 1160, "120.40")));

        // when
        FarmDashboard yesterday = farm(DashboardPeriod.YESTERDAY, days);
        FarmDashboard week = farm(DashboardPeriod.LAST_7_DAYS, days);

        // then
        assertThat(yesterday.indicators().production().value()).isEqualByComparingTo("2860");
        assertThat(yesterday.indicators().production().previous()).isEqualByComparingTo("1650");
        assertThat(week.indicators().production().value()).isEqualByComparingTo("4510");
        assertThat(week.indicators().production().previous()).isEqualByComparingTo("1600");
        assertThat(week.reportingSectors()).isEqualTo(2);
    }

    @Test
    @DisplayName("leaves the indicators without data and without comparison when there is no report")
    void givenNoReport_whenBuilding_thenLeaveTheIndicatorsEmpty() {
        // given
        Map<UUID, List<ReportDay>> days = Map.of();

        // when
        FarmDashboard farm = farm(DashboardPeriod.TODAY, days);

        // then
        assertThat(farm.indicators().production().value()).isNull();
        assertThat(farm.indicators().production().change()).isNull();
        assertThat(farm.indicators().layingRate().value()).isNull();
        assertThat(farm.reportingSectors()).isZero();
        assertThat(farm.activeSectors()).isEqualTo(3);
    }

    @Test
    @DisplayName("ignores the reports of a sector that is not among the active ones")
    void givenReportsOfASectorOutOfTheActiveOnes_whenBuilding_thenIgnoreThem() {
        // given
        Map<UUID, List<ReportDay>> days = new LinkedHashMap<>(twoSectorsTodayAndYesterday());
        days.put(UUID.randomUUID(), List.of(day(TODAY, 5000, 5000, "500.00")));

        // when
        FarmDashboard farm = farm(DashboardPeriod.TODAY, days);

        // then
        assertThat(farm.indicators().production().value()).isEqualByComparingTo("2900");
        assertThat(farm.reportingSectors()).isEqualTo(2);
    }

    @Test
    @DisplayName("gives the 7 days summed, with the sectors with report on each day")
    void givenReportsOnSomeDays_whenBuilding_thenGiveTheSevenDaysWithTheSectorsOfEach() {
        // given
        Map<UUID, List<ReportDay>> days = twoSectorsTodayAndYesterday();

        // when
        List<FarmDay> trend = farm(DashboardPeriod.TODAY, days).trend();

        // then
        assertThat(trend).hasSize(7);
        assertThat(trend).extracting(FarmDay::date).startsWith(TODAY.minusDays(6)).endsWith(TODAY);
        assertThat(trend.getLast().production()).isEqualTo(2900);
        assertThat(trend.getLast().layingRate()).isEqualByComparingTo("90.63");
        assertThat(trend.getLast().reportingSectors()).isEqualTo(2);
        assertThat(trend.getFirst().production()).isNull();
        assertThat(trend.getFirst().reportingSectors()).isZero();
    }

    // ---------------------------------------------------------------- comparacao dos setores (US2)

    @Test
    @DisplayName("gives one row per active sector, in the order of the tabs, with the numbers of the period of each")
    void givenThreeActiveSectors_whenBuilding_thenGiveOneRowPerSectorInTheOrderOfTheTabs() {
        // given
        Map<UUID, List<ReportDay>> days = twoSectorsTodayAndYesterday();

        // when
        List<FarmSectorRow> rows = FarmDashboard.of(
                        DashboardPeriod.TODAY,
                        TODAY,
                        List.of(GALPAO_1, GALPAO_4, GALPAO_2),
                        days,
                        Map.of(GALPAO_1.id(), 3, GALPAO_2.id(), 1))
                .sectors();

        // then
        assertThat(rows).extracting(row -> row.sector().id()).containsExactly(GALPAO_1.id(), GALPAO_4.id(), GALPAO_2.id());
        FarmSectorRow first = rows.get(0);
        assertThat(first.sector().name()).isEqualTo("Codornas — Galpão 1");
        assertThat(first.production()).isEqualTo(1740);
        assertThat(first.layingRate()).isEqualByComparingTo("87.00");
        assertThat(first.costPerEgg()).isEqualByComparingTo("0.092");
        assertThat(first.target()).isEqualByComparingTo("85.00").hasScaleOf(2);
        assertThat(first.targetStatus()).isEqualTo(TargetStatus.ABOVE);
        assertThat(first.todayReport()).isNotNull();
        assertThat(first.openAlerts()).isEqualTo(3);
        assertThat(rows.get(2).openAlerts()).isEqualTo(1);
    }

    @Test
    @DisplayName("compares the period of each sector with its own target, the target included")
    void givenSectorsAroundTheirTargets_whenBuilding_thenCompareEachWithItsOwnTarget() {
        // given
        Map<UUID, List<ReportDay>> days = new LinkedHashMap<>();
        days.put(GALPAO_1.id(), List.of(day(TODAY, 2000, 1680, "157.25")));
        days.put(GALPAO_2.id(), List.of(day(TODAY, 1200, 864, "120.40")));
        Map<UUID, List<ReportDay>> below = new LinkedHashMap<>();
        below.put(GALPAO_2.id(), List.of(day(TODAY, 1200, 840, "120.40")));

        // when
        List<FarmSectorRow> rows = farm(DashboardPeriod.TODAY, days).sectors();
        FarmSectorRow belowItsTarget = farm(DashboardPeriod.TODAY, below).sectors().get(2);

        // then
        assertThat(rows.get(0).layingRate()).isEqualByComparingTo("84.00");
        assertThat(rows.get(0).targetStatus()).isEqualTo(TargetStatus.BELOW);
        assertThat(rows.get(2).layingRate()).isEqualByComparingTo("72.00");
        assertThat(rows.get(2).targetStatus()).isEqualTo(TargetStatus.ABOVE);
        assertThat(belowItsTarget.layingRate()).isEqualByComparingTo("70.00");
        assertThat(belowItsTarget.targetStatus()).isEqualTo(TargetStatus.BELOW);
    }

    @Test
    @DisplayName("keeps a sector without report in the period, with the numbers and the status absent")
    void givenASectorWithoutReport_whenBuilding_thenKeepItsRowWithTheNumbersAbsent() {
        // given
        Map<UUID, List<ReportDay>> days = twoSectorsTodayAndYesterday();

        // when
        FarmSectorRow withoutReport = farm(DashboardPeriod.TODAY, days).sectors().get(1);

        // then
        assertThat(withoutReport.sector().id()).isEqualTo(GALPAO_4.id());
        assertThat(withoutReport.production()).isNull();
        assertThat(withoutReport.layingRate()).isNull();
        assertThat(withoutReport.costPerEgg()).isNull();
        assertThat(withoutReport.targetStatus()).isNull();
        assertThat(withoutReport.todayReport()).isNull();
        assertThat(withoutReport.target()).isEqualByComparingTo("85.00");
        assertThat(withoutReport.openAlerts()).isZero();
    }

    @Test
    @DisplayName("gives the report of today of a sector even in a period that does not include today")
    void givenTheReportOfToday_whenBuildingYesterday_thenKeepItInTheRow() {
        // given
        Map<UUID, List<ReportDay>> days = twoSectorsTodayAndYesterday();

        // when
        FarmSectorRow row = farm(DashboardPeriod.YESTERDAY, days).sectors().get(0);

        // then
        assertThat(row.production()).isEqualTo(1700);
        assertThat(row.todayReport()).isNotNull();
    }

    // ---------------------------------------------------------------- meta da granja e classificacao (US3)

    /** O mesmo relatorio, com tantos ovos pequenos. */
    private static ReportDay withSmall(ReportDay day, int small) {
        return new ReportDay(
                day.reportId(),
                day.date(),
                day.openingBirdCount(),
                4,
                4,
                4,
                day.eggs(),
                small,
                0,
                0,
                0,
                0,
                0,
                day.exactFeedCost(),
                0,
                true);
    }

    @Test
    @DisplayName("gives each day the target of the sectors reporting on it, weighted by their birds")
    void givenADayWithBothSectorsAndADayWithOne_whenBuilding_thenGiveEachDayItsOwnTarget() {
        // given
        Map<UUID, List<ReportDay>> days = new LinkedHashMap<>(twoSectorsTodayAndYesterday());
        days.put(GALPAO_1.id(), List.of(
                day(TODAY.minusDays(2), 2000, 1650, "155.00"),
                day(TODAY.minusDays(1), 2000, 1700, "157.25"),
                day(TODAY, 2000, 1740, "159.60")));

        // when
        FarmDashboard farm = farm(DashboardPeriod.TODAY, days);

        // then
        List<FarmDay> trend = farm.trend();
        assertThat(trend.get(6).target()).isEqualByComparingTo("80.13");
        assertThat(trend.get(4).target()).isEqualByComparingTo("85.00");
        assertThat(trend.get(4).reportingSectors()).isEqualTo(1);
        assertThat(trend.get(0).target()).isNull();
        assertThat(farm.target()).isEqualByComparingTo("81.29");
    }

    @Test
    @DisplayName("compares the last day with a report with the target of that day")
    void givenTheLastDayAboveOrBelowItsTarget_whenBuilding_thenSayAboveOrBelow() {
        // given
        Map<UUID, List<ReportDay>> above = twoSectorsTodayAndYesterday();
        Map<UUID, List<ReportDay>> below = new LinkedHashMap<>();
        below.put(GALPAO_1.id(), List.of(day(TODAY, 2000, 1500, "157.25")));
        below.put(GALPAO_2.id(), List.of(day(TODAY, 1200, 840, "120.40")));

        // when
        FarmDashboard aboveIt = farm(DashboardPeriod.TODAY, above);
        FarmDashboard belowIt = farm(DashboardPeriod.TODAY, below);

        // then
        assertThat(aboveIt.targetStatus()).isEqualTo(TargetStatus.ABOVE);
        assertThat(belowIt.trend().get(6).layingRate()).isEqualByComparingTo("73.13");
        assertThat(belowIt.targetStatus()).isEqualTo(TargetStatus.BELOW);
    }

    @Test
    @DisplayName("has no target nor status without a report in the 7 days")
    void givenNoReportInTheSevenDays_whenBuilding_thenHaveNoTargetNorStatus() {
        // given
        Map<UUID, List<ReportDay>> days = Map.of(GALPAO_1.id(), List.of(day(TODAY.minusDays(9), 2000, 1700, "157.25")));

        // when
        FarmDashboard farm = farm(DashboardPeriod.TODAY, days);

        // then
        assertThat(farm.target()).isNull();
        assertThat(farm.targetStatus()).isNull();
    }

    @Test
    @DisplayName("sums the grading of the sectors, with the percent over the eggs summed")
    void givenGradedReportsOfTwoSectors_whenBuilding_thenSumTheGrading() {
        // given
        Map<UUID, List<ReportDay>> days = new LinkedHashMap<>();
        days.put(GALPAO_1.id(), List.of(withSmall(day(TODAY, 2000, 1740, "159.60"), 30)));
        days.put(GALPAO_2.id(), List.of(withSmall(day(TODAY, 1200, 1160, "120.40"), 18)));

        // when
        EggGrading grades = farm(DashboardPeriod.TODAY, days).grades();

        // then
        assertThat(grades.collected()).isEqualTo(2900);
        assertThat(grades.standard().count()).isEqualTo(2852);
        assertThat(grades.standard().percent()).isEqualByComparingTo("98.3");
        assertThat(grades.shares().getFirst().count()).isEqualTo(48);
        assertThat(grades.shares().getFirst().percent()).isEqualByComparingTo("1.7");
    }

    // ---------------------------------------------------------------- casos da mutacao (T038)

    @Test
    @DisplayName("says above the target when the last day is exactly on the target of the farm")
    void givenTheLastDayExactlyOnTheTarget_whenBuilding_thenSayAbove() {
        // given
        Map<UUID, List<ReportDay>> days = Map.of(GALPAO_1.id(), List.of(day(TODAY, 2000, 1700, "157.25")));

        // when
        FarmDashboard farm = farm(DashboardPeriod.TODAY, days);

        // then
        assertThat(farm.trend().getLast().layingRate()).isEqualByComparingTo("85.00");
        assertThat(farm.trend().getLast().target()).isEqualByComparingTo("85.00");
        assertThat(farm.targetStatus()).isEqualTo(TargetStatus.ABOVE);
    }

    @Test
    @DisplayName("does not count a sector whose reports are all out of the period")
    void givenASectorWithReportsOnlyBeforeThePeriod_whenBuildingToday_thenLeaveItOutOfTheCount() {
        // given
        Map<UUID, List<ReportDay>> days = new LinkedHashMap<>();
        days.put(GALPAO_1.id(), List.of(day(TODAY, 2000, 1740, "159.60")));
        days.put(GALPAO_2.id(), List.of(day(TODAY.minusDays(10), 1200, 1160, "120.40")));

        // when
        FarmDashboard farm = farm(DashboardPeriod.TODAY, days);

        // then
        assertThat(farm.reportingSectors()).isEqualTo(1);
    }

    @Test
    @DisplayName("gives the report of today in the row, and not the one of the period, when the period is yesterday")
    void givenReportsOfTodayAndYesterday_whenBuildingYesterday_thenGiveTheReportOfTodayInTheRow() {
        // given
        ReportDay yesterday = day(TODAY.minusDays(1), 2000, 1700, "157.25");
        ReportDay today = day(TODAY, 2000, 1740, "159.60");
        Map<UUID, List<ReportDay>> days = Map.of(GALPAO_1.id(), List.of(yesterday, today));

        // when
        FarmSectorRow row = farm(DashboardPeriod.YESTERDAY, days).sectors().getFirst();

        // then
        assertThat(row.todayReport().id()).isEqualTo(today.reportId());
    }
}
