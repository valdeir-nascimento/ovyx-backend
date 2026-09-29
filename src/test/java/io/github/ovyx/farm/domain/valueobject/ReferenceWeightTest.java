package io.github.ovyx.farm.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Violation;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A faixa de peso de referência do setor (FR-001 e R-006 da 005): os dois limites vazios, ou os dois
 * preenchidos; inteiros de 1 a 10.000; o mínimo menor que o máximo.
 */
@DisplayName("ReferenceWeight")
class ReferenceWeightTest {

    private static Notification validated(String minimum, String maximum) {
        Notification notification = new Notification();
        ReferenceWeight.validate(minimum, maximum, notification);
        return notification;
    }

    @Test
    @DisplayName("reads both limits as typed")
    void givenBothLimits_whenReading_thenKeepThem() {
        // given
        String minimum = "155";

        // when
        Optional<ReferenceWeight> range = ReferenceWeight.optionalOf(minimum, "175");

        // then
        assertThat(range).contains(new ReferenceWeight(155, 175));
    }

    @ParameterizedTest(name = "\"{0}\" and \"{1}\"")
    @CsvSource(value = {"NULL, NULL", "'', ''", "'  ', ' '"}, nullValues = "NULL")
    @DisplayName("leaves the sector without range when both limits are empty")
    void givenNoLimits_whenReading_thenHaveNoRange(String minimum, String maximum) {
        // given
        Notification notification = validated(minimum, maximum);

        // when
        Optional<ReferenceWeight> range = ReferenceWeight.optionalOf(minimum, maximum);

        // then
        assertThat(notification.hasErrors()).isFalse();
        assertThat(range).isEmpty();
    }

    @Test
    @DisplayName("asks for the maximum when only the minimum is given")
    void givenOnlyTheMinimum_whenValidating_thenAskForTheMaximum() {
        // given
        String minimum = "155";

        // when
        Notification notification = validated(minimum, null);

        // then
        assertThat(notification.violations())
                .extracting(Violation::field, Violation::code, Violation::message)
                .containsExactly(tuple(
                        "maximumWeight", FarmErrorCode.REFERENCE_WEIGHT_INCOMPLETE, "Informe também o peso máximo."));
    }

    @Test
    @DisplayName("asks for the minimum when only the maximum is given")
    void givenOnlyTheMaximum_whenValidating_thenAskForTheMinimum() {
        // given
        String maximum = "175";

        // when
        Notification notification = validated("", maximum);

        // then
        assertThat(notification.violations())
                .extracting(Violation::field, Violation::code, Violation::message)
                .containsExactly(tuple(
                        "minimumWeight", FarmErrorCode.REFERENCE_WEIGHT_INCOMPLETE, "Informe também o peso mínimo."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"155,5", "0", "10001", "abc", "-5"})
    @DisplayName("refuses a limit that is not a whole number of 1 to 10,000 g, in its field")
    void givenInvalidLimit_whenValidating_thenRefuseItInItsField(String invalid) {
        // given
        String maximum = "175";

        // when
        Notification notification = validated(invalid, maximum);

        // then
        assertThat(notification.violations())
                .extracting(Violation::field, Violation::code, Violation::message)
                .containsExactly(tuple(
                        "minimumWeight",
                        FarmErrorCode.REFERENCE_WEIGHT_INVALID,
                        "O peso deve ser um número inteiro de 1 a 10.000 gramas."));
    }

    @Test
    @DisplayName("refuses both invalid limits at once, each in its field")
    void givenBothLimitsInvalid_whenValidating_thenRefuseBoth() {
        // given
        String minimum = "0";

        // when
        Notification notification = validated(minimum, "10001");

        // then
        assertThat(notification.violations())
                .extracting(Violation::field)
                .containsExactly("minimumWeight", "maximumWeight");
    }

    @ParameterizedTest(name = "minimum {0}, maximum {1}")
    @CsvSource({"180, 170", "160, 160"})
    @DisplayName("refuses a minimum that is not below the maximum, in the minimum field")
    void givenMinimumNotBelowTheMaximum_whenValidating_thenRefuseTheInversion(String minimum, String maximum) {
        // given
        String[] limits = {minimum, maximum};

        // when
        Notification notification = validated(limits[0], limits[1]);

        // then
        assertThat(notification.violations())
                .extracting(Violation::field, Violation::code, Violation::message)
                .containsExactly(tuple(
                        "minimumWeight",
                        FarmErrorCode.REFERENCE_WEIGHT_INVERTED,
                        "O peso mínimo deve ser menor que o máximo."));
    }

    @ParameterizedTest(name = "minimum {0}, maximum {1}")
    @CsvSource({"1, 2", "9999, 10000", "1, 10000"})
    @DisplayName("accepts the limits at the edges of the range")
    void givenLimitsAtTheEdges_whenValidating_thenAcceptThem(String minimum, String maximum) {
        // given
        String[] limits = {minimum, maximum};

        // when
        Notification notification = validated(limits[0], limits[1]);

        // then
        assertThat(notification.hasErrors()).isFalse();
    }

    @ParameterizedTest(name = "{0} g in 155–175 g")
    @CsvSource({"155, true", "175, true", "161.4, true", "154.9, false", "175.1, false"})
    @DisplayName("contains a weight between the limits, the limits included")
    void givenWeight_whenAskingIfTheRangeContainsIt_thenIncludeTheLimits(String weight, boolean inside) {
        // given
        ReferenceWeight range = new ReferenceWeight(155, 175);

        // when
        boolean contains = range.contains(new BigDecimal(weight));

        // then
        assertThat(contains).isEqualTo(inside);
    }
}
