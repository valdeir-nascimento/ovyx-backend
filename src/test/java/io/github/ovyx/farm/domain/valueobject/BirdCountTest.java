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
 * Quantidade de aves da gaiola: inteiro de 0 a 1.000; zero é gaiola vazia (data-model.md; FR-006;
 * spec, casos de borda).
 */
@DisplayName("BirdCount")
class BirdCountTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("rejects a missing bird count")
    void givenMissingBirdCount_whenCreating_thenRejectAsRequired(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> BirdCount.of(raw));

        // then
        assertThat(details).containsExactly(Map.entry("birdCount", "Informe a quantidade de aves."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"12.5", "cinquenta"})
    @DisplayName("rejects a bird count that is not an integer")
    void givenBirdCountThatIsNotAnInteger_whenCreating_thenRejectAsNotInteger(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> BirdCount.of(raw));

        // then
        assertThat(details)
                .containsExactly(Map.entry("birdCount", "A quantidade de aves deve ser um número inteiro."));
        assertThat(violationsOf(() -> BirdCount.of(raw)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.BIRD_COUNT_NOT_INTEGER);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"-1", "1001"})
    @DisplayName("rejects a bird count outside 0 to 1,000, with the message the contract publishes")
    void givenBirdCountOutsideTheRange_whenCreating_thenRejectAsOutOfRange(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> BirdCount.of(raw));

        // then
        assertThat(details)
                .containsExactly(Map.entry("birdCount", "A quantidade de aves deve ficar entre 0 e 1.000."));
        assertThat(violationsOf(() -> BirdCount.of(raw)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.BIRD_COUNT_OUT_OF_RANGE);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"0", "1000", "50"})
    @DisplayName("accepts from 0 to 1,000 birds, zero being an empty cage")
    void givenBirdCountInsideTheRange_whenCreating_thenAcceptIt(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        BirdCount birdCount = BirdCount.of(raw);

        // then
        assertThat(birdCount.value()).isEqualTo(Integer.parseInt(raw));
    }
}
