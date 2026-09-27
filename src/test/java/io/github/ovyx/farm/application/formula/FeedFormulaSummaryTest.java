package io.github.ovyx.farm.application.formula;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.farm.domain.model.Status;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** O custo por ave ao dia da fórmula: preço × esperado ÷ 1.000, com três casas, a metade para cima (R-007 da 004). */
@DisplayName("FeedFormulaSummary")
class FeedFormulaSummaryTest {

    @ParameterizedTest(name = "R$ {0} × {1} g is R$ {2}")
    @CsvSource({"2.85, 28, 0.080", "3.10, 24, 0.074", "2.50, 25, 0.063", "2.50, 27, 0.068"})
    @DisplayName("derives the cost per bird a day with three decimals, rounding the half up")
    void givenPriceAndIntake_whenSummarizing_thenDeriveTheCostPerBirdDay(String price, int intake, String cost) {
        // given
        BigDecimal pricePerKg = new BigDecimal(price);

        // when
        FeedFormulaSummary summary = FeedFormulaSummary.of(
                FeedFormulaId.generate(),
                "Postura Plus",
                null,
                pricePerKg,
                intake,
                Status.ACTIVE,
                Instant.parse("2026-09-20T10:15:00Z"),
                Instant.parse("2026-09-20T10:15:00Z"));

        // then
        assertThat(summary.costPerBirdDay()).isEqualByComparingTo(cost).hasScaleOf(3);
    }
}
