package io.github.ovyx.production.application.dailyreport;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * As contas de um período com vários dias (R-007 da 007), as mesmas do painel e da linha de totais da planilha:
 * a soma sobre a soma, o custo só dos dias com a ração completa e arredondado só no fim.
 */
@DisplayName("PeriodTotals")
class PeriodTotalsTest {

    /** Um dia: aves do início, ovos, consumo vezes preço antes de dividir por 1.000, e se a ração está completa. */
    private static PeriodDay day(int birds, int eggs, String exactFeedCost, boolean feedComplete) {
        return new PeriodDay(birds, eggs, new BigDecimal(exactFeedCost), feedComplete);
    }

    @Test
    @DisplayName("adds the eggs and divides by the birds of every day, not averaging the rates")
    void givenDaysOfDifferentSizes_whenTotalling_thenDivideTheSumOfTheEggsByTheSumOfTheBirds() {
        // given
        List<PeriodDay> days = List.of(day(100, 87, "7837.5", true), day(99, 40, "3705", false));

        // when
        PeriodTotals totals = PeriodTotals.of(days);

        // then
        assertThat(totals.days()).isEqualTo(2);
        assertThat(totals.eggs()).isEqualTo(127);
        assertThat(totals.production()).isEqualByComparingTo("127");
        assertThat(totals.layingRate()).isEqualByComparingTo("63.82");
        assertThat(totals.layingRate().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("counts the cost only of the days with the feed complete, and says how many were left out")
    void givenDayWithTheFeedPending_whenTotalling_thenLeaveItOutOfTheCost() {
        // given
        List<PeriodDay> days = List.of(day(100, 87, "7837.5", true), day(99, 40, "3705", false));

        // when
        PeriodTotals totals = PeriodTotals.of(days);

        // then
        assertThat(totals.feedCost()).isEqualByComparingTo("7.84");
        assertThat(totals.costPerEgg()).isEqualByComparingTo("0.090");
        assertThat(totals.costPerEgg().scale()).isEqualTo(3);
        assertThat(totals.incompleteDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("rounds the cost half up only at the end: 30,125 is 30,13")
    void givenCostEndingInHalfACent_whenTotalling_thenRoundItHalfUpAtTheEnd() {
        // given
        List<PeriodDay> days = List.of(day(1000, 900, "15062.5", true), day(1000, 900, "15062.5", true));

        // when
        PeriodTotals totals = PeriodTotals.of(days);

        // then
        assertThat(totals.feedCost()).isEqualByComparingTo("30.13");
        assertThat(totals.feedCost().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("divides the exact cost, and not the rounded one, to find the cost per egg")
    void givenOneEggAndACostEndingInHalfACent_whenTotalling_thenDivideTheExactCost() {
        // given
        List<PeriodDay> days = List.of(day(1000, 1, "30125", true));

        // when
        PeriodTotals totals = PeriodTotals.of(days);

        // then
        assertThat(totals.costPerEgg()).isEqualByComparingTo("30.125");
    }

    @Test
    @DisplayName("leaves the cost and the cost per egg out without a day with the feed complete")
    void givenNoDayWithTheFeedComplete_whenTotalling_thenLeaveTheCostsOut() {
        // given
        List<PeriodDay> days = List.of(day(99, 40, "3705", false));

        // when
        PeriodTotals totals = PeriodTotals.of(days);

        // then
        assertThat(totals.feedCost()).isNull();
        assertThat(totals.costPerEgg()).isNull();
        assertThat(totals.incompleteDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("leaves the cost per egg out without eggs on the days with the feed complete")
    void givenNoEggOnTheFedDays_whenTotalling_thenLeaveTheCostPerEggOut() {
        // given
        List<PeriodDay> days = List.of(day(100, 0, "7837.5", true), day(99, 40, "3705", false));

        // when
        PeriodTotals totals = PeriodTotals.of(days);

        // then
        assertThat(totals.feedCost()).isEqualByComparingTo("7.84");
        assertThat(totals.costPerEgg()).isNull();
    }

    @Test
    @DisplayName("leaves everything out without days")
    void givenNoDays_whenTotalling_thenLeaveEverythingOut() {
        // given
        List<PeriodDay> days = List.of();

        // when
        PeriodTotals totals = PeriodTotals.of(days);

        // then
        assertThat(totals.days()).isZero();
        assertThat(totals.production()).isNull();
        assertThat(totals.layingRate()).isNull();
        assertThat(totals.feedCost()).isNull();
        assertThat(totals.costPerEgg()).isNull();
        assertThat(totals.incompleteDays()).isZero();
    }

    @Test
    @DisplayName("leaves the rate out when the days have no birds")
    void givenDaysWithoutBirds_whenTotalling_thenLeaveTheRateOut() {
        // given
        List<PeriodDay> days = List.of(day(0, 0, "0", true));

        // when
        PeriodTotals totals = PeriodTotals.of(days);

        // then
        assertThat(totals.production()).isEqualByComparingTo("0");
        assertThat(totals.layingRate()).isNull();
    }
}
