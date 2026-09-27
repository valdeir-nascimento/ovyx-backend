package io.github.ovyx.production.application.dailyreport;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Os totais do dia, derivados das gaiolas e das aves do início do dia (spec, Key Entities; R-008), com as
 * porcentagens em duas casas, arredondadas para cima a partir da metade. As situações são do domínio
 * ({@code ProductionStatusTest} e {@code MortalityStatusTest}).
 */
@DisplayName("DailyReportTotals")
class DailyReportTotalsTest {

    private static ReportCageDetail cage(CageProduction production, CageMortality mortality) {
        return new ReportCageDetail(UUID.randomUUID(), "A-01", "A", 1, 48, production, mortality, null);
    }

    private static final UUID POSTURA_PLUS = UUID.fromString("4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11");
    private static final UUID RECRIA = UUID.fromString("8a0c2e4a-6c8e-4a0c-8e2a-4c6e8a0c2e22");

    /** Uma gaiola com as aves e a ração dadas; sem ração quando o consumo é nulo. */
    private static ReportCageDetail fed(int birds, UUID formula, String price, int expected, Integer consumption) {
        CageFeed feed = consumption == null
                ? null
                : CageFeed.of(formula, "Fórmula", new BigDecimal(price), expected, consumption, birds);
        return new ReportCageDetail(UUID.randomUUID(), "A-01", "A", 1, birds, null, null, feed);
    }

