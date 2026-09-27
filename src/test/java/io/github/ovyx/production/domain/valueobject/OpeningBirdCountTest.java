package io.github.ovyx.production.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Aves no início do dia: inteiro de 1 a 1.000.000 (FR-001). Chega como texto cru, e o que não é inteiro
 * vira violação do campo, junto das demais (R-010).
 */
@DisplayName("OpeningBirdCount")
class OpeningBirdCountTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("rejects a missing count")
    void givenMissingCount_whenCreating_thenRejectAsRequired(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> OpeningBirdCount.of(raw));

        // then
        assertThat(details).containsExactly(Map.entry("openingBirdCount", "Informe as aves no início do dia."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"12.5", "doze", "1e3"})
    @DisplayName("rejects a count that is not an integer")
    void givenCountThatIsNotAnInteger_whenCreating_thenRejectAsNotInteger(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> OpeningBirdCount.of(raw));

        // then
        assertThat(details)
                .containsExactly(Map.entry(
                        "openingBirdCount", "As aves do início do dia devem ser um número inteiro."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"0", "1000001", "-5", "99999999999"})
    @DisplayName("rejects a count outside 1 to 1,000,000")
    void givenCountOutOfRange_whenCreating_thenRejectAsOutOfRange(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> OpeningBirdCount.of(raw));

        // then
        assertThat(details)
                .containsExactly(Map.entry(
                        "openingBirdCount", "As aves do início do dia devem ficar entre 1 e 1.000.000."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"1", "1000000", "098"})
    @DisplayName("accepts a count at the limits")
    void givenCountWithinTheLimits_whenCreating_thenAcceptIt(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        OpeningBirdCount count = OpeningBirdCount.of(raw);

        // then
        assertThat(count.value()).isEqualTo(Integer.parseInt(raw));
    }
}
