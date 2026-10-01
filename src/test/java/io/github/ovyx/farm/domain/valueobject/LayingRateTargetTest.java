package io.github.ovyx.farm.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Violation;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Meta de produtividade do setor: porcentagem de 1 a 100, com ate uma casa; chega como texto cru (R-003 e
 * data-model.md da 008).
 */
@DisplayName("LayingRateTarget")
class LayingRateTargetTest {

    @ParameterizedTest(name = "\"{0}\" is {1}%")
    @CsvSource(
            delimiter = ';',
            value = {"85; 85.0", "82,5; 82.5", "82.5; 82.5", "  72 ; 72.0", "1; 1.0", "100; 100.0", "100,0; 100.0"})
    @DisplayName("reads a comma or a dot as the decimal separator, and keeps one decimal place")
    void givenTargetTypedWithCommaOrDot_whenCreating_thenKeepItWithOneDecimal(String raw, String expected) {
        // given — valor bruto vindo do @CsvSource

        // when
        LayingRateTarget target = LayingRateTarget.of(raw);

        // then
        assertThat(target.value()).isEqualByComparingTo(expected).hasScaleOf(1);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("rejects a missing target")
    void givenMissingTarget_whenCreating_thenRejectAsRequired(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> LayingRateTarget.of(raw));

        // then
        assertThat(details)
                .containsExactly(Map.entry("layingRateTarget", "Informe a meta de produtividade, de 1 a 100%."));
        assertThat(violationsOf(() -> LayingRateTarget.of(raw)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.LAYING_RATE_TARGET_REQUIRED);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"abc", "82,55", "85%", "8,5,1", "1.000", "82,", ",5"})
    @DisplayName("rejects a target that is not a percentage with up to one decimal place")
    void givenTargetThatIsNotAPercentageWithOneDecimal_whenCreating_thenRejectAsInvalid(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> LayingRateTarget.of(raw));

        // then
        assertThat(details)
                .containsExactly(
                        Map.entry("layingRateTarget", "Informe a meta em porcentagem, com até uma casa decimal."));
        assertThat(violationsOf(() -> LayingRateTarget.of(raw)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.LAYING_RATE_TARGET_INVALID);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"0", "0,9", "100,1", "101", "-5", "99999999999999"})
    @DisplayName("rejects a target outside 1% to 100%, with the message the contract publishes")
    void givenTargetOutsideTheRange_whenCreating_thenRejectAsOutOfRange(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> LayingRateTarget.of(raw));

        // then
        assertThat(details).containsExactly(Map.entry("layingRateTarget", "A meta deve ficar entre 1% e 100%."));
        assertThat(violationsOf(() -> LayingRateTarget.of(raw)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.LAYING_RATE_TARGET_OUT_OF_RANGE);
    }

    @Test
    @DisplayName("refuses with the validation code when created from an invalid text")
    void givenInvalidTarget_whenCreating_thenThrowValidationFailed() {
        // given
        String raw = "abc";

        // when / then
        assertThatThrownBy(() -> LayingRateTarget.of(raw))
                .isInstanceOfSatisfying(
                        DomainException.class,
                        refusal -> assertThat(refusal.errorCode()).isEqualTo(FarmErrorCode.VALIDATION_FAILED));
    }

    @Test
    @DisplayName("records the violation without throwing when validating")
    void givenInvalidTarget_whenValidating_thenRecordOneViolationWithoutThrowing() {
        // given
        Notification notification = new Notification();

        // when
        LayingRateTarget.validate("101", notification);

        // then
        assertThat(notification.violations())
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.LAYING_RATE_TARGET_OUT_OF_RANGE);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"  72 ", "\t82,5\n", " 100"})
    @DisplayName("ignores the spaces around the target")
    void givenTargetWithSpacesAround_whenCreating_thenReadTheNumberInside(String raw) {
        // given — valor bruto vindo do @ValueSource, com os espacos que o @CsvSource apararia

        // when
        LayingRateTarget target = LayingRateTarget.of(raw);

        // then
        assertThat(target.value()).isEqualByComparingTo(raw.strip().replace(',', '.'));
    }

    @Test
    @DisplayName("keeps one decimal place whatever the scale it was restored with")
    void givenTargetRestoredWithoutDecimals_whenComparing_thenEqualTheTypedOne() {
        // given
        LayingRateTarget restored = new LayingRateTarget(new BigDecimal("72"));

        // when
        LayingRateTarget typed = LayingRateTarget.of("72");

        // then
        assertThat(restored).isEqualTo(typed);
        assertThat(restored.value()).hasScaleOf(1);
    }
}
