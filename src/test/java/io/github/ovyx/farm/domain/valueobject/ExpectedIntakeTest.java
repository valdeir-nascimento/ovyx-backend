package io.github.ovyx.farm.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Violation;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Consumo esperado: gramas por ave ao dia, inteiro de 1 a 200; chega como texto cru (data-model.md da
 * 004; R-011).
 */
@DisplayName("ExpectedIntake")
class ExpectedIntakeTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("rejects a missing expected intake")
    void givenMissingExpectedIntake_whenCreating_thenRejectAsRequired(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> ExpectedIntake.of(raw));

        // then
        assertThat(details).containsExactly(Map.entry("expectedIntake", "Informe o consumo esperado."));
        assertThat(violationsOf(() -> ExpectedIntake.of(raw)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.EXPECTED_INTAKE_REQUIRED);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"28,5", "28.5", "vinte"})
    @DisplayName("rejects an expected intake that is not an integer")
    void givenExpectedIntakeThatIsNotAnInteger_whenCreating_thenRejectAsNotInteger(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> ExpectedIntake.of(raw));

        // then
        assertThat(details)
                .containsExactly(
                        Map.entry("expectedIntake", "O consumo esperado deve ser um número inteiro de gramas."));
        assertThat(violationsOf(() -> ExpectedIntake.of(raw)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.EXPECTED_INTAKE_NOT_INTEGER);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"0", "201", "-28", "99999999999"})
    @DisplayName("rejects an expected intake outside 1 to 200 grams, with the message the contract publishes")
    void givenExpectedIntakeOutsideTheRange_whenCreating_thenRejectAsOutOfRange(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> ExpectedIntake.of(raw));

        // then
        assertThat(details)
                .containsExactly(Map.entry(
                        "expectedIntake", "O consumo esperado deve ficar entre 1 e 200 gramas por ave ao dia."));
        assertThat(violationsOf(() -> ExpectedIntake.of(raw)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.EXPECTED_INTAKE_OUT_OF_RANGE);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"1", "200", "28", " 28 "})
    @DisplayName("accepts from 1 to 200 grams")
    void givenExpectedIntakeInsideTheRange_whenCreating_thenAcceptIt(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        ExpectedIntake intake = ExpectedIntake.of(raw);

        // then
        assertThat(intake.value()).isEqualTo(Integer.parseInt(raw.strip()));
    }
}
