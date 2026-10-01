package io.github.ovyx.production.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * As contas do painel, as mesmas para o setor e para a granja (R-003 da 009): os indicadores e a serie dos 7 dias
 * somando todos os relatorios de cada data, com soma sobre soma.
 */
@DisplayName("DashboardFigures")
class DashboardFiguresTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);

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

    /** O mesmo relatorio, com a producao lancada em so duas das quatro gaiolas. */
    private static ReportDay productionPending(ReportDay day) {
        return new ReportDay(
                day.reportId(),
                day.date(),
                day.openingBirdCount(),
                4,
                2,
                4,
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

    @Test
    @DisplayName("sums every report of a date in the trend, with the sum of eggs over the sum of birds")
    void givenTwoReportsOnTheSameDate_whenBuildingTheTrend_thenSumBoth() {
        // given
        List<ReportDay> days =
                List.of(day(TODAY, 2000, 1740, "159.60"), day(TODAY, 1200, 1160, "120.40"));

        // when
        List<DashboardDay> trend = DashboardFigures.trend(days, TODAY);

        // then
        DashboardDay today = trend.getLast();
        assertThat(trend).hasSize(7);
        assertThat(today.date()).isEqualTo(TODAY);
        assertThat(today.production()).isEqualTo(2900);
        assertThat(today.layingRate()).isEqualByComparingTo("90.63");
        assertThat(today.feedCost()).isEqualByComparingTo("280.00");
        assertThat(today.costPerEgg()).isEqualByComparingTo("0.097");
    }

    @Test
    @DisplayName("counts only the reports with the feed complete in the cost of a date")
    void givenOneReportWithTheFeedPendingOnADate_whenBuildingTheTrend_thenCostOnlyTheOther() {
        // given
        List<ReportDay> days =
                List.of(day(TODAY, 2000, 1740, "159.60"), feedPending(day(TODAY, 1200, 1160, "120.40")));

        // when
        DashboardDay today = DashboardFigures.trend(days, TODAY).getLast();

        // then
        assertThat(today.production()).isEqualTo(2900);
        assertThat(today.feedCost()).isEqualByComparingTo("159.60");
        assertThat(today.costPerEgg()).isEqualByComparingTo("0.092");
    }

    @Test
    @DisplayName("leaves a date without report without values, instead of zero")
    void givenNoReportOnADate_whenBuildingTheTrend_thenLeaveItEmpty() {
        // given
        List<ReportDay> days = List.of(day(TODAY, 2000, 1740, "159.60"));

        // when
        DashboardDay yesterday = DashboardFigures.trend(days, TODAY).get(5);

        // then
        assertThat(yesterday.date()).isEqualTo(TODAY.minusDays(1));
        assertThat(yesterday.production()).isNull();
        assertThat(yesterday.layingRate()).isNull();
        assertThat(yesterday.feedCost()).isNull();
    }

    @Test
    @DisplayName("counts the pending reports, and not the days, in the indicators of several sectors")
    void givenTwoSectorsPendingOnTheSameDay_whenBuildingTheIndicators_thenCountTwoReports() {
        // given
        List<ReportDay> current = List.of(
                feedPending(day(TODAY, 2000, 1740, "159.60")),
                feedPending(productionPending(day(TODAY, 1200, 1160, "120.40"))));
        List<ReportDay> previous = List.of(day(TODAY.minusDays(1), 2000, 1700, "157.25"));

        // when
        Indicators indicators = DashboardFigures.indicators(current, previous);

        // then
        assertThat(indicators.production().value()).isEqualByComparingTo("2900");
        assertThat(indicators.production().incompleteDays()).isEqualTo(1);
        assertThat(indicators.feedCost().incompleteDays()).isEqualTo(2);
        assertThat(indicators.feedCost().value()).isNull();
        assertThat(indicators.layingRate().value()).isEqualByComparingTo("90.63");
    }

    @Test
    @DisplayName("keeps the reports of the interval, both ends included")
    void givenReportsAroundAnInterval_whenFilteringIt_thenKeepBothEnds() {
        // given
        List<ReportDay> days = List.of(
                day(TODAY.minusDays(7), 2000, 1700, "157.25"),
                day(TODAY.minusDays(6), 2000, 1700, "157.25"),
                day(TODAY, 2000, 1740, "159.60"));

        // when
        List<ReportDay> within = DashboardFigures.within(days, TODAY.minusDays(6), TODAY);

        // then
        assertThat(within).extracting(ReportDay::date).containsExactly(TODAY.minusDays(6), TODAY);
    }

    @Test
    @DisplayName("counts every report with the production pending, one per sector on the same day")
    void givenTwoReportsWithTheProductionPending_whenBuildingTheIndicators_thenCountBoth() {
        // given
        List<ReportDay> current = List.of(
                productionPending(day(TODAY, 2000, 1740, "159.60")),
                productionPending(day(TODAY, 1200, 1160, "120.40")));

        // when
        Indicators indicators = DashboardFigures.indicators(current, List.of());

        // then
        assertThat(indicators.production().incompleteDays()).isEqualTo(2);
        assertThat(indicators.layingRate().incompleteDays()).isEqualTo(2);
    }
}
