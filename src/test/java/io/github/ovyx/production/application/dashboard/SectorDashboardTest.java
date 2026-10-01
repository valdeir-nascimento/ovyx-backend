package io.github.ovyx.production.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.application.dailyreport.DailyReportTotals;
import io.github.ovyx.production.application.dailyreport.ReportingSector;
import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * O calculo do painel de um setor (US1 da 006; FR-003, FR-005 a FR-009, R-005, R-006): os indicadores do
 * periodo contra o anterior, com as contas e os arredondamentos do relatorio, e a serie dos ultimos 7 dias.
 */
@DisplayName("SectorDashboard")
class SectorDashboardTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);
    private static final ReportingSector SECTOR =
            new ReportingSector(UUID.fromString("3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11"), "Codornas — Galpão 1", "ACTIVE");

    /**
     * Um relatorio somado, completo: todas as gaiolas com producao e racao.
     *
     * @param feedCost o custo da racao do dia, em reais (vira o custo exato, vezes 1.000)
     */
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

    /** O mesmo relatorio, com gaiolas sem producao e sem racao. */
    private static ReportDay partial(ReportDay day, int withProduction, int withFeed) {
        return new ReportDay(
                day.reportId(),
                day.date(),
                day.openingBirdCount(),
                day.cages(),
                withProduction,
                withFeed,
                day.eggs(),
                day.small(),
                day.jumbo(),
                day.dirty(),
                day.cracked(),
                day.bloodSpot(),
                day.abnormal(),
                day.exactFeedCost(),
                day.removedBirds(),
                day.noMortalityConfirmed());
    }

    /** A meta dos setores cadastrados antes da feature 008. */
    private static final LayingRateTarget EIGHTY_FIVE = new LayingRateTarget(new BigDecimal("85"));

    private static SectorDashboard dashboard(DashboardPeriod period, ReportDay... days) {
        return SectorDashboard.of(SECTOR, period, TODAY, List.of(days), EIGHTY_FIVE);
    }

    @Test
    @DisplayName("gives the indicators of today against yesterday, as the acceptance scenario of US1")
    void givenReportsOfTodayAndYesterday_whenBuildingToday_thenGiveTheFourIndicatorsWithTheChange() {
        // given
        ReportDay today = day(TODAY, 2000, 1740, "159.60");
        ReportDay yesterday = day(TODAY.minusDays(1), 2000, 1700, "157.25");

        // when
        Indicators indicators = dashboard(DashboardPeriod.TODAY, today, yesterday).indicators();

        // then
        assertThat(indicators.production().value()).isEqualByComparingTo("1740");
        assertThat(indicators.production().previous()).isEqualByComparingTo("1700");
        assertThat(indicators.production().change()).isEqualByComparingTo("2.4");
        assertThat(indicators.layingRate().value()).isEqualByComparingTo("87.00");
        assertThat(indicators.layingRate().previous()).isEqualByComparingTo("85.00");
        assertThat(indicators.layingRate().change()).isEqualByComparingTo("2.00");
        assertThat(indicators.feedCost().value()).isEqualByComparingTo("159.60");
        assertThat(indicators.feedCost().change()).isEqualByComparingTo("1.5");
        assertThat(indicators.costPerEgg().value()).isEqualByComparingTo("0.092");
        assertThat(indicators.costPerEgg().previous()).isEqualByComparingTo("0.093");
        assertThat(indicators.costPerEgg().change()).isEqualByComparingTo("-1.1");
    }

    @Test
    @DisplayName("writes the values with the scales of the report: 2 places in the rate and the cost, 3 per egg")
    void givenOneDay_whenBuildingToday_thenUseTheScalesOfTheReport() {
        // given
        ReportDay today = day(TODAY, 2000, 1740, "159.60");

        // when
        Indicators indicators = dashboard(DashboardPeriod.TODAY, today).indicators();

        // then
        assertThat(indicators.layingRate().value()).isEqualTo(DailyReportTotals.percent(1740, 2000));
        assertThat(indicators.layingRate().value().scale()).isEqualTo(2);
        assertThat(indicators.feedCost().value().scale()).isEqualTo(2);
        assertThat(indicators.costPerEgg().value().scale()).isEqualTo(3);
    }

    @Test
    @DisplayName("says which way is good: more eggs and a higher rate, and lower costs")
    void givenIndicators_whenReadingTheGoodDirection_thenUpForProductionAndDownForCosts() {
        // given
        ReportDay today = day(TODAY, 2000, 1740, "159.60");

        // when
        Indicators indicators = dashboard(DashboardPeriod.TODAY, today).indicators();

        // then
        assertThat(indicators.production().goodDirection()).isEqualTo(GoodDirection.UP);
        assertThat(indicators.layingRate().goodDirection()).isEqualTo(GoodDirection.UP);
        assertThat(indicators.feedCost().goodDirection()).isEqualTo(GoodDirection.DOWN);
        assertThat(indicators.costPerEgg().goodDirection()).isEqualTo(GoodDirection.DOWN);
    }

    @ParameterizedTest(name = "{0}: from {1} to {2}")
    @CsvSource({"TODAY, 2026-09-24, 2026-09-24", "YESTERDAY, 2026-09-23, 2026-09-23", "LAST_7_DAYS, 2026-09-18, 2026-09-24"})
    @DisplayName("tells the days of the period")
    void givenPeriod_whenBuilding_thenTellItsFirstAndLastDay(DashboardPeriod period, LocalDate from, LocalDate to) {
        // given
        ReportDay today = day(TODAY, 2000, 1740, "159.60");

        // when
        SectorDashboard dashboard = dashboard(period, today);

        // then
        assertThat(dashboard.period()).isEqualTo(period);
        assertThat(dashboard.from()).isEqualTo(from);
        assertThat(dashboard.to()).isEqualTo(to);
    }

    @Test
    @DisplayName("compares yesterday with the day before")
    void givenThreeDays_whenBuildingYesterday_thenCompareWithTheDayBefore() {
        // given
        ReportDay today = day(TODAY, 2000, 1740, "159.60");
        ReportDay yesterday = day(TODAY.minusDays(1), 2000, 1700, "157.25");
        ReportDay dayBefore = day(TODAY.minusDays(2), 2000, 1720, "158.75");

        // when
        Indicator production = dashboard(DashboardPeriod.YESTERDAY, today, yesterday, dayBefore)
                .indicators()
                .production();

        // then
        assertThat(production.value()).isEqualByComparingTo("1700");
        assertThat(production.previous()).isEqualByComparingTo("1720");
    }

    @Test
    @DisplayName("sums the last 7 days against the 7 before, with the days at the edges in the right window")
    void givenDaysAroundTheEdges_whenBuildingSevenDays_thenPutEachInItsWindow() {
        // given
        ReportDay first = day(LocalDate.of(2026, 9, 18), 1000, 900, "30.00");
        ReportDay last = day(TODAY, 1000, 800, "30.00");
        ReportDay previousLast = day(LocalDate.of(2026, 9, 17), 1000, 700, "30.00");
        ReportDay previousFirst = day(LocalDate.of(2026, 9, 11), 1000, 600, "30.00");
        ReportDay outside = day(LocalDate.of(2026, 9, 10), 1000, 500, "30.00");

        // when
        Indicator production = dashboard(DashboardPeriod.LAST_7_DAYS, first, last, previousLast, previousFirst, outside)
                .indicators()
                .production();

        // then
        assertThat(production.value()).isEqualByComparingTo("1700");
        assertThat(production.previous()).isEqualByComparingTo("1300");
    }

    @Test
    @DisplayName("weighs the rate of several days by the birds: the sum of eggs over the sum of birds")
    void givenTwoDaysOfDifferentSizes_whenBuildingSevenDays_thenDivideTheSums() {
        // given
        ReportDay small = day(TODAY.minusDays(1), 1000, 100, "10.00");
        ReportDay large = day(TODAY, 2000, 1800, "90.00");

        // when
        Indicators indicators = dashboard(DashboardPeriod.LAST_7_DAYS, small, large).indicators();

        // then
        assertThat(indicators.layingRate().value()).isEqualByComparingTo("63.33");
        assertThat(indicators.costPerEgg().value()).isEqualByComparingTo("0.053");
    }

    @Test
    @DisplayName("leaves a day with the feed pending out of the costs, and tells it")
    void givenDayWithFeedPending_whenBuildingSevenDays_thenCountOnlyTheCompleteDaysInTheCosts() {
        // given
        ReportDay complete = day(TODAY.minusDays(1), 1000, 900, "30.00");
        ReportDay pending = partial(day(TODAY, 1000, 950, "12.00"), 4, 2);

        // when
        Indicators indicators = dashboard(DashboardPeriod.LAST_7_DAYS, complete, pending).indicators();

        // then
        assertThat(indicators.production().value()).isEqualByComparingTo("1850");
        assertThat(indicators.feedCost().value()).isEqualByComparingTo("30.00");
        assertThat(indicators.costPerEgg().value()).isEqualByComparingTo("0.033");
        assertThat(indicators.feedCost().incompleteDays()).isEqualTo(1);
        assertThat(indicators.costPerEgg().incompleteDays()).isEqualTo(1);
        assertThat(indicators.production().incompleteDays()).isZero();
    }

    @Test
    @DisplayName("counts a day with the production pending as it is, and tells it")
    void givenDayWithProductionPending_whenBuildingToday_thenUseWhatWasRecordedAndTellIt() {
        // given
        ReportDay pending = partial(day(TODAY, 2000, 870, "159.60"), 2, 4);

        // when
        Indicators indicators = dashboard(DashboardPeriod.TODAY, pending).indicators();

        // then
        assertThat(indicators.production().value()).isEqualByComparingTo("870");
        assertThat(indicators.layingRate().value()).isEqualByComparingTo("43.50");
        assertThat(indicators.production().incompleteDays()).isEqualTo(1);
        assertThat(indicators.layingRate().incompleteDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("gives no value, and not zero, to a period without a report, and no change without the previous one")
    void givenNoReportToday_whenBuildingToday_thenLeaveTheValuesAndTheChangeAbsent() {
        // given
        ReportDay yesterday = day(TODAY.minusDays(1), 1000, 912, "30.00");

        // when
        Indicators indicators = dashboard(DashboardPeriod.TODAY, yesterday).indicators();

        // then
        assertThat(indicators.production().value()).isNull();
        assertThat(indicators.production().previous()).isEqualByComparingTo("912");
        assertThat(indicators.production().change()).isNull();
        assertThat(indicators.costPerEgg().value()).isNull();
    }

    @Test
    @DisplayName("gives no change over a previous value of zero")
    void givenPreviousDayWithoutEggs_whenBuildingToday_thenLeaveTheChangeAbsent() {
        // given
        ReportDay today = day(TODAY, 1000, 900, "30.00");
        ReportDay yesterday = day(TODAY.minusDays(1), 1000, 0, "30.00");

        // when
        Indicator production = dashboard(DashboardPeriod.TODAY, today, yesterday)
                .indicators()
                .production();

        // then
        assertThat(production.previous()).isEqualByComparingTo("0");
        assertThat(production.change()).isNull();
    }

    @Test
    @DisplayName("gives a rate of zero and no cost per egg to a day without eggs")
    void givenDayWithoutEggs_whenBuildingToday_thenGiveZeroRateAndNoCostPerEgg() {
        // given
        ReportDay today = day(TODAY, 1000, 0, "30.00");

        // when
        Indicators indicators = dashboard(DashboardPeriod.TODAY, today).indicators();

        // then
        assertThat(indicators.layingRate().value()).isEqualByComparingTo("0.00");
        assertThat(indicators.feedCost().value()).isEqualByComparingTo("30.00");
        assertThat(indicators.costPerEgg().value()).isNull();
    }

    @Test
    @DisplayName("gives the 7 days up to today, whatever the period, with no value where there is none")
    void givenSomeDaysOfTheWeek_whenBuildingTheTrend_thenGiveSevenDaysWithGaps() {
        // given
        ReportDay today = day(TODAY, 2000, 1740, "159.60");
        ReportDay pending = partial(day(TODAY.minusDays(2), 2000, 1720, "80.00"), 4, 2);
        ReportDay old = day(TODAY.minusDays(7), 2000, 1600, "150.00");

        // when
        List<DashboardDay> trend = dashboard(DashboardPeriod.YESTERDAY, today, pending, old).trend();

        // then
        assertThat(trend).extracting(DashboardDay::date).containsExactly(
                LocalDate.of(2026, 9, 18),
                LocalDate.of(2026, 9, 19),
                LocalDate.of(2026, 9, 20),
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 22),
                LocalDate.of(2026, 9, 23),
                TODAY);
        assertThat(trend.get(0).production()).isNull();
        assertThat(trend.get(0).layingRate()).isNull();
        assertThat(trend.get(4).production()).isEqualTo(1720);
        assertThat(trend.get(4).layingRate()).isEqualByComparingTo("86.00");
        assertThat(trend.get(4).feedCost()).isNull();
        assertThat(trend.get(4).costPerEgg()).isNull();
        assertThat(trend.get(6).feedCost()).isEqualByComparingTo("159.60");
        assertThat(trend.get(6).costPerEgg()).isEqualByComparingTo("0.092");
    }

    @Test
    @DisplayName("tells the report of today and how each entry stands")
    void givenTodayReportWithTheFeedPending_whenBuilding_thenTellItsStatuses() {
        // given
        ReportDay today = partial(day(TODAY, 2000, 1740, "80.00"), 4, 2);

        // when
        TodayReport report = dashboard(DashboardPeriod.LAST_7_DAYS, today).todayReport();

        // then
        assertThat(report.id()).isEqualTo(today.reportId());
        assertThat(report.productionStatus()).isEqualTo(ProductionStatus.COMPLETE);
        assertThat(report.feedStatus()).isEqualTo(FeedStatus.PENDING);
        assertThat(report.mortalityStatus()).isEqualTo(MortalityStatus.RECORDED);
    }

    @Test
    @DisplayName("has no report of today when it was not opened")
    void givenNoReportToday_whenBuilding_thenHaveNoTodayReport() {
        // given
        ReportDay yesterday = day(TODAY.minusDays(1), 2000, 1700, "157.25");

        // when
        SectorDashboard dashboard = dashboard(DashboardPeriod.TODAY, yesterday);

        // then
        assertThat(dashboard.todayReport()).isNull();
        assertThat(dashboard.sector()).isEqualTo(SECTOR);
    }

    // ---------------------------------------------------------------- meta e classificação (US2)

    /** Um relatório com a classificação dada: pequenos, jumbo, sujos, trincados, com sangue e anormais. */
    private static ReportDay graded(LocalDate date, int eggs, int... grades) {
        return new ReportDay(
                UUID.randomUUID(),
                date,
                2000,
                4,
                4,
                4,
                eggs,
                grades[0],
                grades[1],
                grades[2],
                grades[3],
                grades[4],
                grades[5],
                new BigDecimal("159600.00"),
                0,
                true);
    }

    @ParameterizedTest(name = "{0} eggs of 2000 birds is {1}")
    @CsvSource({"1700, ABOVE", "1699, BELOW"})
    @DisplayName("tells whether the last day with a report reached the target of 85%, the target included")
    void givenLastDay_whenBuilding_thenCompareItWithTheTarget(int eggs, TargetStatus expected) {
        // given
        ReportDay today = day(TODAY, 2000, eggs, "159.60");

        // when
        SectorDashboard dashboard = dashboard(DashboardPeriod.TODAY, today);

        // then
        assertThat(dashboard.target()).isEqualByComparingTo("85.00");
        assertThat(dashboard.targetStatus()).isEqualTo(expected);
    }

    @Test
    @DisplayName("takes the day before for the target when today has no report yet")
    void givenNoReportToday_whenBuilding_thenCompareTheLastDayWithAReport() {
        // given
        ReportDay yesterday = day(TODAY.minusDays(1), 2000, 1600, "157.25");

        // when
        SectorDashboard dashboard = dashboard(DashboardPeriod.TODAY, yesterday);

        // then
        assertThat(dashboard.targetStatus()).isEqualTo(TargetStatus.BELOW);
    }

    @Test
    @DisplayName("has no target status without a report in the 7 days")
    void givenNoReportInTheWeek_whenBuilding_thenHaveNoTargetStatus() {
        // given
        ReportDay old = day(TODAY.minusDays(9), 2000, 1700, "157.25");

        // when
        SectorDashboard dashboard = dashboard(DashboardPeriod.TODAY, old);

        // then
        assertThat(dashboard.targetStatus()).isNull();
    }

    @Test
    @DisplayName("grades the eggs of the period as the acceptance scenario of US2, with one place, half up")
    void givenGradedEggs_whenBuildingToday_thenGiveTheStandardAndEachGrade() {
        // given
        ReportDay today = graded(TODAY, 1740, 30, 12, 20, 18, 6, 4);

        // when
        EggGrading grades = dashboard(DashboardPeriod.TODAY, today).grades();

        // then
        assertThat(grades.collected()).isEqualTo(1740);
        assertThat(grades.standard().grade()).isEqualTo("standard");
        assertThat(grades.standard().count()).isEqualTo(1650);
        assertThat(grades.standard().percent()).isEqualByComparingTo("94.8");
        assertThat(grades.shares()).extracting(EggGradeShare::grade)
                .containsExactly("small", "jumbo", "dirty", "cracked", "bloodSpot", "abnormal");
        assertThat(grades.shares()).extracting(EggGradeShare::count).containsExactly(30, 12, 20, 18, 6, 4);
        assertThat(grades.shares()).extracting(share -> share.percent().toPlainString())
                .containsExactly("1.7", "0.7", "1.1", "1.0", "0.3", "0.2");
    }

    @Test
    @DisplayName("sums the grades of the 7 days")
    void givenGradesOnTwoDays_whenBuildingSevenDays_thenSumThem() {
        // given
        ReportDay today = graded(TODAY, 100, 10, 0, 0, 0, 0, 0);
        ReportDay yesterday = graded(TODAY.minusDays(1), 100, 0, 5, 0, 0, 0, 0);

        // when
        EggGrading grades = dashboard(DashboardPeriod.LAST_7_DAYS, today, yesterday).grades();

        // then
        assertThat(grades.collected()).isEqualTo(200);
        assertThat(grades.standard().count()).isEqualTo(185);
        assertThat(grades.shares().get(0).count()).isEqualTo(10);
        assertThat(grades.shares().get(1).count()).isEqualTo(5);
    }

    @Test
    @DisplayName("has no grading without eggs in the period")
    void givenNoEggs_whenBuildingToday_thenHaveNoGrading() {
        // given
        ReportDay today = day(TODAY, 2000, 0, "159.60");

        // when
        SectorDashboard dashboard = dashboard(DashboardPeriod.TODAY, today);

        // then
        assertThat(dashboard.grades()).isNull();
    }

    // ---------------------------------------------------------------- últimos relatórios (US4)

    @Test
    @DisplayName("keeps the latest reports in the order they come, whatever the period")
    void givenLatestReports_whenAddingThem_thenKeepTheirOrder() {
        // given
        LatestReport newest = latest(TODAY);
        LatestReport older = latest(TODAY.minusDays(1));

        // when
        SectorDashboard dashboard =
                dashboard(DashboardPeriod.LAST_7_DAYS, day(TODAY, 2000, 1740, "159.60")).withLatestReports(
                        List.of(newest, older));

        // then
        assertThat(dashboard.latestReports()).containsExactly(newest, older);
    }

    private static LatestReport latest(LocalDate date) {
        return new LatestReport(
                UUID.randomUUID(),
                date,
                java.time.LocalTime.of(6, 30),
                new io.github.ovyx.production.domain.model.Actor(UUID.randomUUID(), "Marina Alves"),
                1740,
                3,
                ProductionStatus.COMPLETE,
                FeedStatus.COMPLETE,
                MortalityStatus.RECORDED);
    }

    // ---------------------------------------------------------------- mutação (T045)

    @Test
    @DisplayName("rounds the cost half up only at the end, as the report does: 30,125 is 30,13")
    void givenCostEndingInHalfACent_whenBuildingToday_thenRoundItHalfUp() {
        // given
        ReportDay today = day(TODAY, 1000, 900, "30.125");

        // when
        Indicator feedCost = dashboard(DashboardPeriod.TODAY, today).indicators().feedCost();

        // then
        assertThat(feedCost.value()).isEqualByComparingTo("30.13");
    }

    @Test
    @DisplayName("divides the exact cost, and not the rounded one, to find the cost per egg")
    void givenCostEndingInHalfACent_whenBuildingToday_thenDivideTheExactCost() {
        // given
        ReportDay today = day(TODAY, 1000, 1, "30.125");

        // when
        Indicator costPerEgg = dashboard(DashboardPeriod.TODAY, today).indicators().costPerEgg();

        // then
        assertThat(costPerEgg.value()).isEqualByComparingTo("30.125");
    }

    @Test
    @DisplayName("keeps two places in the change of the rate: 87,00 against 84,95 is 2,05 points")
    void givenRatesWithTheSecondPlace_whenBuildingToday_thenKeepItInTheChange() {
        // given
        ReportDay today = day(TODAY, 2000, 1740, "159.60");
        ReportDay yesterday = day(TODAY.minusDays(1), 2000, 1699, "157.25");

        // when
        Indicator layingRate = dashboard(DashboardPeriod.TODAY, today, yesterday).indicators().layingRate();

        // then
        assertThat(layingRate.change()).isEqualByComparingTo("2.05");
    }

    @Test
    @DisplayName("compares the last day with a report with the target, and not the first of the week")
    void givenWeekStartingBelowAndEndingAtTheTarget_whenBuilding_thenSayAbove() {
        // given
        ReportDay first = day(TODAY.minusDays(6), 2000, 1600, "150.00");
        ReportDay last = day(TODAY, 2000, 1700, "159.60");

        // when
        SectorDashboard dashboard = dashboard(DashboardPeriod.TODAY, first, last);

        // then
        assertThat(dashboard.targetStatus()).isEqualTo(TargetStatus.ABOVE);
    }

    // ---------------------------------------------------------------- meta do setor (008)

    @ParameterizedTest(name = "a target of {0}% is {1}")
    @CsvSource({"72, ABOVE", "85, BELOW", "79, ABOVE"})
    @DisplayName("compares the last day with the target of the sector, the target included")
    void givenLastDayAt79Percent_whenBuildingWithTheTargetOfTheSector_thenCompareWithIt(
            String target, TargetStatus expected) {
        // given
        ReportDay today = day(TODAY, 2000, 1580, "159.60");

        // when
        SectorDashboard dashboard = SectorDashboard.of(
                SECTOR, DashboardPeriod.TODAY, TODAY, List.of(today), new LayingRateTarget(new BigDecimal(target)));

        // then
        assertThat(dashboard.target()).isEqualByComparingTo(target).hasScaleOf(2);
        assertThat(dashboard.targetStatus()).isEqualTo(expected);
    }
}
