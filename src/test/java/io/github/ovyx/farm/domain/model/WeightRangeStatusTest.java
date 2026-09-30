package io.github.ovyx.farm.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.valueobject.ReferenceWeight;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A situação de um peso diante da faixa do setor (R-009 da 007), a regra que a tela Peso médio e a planilha das
 * gaiolas usam: os limites contam como dentro da faixa.
 */
@DisplayName("WeightRangeStatus")
class WeightRangeStatusTest {

    private static final ReferenceWeight RANGE = new ReferenceWeight(155, 175);

    @ParameterizedTest(name = "given {0} g then {1}")
    @CsvSource({"155, WITHIN", "175, WITHIN", "161.4, WITHIN", "154.9, OUTSIDE", "175.1, OUTSIDE", "150.8, OUTSIDE"})
    @DisplayName("puts the weight within the range, the limits included, or outside it")
    void givenWeightAroundTheRange_whenJudging_thenSayWithinOrOutside(String grams, WeightRangeStatus expected) {
        // given
        BigDecimal weight = new BigDecimal(grams);

        // when
        WeightRangeStatus status = WeightRangeStatus.of(RANGE, weight);

        // then
        assertThat(status).isEqualTo(expected);
    }

    @Test
    @DisplayName("says there is no range when the sector has none")
    void givenSectorWithoutRange_whenJudging_thenSayNoRange() {
        // given
        ReferenceWeight noRange = null;

        // when
        WeightRangeStatus status = WeightRangeStatus.of(noRange, new BigDecimal("150.8"));

        // then
        assertThat(status).isEqualTo(WeightRangeStatus.NO_RANGE);
    }

    @Test
    @DisplayName("says nothing about a cage never weighed")
    void givenNoWeight_whenJudging_thenSayNothing() {
        // given
        BigDecimal noWeight = null;

        // when
        WeightRangeStatus status = WeightRangeStatus.of(RANGE, noWeight);

        // then
        assertThat(status).isNull();
    }
}
