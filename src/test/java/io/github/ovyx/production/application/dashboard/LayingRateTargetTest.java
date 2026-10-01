package io.github.ovyx.production.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** A meta de produtividade do setor, como o painel a le do farm (R-008 da 008). */
@DisplayName("LayingRateTarget (production)")
class LayingRateTargetTest {

    @Test
    @DisplayName("keeps the target with two decimal places, like the laying rate")
    void givenTargetWithOneDecimal_whenCreating_thenKeepTwoDecimals() {
        // given
        BigDecimal stored = new BigDecimal("82.5");

        // when
        LayingRateTarget target = new LayingRateTarget(stored);

        // then
        assertThat(target.value()).isEqualByComparingTo("82.50").hasScaleOf(2);
    }

    @ParameterizedTest(name = "{1}% against {0}%: {2}")
    @CsvSource({"72, 72.00, true", "72, 79.00, true", "72, 71.99, false", "82.5, 82.49, false", "100, 100.00, true"})
    @DisplayName("counts a laying rate equal to the target as meeting it")
    void givenLayingRate_whenComparingWithTheTarget_thenMeetItFromTheTargetUp(
            String target, String rate, boolean expected) {
        // given
        LayingRateTarget laying = new LayingRateTarget(new BigDecimal(target));

        // when
        boolean met = laying.isMetBy(new BigDecimal(rate));

        // then
        assertThat(met).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} is \"{1}\"")
    @CsvSource(delimiter = ';', value = {"85.0; 85", "82.5; 82,5", "100.0; 100", "1.0; 1"})
    @DisplayName("writes the target without trailing zeros, with a comma")
    void givenTarget_whenWritingIt_thenDropTheTrailingZeros(String stored, String expected) {
        // given
        LayingRateTarget target = new LayingRateTarget(new BigDecimal(stored));

        // when
        String label = target.label();

        // then
        assertThat(label).isEqualTo(expected);
    }
}
