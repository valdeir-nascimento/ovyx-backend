package io.github.ovyx.farm.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Violation;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Preço por quilo: decimal de 0,01 a 1.000,00, até duas casas; chega como texto cru (data-model.md da
 * 004; R-011).
 */
@DisplayName("PricePerKg")
class PricePerKgTest {

    @ParameterizedTest(name = "\"{0}\" is R$ {1}")
    @CsvSource(
            delimiter = ';',
            value = {"2,85; 2.85", "2.85; 2.85", " 2,85 ; 2.85", "1000; 1000.00", "0,01; 0.01", "3,1; 3.10", "7; 7.00"})
    @DisplayName("reads a comma or a dot as the decimal separator, and keeps two decimal places")
    void givenPriceTypedWithCommaOrDot_whenCreating_thenKeepItWithTwoDecimals(String raw, String expected) {
        // given — valor bruto vindo do @CsvSource

        // when
        PricePerKg price = PricePerKg.of(raw);

        // then
        assertThat(price.value()).isEqualByComparingTo(expected).hasScaleOf(2);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("rejects a missing price")
    void givenMissingPrice_whenCreating_thenRejectAsRequired(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> PricePerKg.of(raw));

        // then
        assertThat(details).containsExactly(Map.entry("pricePerKg", "Informe o preço por quilo."));
        assertThat(violationsOf(() -> PricePerKg.of(raw)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.PRICE_REQUIRED);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"2,855", "abc", "1.000,00", "2,", ",85", "R$ 2,85", "2,8,5"})
    @DisplayName("rejects a price that is not reais with up to two decimal places")
    void givenPriceThatIsNotReaisWithTwoDecimals_whenCreating_thenRejectAsInvalid(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> PricePerKg.of(raw));

        // then
        assertThat(details)
                .containsExactly(Map.entry("pricePerKg", "Informe o preço em reais, com até duas casas decimais."));
        assertThat(violationsOf(() -> PricePerKg.of(raw)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.PRICE_INVALID);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"0", "0,00", "1000,01", "-2,85", "99999999999999"})
    @DisplayName("rejects a price outside R$ 0.01 to R$ 1,000.00, with the message the contract publishes")
    void givenPriceOutsideTheRange_whenCreating_thenRejectAsOutOfRange(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> PricePerKg.of(raw));

        // then
        assertThat(details)
                .containsExactly(Map.entry("pricePerKg", "O preço deve ficar entre R$ 0,01 e R$ 1.000,00 o quilo."));
        assertThat(violationsOf(() -> PricePerKg.of(raw)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.PRICE_OUT_OF_RANGE);
    }
}
