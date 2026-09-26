package io.github.ovyx.production.application.dailyreport;

import static org.assertj.core.api.Assertions.assertThat;

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
        return new ReportCageDetail(UUID.randomUUID(), "A-01", "A", 1, 48, production, mortality);
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
}
