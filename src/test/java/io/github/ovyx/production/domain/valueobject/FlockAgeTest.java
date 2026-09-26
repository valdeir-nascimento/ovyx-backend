package io.github.ovyx.production.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Idade do lote: inteiro de 1 a 150 semanas (FR-001), como texto cru (R-010). */
@DisplayName("FlockAge")
class FlockAgeTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("rejects a missing age")
    void givenMissingAge_whenCreating_thenRejectAsRequired(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> FlockAge.of(raw));

        // then
        assertThat(details).containsExactly(Map.entry("flockAge", "Informe a idade do lote, em semanas."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"20,5", "20.5", "vinte"})
    @DisplayName("rejects an age that is not a whole number of weeks")
    void givenAgeThatIsNotAnInteger_whenCreating_thenRejectAsNotInteger(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> FlockAge.of(raw));

        // then
        assertThat(details)
                .containsExactly(Map.entry("flockAge", "A idade do lote deve ser um número inteiro de semanas."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"0", "151"})
    @DisplayName("rejects an age outside 1 to 150 weeks")
    void givenAgeOutOfRange_whenCreating_thenRejectAsOutOfRange(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> FlockAge.of(raw));

        // then
        assertThat(details)
                .containsExactly(Map.entry("flockAge", "A idade do lote deve ficar entre 1 e 150 semanas."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"1", "150"})
    @DisplayName("accepts an age at the limits")
    void givenAgeWithinTheLimits_whenCreating_thenAcceptIt(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        FlockAge age = FlockAge.of(raw);

        // then
        assertThat(age.value()).isEqualTo(Integer.parseInt(raw));
    }
}
