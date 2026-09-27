package io.github.ovyx.farm.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Violation;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Número da gaiola na bateria: inteiro de 1 a 999; "07" e "7" são o mesmo número (data-model.md;
 * FR-006; spec, casos de borda).
 *
 * <p>O número chega como texto cru: decimal ou texto viram violação do campo, junto das demais, e não
 * corpo ilegível (FR-017, R-013).
 */
@DisplayName("CageNumber")
class CageNumberTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("rejects a missing number")
    void givenMissingNumber_whenCreating_thenRejectAsRequired(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> CageNumber.of(raw));

        // then
        assertThat(details).containsExactly(Map.entry("number", "Informe o número da gaiola."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"7.5", "sete", "7a", "1e2"})
    @DisplayName("rejects a number that is not an integer")
    void givenNumberThatIsNotAnInteger_whenCreating_thenRejectAsNotInteger(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> CageNumber.of(raw));

        // then
        assertThat(details).containsExactly(Map.entry("number", "O número da gaiola deve ser um número inteiro."));
        assertThat(violationsOf(() -> CageNumber.of(raw)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.CAGE_NUMBER_NOT_INTEGER);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"0", "1000", "-1", "99999999999999999999"})
    @DisplayName("rejects a number outside 1 to 999, with the message the contract publishes")
    void givenNumberOutsideTheRange_whenCreating_thenRejectAsOutOfRange(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> CageNumber.of(raw));

        // then
        assertThat(details).containsExactly(Map.entry("number", "O número da gaiola deve ficar entre 1 e 999."));
        assertThat(violationsOf(() -> CageNumber.of(raw)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.CAGE_NUMBER_OUT_OF_RANGE);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"1", "999", " 7 "})
    @DisplayName("accepts a number from 1 to 999")
    void givenNumberInsideTheRange_whenCreating_thenAcceptIt(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        CageNumber number = CageNumber.of(raw);

        // then
        assertThat(number.value()).isEqualTo(Integer.parseInt(raw.strip()));
    }

    @Test
    @DisplayName("reads \"07\" and \"7\" as the same number")
    void givenNumberWithLeadingZero_whenCreating_thenFindTheSameNumber() {
        // given
        String withZero = "07";

        // when
        CageNumber number = CageNumber.of(withZero);

        // then
        assertThat(number).isEqualTo(CageNumber.of("7"));
    }
}