    @ParameterizedTest(name = "{0} over {1} is {2}")
    @CsvSource({"89, 98, 90.82", "1, 3, 33.33", "1, 32, 3.13", "3, 2400, 0.13", "0, 98, 0.00"})
    @DisplayName("writes a percentage with two decimals, rounded half up")
    void givenPartAndWhole_whenWritingThePercentage_thenRoundHalfUpToTwoDecimals(
            int part, int whole, BigDecimal expected) {
        // given — valores vindos do @CsvSource

        // when
        BigDecimal percent = DailyReportTotals.percent(part, whole);

        // then
        assertThat(percent).isEqualByComparingTo(expected);
        assertThat(percent.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("sums the production of the recorded cages and counts the pending ones")
    void givenOneRecordedAndOnePendingCage_whenSummingTheProduction_thenSumOnlyTheRecordedOne() {
        // given
        List<ReportCageDetail> cages =
                List.of(cage(new CageProduction(45, 0, 1, 1, 2, 1, 0), null), cage(null, null));

        // when
        ProductionTotals totals = DailyReportTotals.production(98, cages);

        // then
        assertThat(totals.status()).isEqualTo(ProductionStatus.PENDING);
        assertThat(totals.pendingCages()).isEqualTo(1);
        assertThat(totals.collectedEggs()).isEqualTo(45);
        assertThat(totals.standardEggs()).isEqualTo(40);
        assertThat(totals.unsellableEggs()).isEqualTo(3);
        assertThat(totals.layingRate()).isEqualByComparingTo("45.92");
    }

    @Test
    @DisplayName("sums the deaths and the culls of the cages, with the rate and the balance of the day")
    void givenOneDeathAndOneCull_whenSummingTheMortality_thenFindTheRateAndTheBalance() {
        // given
        List<ReportCageDetail> cages =
                List.of(cage(null, new CageMortality(1, 0, null)), cage(null, new CageMortality(0, 1, "Prostração.")));

        // when
        MortalityTotals totals = DailyReportTotals.mortality(98, false, cages);

        // then
        assertThat(totals.status()).isEqualTo(MortalityStatus.RECORDED);
        assertThat(totals.deaths()).isEqualTo(1);
        assertThat(totals.culls()).isEqualTo(1);
        assertThat(totals.removalRate()).isEqualByComparingTo("2.04");
        assertThat(totals.closingBirdCount()).isEqualTo(96);
    }

    // ------------------------------------------------------------------ ração (feature 004, R-007)

    @Test
    @DisplayName("sums the feed of the fed cages, with the cost rounded only at the end")
    void givenTwoFedCages_whenSummingTheFeed_thenSumTheConsumptionAndTheExactCost() {
        // given
        // 1.344 g e 1.400 g a R$ 2,85: R$ 3,8304 + R$ 3,99 = R$ 7,8204.
        List<ReportCageDetail> cages =
                List.of(fed(48, POSTURA_PLUS, "2.85", 28, 1344), fed(50, POSTURA_PLUS, "2.85", 28, 1400));

        // when
        FeedTotals totals = DailyReportTotals.feed(cages, 89);

        // then
        assertThat(totals.status()).isEqualTo(FeedStatus.COMPLETE);
        assertThat(totals.pendingCages()).isZero();
        assertThat(totals.consumption()).isEqualTo(2744);
        assertThat(totals.cost()).isEqualByComparingTo("7.82").hasScaleOf(2);
        assertThat(totals.costPerEgg()).isEqualByComparingTo("0.088").hasScaleOf(3);
        assertThat(totals.intakePerBird()).isEqualByComparingTo("28.0").hasScaleOf(1);
        assertThat(totals.expectedIntakePerBird()).isEqualByComparingTo("28.0").hasScaleOf(1);
    }

    @Test
    @DisplayName("rounds the cost of the day once, and not the cost of each cage")
    void givenThreeCagesOfHalfACent_whenSummingTheFeed_thenRoundTheExactSumOnce() {
        // given
        // 2 g a R$ 2,50 custam R$ 0,005: arredondada por gaiola, a soma de cinco daria R$ 0,05; exata, R$ 0,025,
        // que sobe para R$ 0,03 — e não desce ao par, R$ 0,02.
        ReportCageDetail halfCent = fed(1, POSTURA_PLUS, "2.50", 28, 2);
        List<ReportCageDetail> cages = List.of(halfCent, halfCent, halfCent, halfCent, halfCent);

        // when
        FeedTotals totals = DailyReportTotals.feed(cages, 10);

        // then
        assertThat(totals.cost()).isEqualByComparingTo("0.03");
        assertThat(totals.costPerEgg()).isEqualByComparingTo("0.003");
    }

    @Test
    @DisplayName("weighs the expected intake of each formula by the birds of its cage")
    void givenCagesOfTwoFormulas_whenSummingTheFeed_thenWeighTheExpectedIntakeByTheBirds() {
        // given
        // (48 aves × 24 g + 50 aves × 28 g) ÷ 98 aves = 26,04 g.
        List<ReportCageDetail> cages =
                List.of(fed(48, RECRIA, "3.10", 24, 1100), fed(50, POSTURA_PLUS, "2.85", 28, 1250));

        // when
        FeedTotals totals = DailyReportTotals.feed(cages, 89);

        // then
        assertThat(totals.expectedIntakePerBird()).isEqualByComparingTo("26.0");
        assertThat(totals.intakePerBird()).isEqualByComparingTo("24.0");
    }

    @Test
    @DisplayName("leaves the cost per egg out without eggs, and the per bird intakes out without birds")
    void givenNoEggsAndNoBirds_whenSummingTheFeed_thenLeaveThoseTotalsOut() {
        // given
        List<ReportCageDetail> cages = List.of(fed(0, POSTURA_PLUS, "2.85", 28, 0));

        // when
        FeedTotals totals = DailyReportTotals.feed(cages, 0);

        // then
        assertThat(totals.costPerEgg()).isNull();
        assertThat(totals.intakePerBird()).isNull();
        assertThat(totals.expectedIntakePerBird()).isNull();
        assertThat(totals.cost()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("leaves the cost per egg out while no cage has feed, even with eggs collected (QA 1 of 004)")
    void givenEggsButNoCageFed_whenSummingTheFeed_thenLeaveTheCostPerEggOut() {
        // given
        List<ReportCageDetail> cages =
                List.of(fed(48, POSTURA_PLUS, "2.85", 28, null), fed(50, POSTURA_PLUS, "2.85", 28, null));

        // when
        FeedTotals totals = DailyReportTotals.feed(cages, 89);

        // then
        assertThat(totals.costPerEgg()).isNull();
        assertThat(totals.cost()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("counts the cages without feed as pending, and sums only the fed ones")
    void givenOneFedAndOnePendingCage_whenSummingTheFeed_thenKeepItPending() {
        // given
        List<ReportCageDetail> cages =
                List.of(fed(48, POSTURA_PLUS, "2.85", 28, 1344), fed(50, POSTURA_PLUS, "2.85", 28, null));

        // when
        FeedTotals totals = DailyReportTotals.feed(cages, 89);

        // then
        assertThat(totals.status()).isEqualTo(FeedStatus.PENDING);
        assertThat(totals.pendingCages()).isEqualTo(1);
        assertThat(totals.consumption()).isEqualTo(1344);
        assertThat(totals.intakePerBird()).isEqualByComparingTo("28.0");
    }

    @Test
    @DisplayName("gives the feed of a cage its cost, its intake per bird and its deviation from the expected")
    void givenCorrectedCage_whenDerivingItsFeed_thenFindTheCostTheIntakeAndTheDeviation() {
        // given
        // 1.250 g para 50 aves: 25,0 g por ave, 10,7% abaixo dos 28 g; a R$ 2,85, R$ 3,5625.
        int consumption = 1250;

        // when
        CageFeed feed = CageFeed.of(POSTURA_PLUS, "Postura Plus", new BigDecimal("2.85"), 28, consumption, 50);

        // then
        assertThat(feed.cost()).isEqualByComparingTo("3.56").hasScaleOf(2);
        assertThat(feed.intakePerBird()).isEqualByComparingTo("25.0").hasScaleOf(1);
        assertThat(feed.deviation()).isEqualByComparingTo("-10.7").hasScaleOf(1);
    }

    @ParameterizedTest(name = "{0} g for 50 birds deviates {1}%")
    @CsvSource({"1400, 0.0", "1435, 2.5", "1436, 2.6", "1365, -2.5", "1500, 7.1"})
    @DisplayName("writes the deviation with one decimal, rounded half up")
    void givenConsumption_whenDerivingTheDeviation_thenRoundItToOneDecimal(int consumption, String deviation) {
        // given
        int birds = 50;

        // when
        CageFeed feed = CageFeed.of(POSTURA_PLUS, "Postura Plus", new BigDecimal("2.85"), 28, consumption, birds);

        // then
        assertThat(feed.deviation()).isEqualByComparingTo(deviation);
    }

    @Test
    @DisplayName("rounds the half cent of the cost of a cage up, and not to the even")
    void givenHalfACentOfCost_whenDerivingTheFeedOfACage_thenRoundItUp() {
        // given
        int consumption = 2;

        // when
        CageFeed feed = CageFeed.of(POSTURA_PLUS, "Postura Plus", new BigDecimal("2.50"), 28, consumption, 1);

        // then
        assertThat(feed.cost()).isEqualByComparingTo("0.01");
    }

    @Test
    @DisplayName("leaves the intake per bird and the deviation out of a cage without birds")
    void givenCageWithoutBirds_whenDerivingItsFeed_thenLeaveTheIntakeAndTheDeviationOut() {
        // given
        int birds = 0;

        // when
        CageFeed feed = CageFeed.of(POSTURA_PLUS, "Postura Plus", new BigDecimal("2.85"), 28, 0, birds);

        // then
        assertThat(feed.intakePerBird()).isNull();
        assertThat(feed.deviation()).isNull();
        assertThat(feed.cost()).isEqualByComparingTo("0.00");
    }
}
