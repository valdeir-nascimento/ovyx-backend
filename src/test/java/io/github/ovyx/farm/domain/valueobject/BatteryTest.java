package io.github.ovyx.farm.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Violation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Bateria da gaiola: obrigatória; aparada e em maiúsculas; de 1 a 3 letras ou dígitos
 * (`^[A-Z0-9]{1,3}$`) (data-model.md; FR-006).
 */
@DisplayName("Battery")
class BatteryTest {

    @Test
    @DisplayName("keeps the battery trimmed and in capitals")
    void givenBatteryInLowerCaseWithSpaces_whenCreating_thenKeepItTrimmedInCapitals() {
        // given
        String typed = " b ";

        // when
        Battery battery = Battery.of(typed);

        // then
        assertThat(battery.value()).isEqualTo("B");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("rejects a missing battery, with the message the contract publishes")
    void givenMissingBattery_whenCreating_thenRejectAsRequired(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> Battery.of(raw));

        // then
        assertThat(details).containsExactly(Map.entry("battery", "Informe a bateria, com até 3 letras ou dígitos."));
        assertThat(violationsOf(() -> Battery.of(raw)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.CAGE_BATTERY_REQUIRED);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"AB-", "ABCD", "Ç", "B 1"})
    @DisplayName("rejects a battery outside one to three letters or digits")
    void givenBatteryOutsideThePattern_whenCreating_thenRejectAsInvalid(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        List<Violation> violations = violationsOf(() -> Battery.of(raw));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("battery", FarmErrorCode.CAGE_BATTERY_INVALID));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"A", "A1", "C12", "123"})
    @DisplayName("accepts one to three letters or digits")
    void givenBatteryInsideThePattern_whenCreating_thenAcceptIt(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Battery battery = Battery.of(raw);

        // then
        assertThat(battery.value()).isEqualTo(raw);
    }
}
