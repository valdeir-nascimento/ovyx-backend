package io.github.ovyx.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.OptionalInt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Leitura de um inteiro digitado, que os contextos usam para as quantidades que chegam como texto cru
 * (R-013 da feature 002; T005 da feature 003).
 */
@DisplayName("WholeNumber")
class WholeNumberTest {

    @ParameterizedTest(name = "\"{0}\" is {1}")
    @CsvSource({"07, 7", "7, 7", "+3, 3", "-1, -1", "' 12 ', 12", "0, 0"})
    @DisplayName("reads the value of a typed integer")
    void givenTypedInteger_whenReadingItsValue_thenReturnTheInteger(String raw, int expected) {
        // given — valor bruto vindo do @CsvSource

        // when
        OptionalInt value = WholeNumber.valueOf(raw);

        // then
        assertThat(value).hasValue(expected);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"12.5", "12,5", "abc", "", "  ", "1e2", "7a"})
    @DisplayName("does not take text that is not an integer as one")
    void givenTextThatIsNotAnInteger_whenCheckingIt_thenAnswerNo(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        boolean integer = WholeNumber.isInteger(raw);

        // then
        assertThat(integer).isFalse();
    }

    @Test
    @DisplayName("takes a ten-digit number as an integer too large to have a value")
    void givenTenDigitNumber_whenReadingIt_thenIntegerWithoutValue() {
        // given
        String raw = "1234567890";

        // when
        boolean integer = WholeNumber.isInteger(raw);
        OptionalInt value = WholeNumber.valueOf(raw);

        // then
        assertThat(integer).isTrue();
        assertThat(value).isEmpty();
    }

    @ParameterizedTest(name = "\"{0}\" within 0 and 1000: {1}")
    @CsvSource({"1000, true", "0, true", "1001, false", "-1, false", "1234567890, false"})
    @DisplayName("tells whether a typed integer is within the bounds, inclusive")
    void givenTypedInteger_whenCheckingTheBounds_thenAnswerInclusively(String raw, boolean expected) {
        // given — valor bruto vindo do @CsvSource

        // when
        boolean within = WholeNumber.within(raw, 0, 1000);

        // then
        assertThat(within).isEqualTo(expected);
    }
}
