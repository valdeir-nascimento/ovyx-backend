package io.github.ovyx.farm.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Violation;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * O peso médio da amostra (FR-003 e R-007 da 005): gramas, de 1 a 10.000, com até uma casa decimal, lido
 * do texto como foi digitado, com vírgula ou ponto.
 */
@DisplayName("AverageWeight")
class AverageWeightTest {

    @ParameterizedTest(name = "\"{0}\" is {1} g")
    @CsvSource({"'158,4', 158.4", "158.4, 158.4", "161, 161.0", "1, 1.0", "10000, 10000.0", "' 158,4 ', 158.4"})
    @DisplayName("reads the weight as typed, with a comma or a dot, at one decimal place")
    void givenWeightAsTyped_whenCreating_thenKeepItAtOneDecimalPlace(String raw, String grams) {
        // given
        String typed = raw;

        // when
        AverageWeight weight = AverageWeight.of(typed);

        // then
        assertThat(weight.value()).isEqualByComparingTo(grams).hasScaleOf(1);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("asks for the weight when it is missing")
    void givenMissingWeight_whenValidating_thenAskForIt(String raw) {
        // given
        Notification notification = new Notification();

        // when
        AverageWeight.validate(raw, notification);

        // then
        assertThat(notification.violations())
                .extracting(Violation::field, Violation::code, Violation::message)
                .containsExactly(
                        tuple("averageWeight", FarmErrorCode.WEIGHT_REQUIRED, "Informe o peso médio em gramas."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"158,25", "abc", "1.000", "158,", ",5", "15 8"})
    @DisplayName("refuses what is not a weight in grams with up to one decimal place")
    void givenWeightOutOfTheFormat_whenValidating_thenRefuseTheFormat(String raw) {
        // given
        Notification notification = new Notification();

        // when
        AverageWeight.validate(raw, notification);

        // then
        assertThat(notification.violations())
                .extracting(Violation::field, Violation::code, Violation::message)
                .containsExactly(tuple(
                        "averageWeight",
                        FarmErrorCode.WEIGHT_INVALID,
                        "Informe o peso em gramas, com até uma casa decimal."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"0", "0,9", "10000,1", "10001", "-5"})
    @DisplayName("refuses a weight out of 1 to 10,000 g")
    void givenWeightOutOfRange_whenValidating_thenRefuseTheRange(String raw) {
        // given
        Notification notification = new Notification();

        // when
        AverageWeight.validate(raw, notification);

        // then
        assertThat(notification.violations())
                .extracting(Violation::field, Violation::code, Violation::message)
                .containsExactly(tuple(
                        "averageWeight",
                        FarmErrorCode.WEIGHT_OUT_OF_RANGE,
                        "O peso médio deve ficar entre 1 e 10.000 gramas."));
    }

    @Test
    @DisplayName("refuses right away when created from an invalid weight")
    void givenInvalidWeight_whenCreating_thenRefuseWithTheFieldViolation() {
        // given
        String raw = "0";

        // when
        List<Violation> violations = violationsOf(() -> AverageWeight.of(raw));

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(FarmErrorCode.WEIGHT_OUT_OF_RANGE);
    }

    @Test
    @DisplayName("writes itself with the decimal place, as the API returns it")
    void givenWeight_whenWritingIt_thenUseTheDecimalPlace() {
        // given
        AverageWeight weight = AverageWeight.of("161");

        // when
        String written = weight.toString();

        // then
        assertThat(written).isEqualTo("161.0");
    }
}
