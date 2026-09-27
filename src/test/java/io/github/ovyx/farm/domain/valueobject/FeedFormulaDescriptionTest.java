package io.github.ovyx.farm.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Violation;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Descrição da fórmula: opcional; aparada; até 500 caracteres; vazia vira ausente (data-model.md da 004). */
@DisplayName("FeedFormulaDescription")
class FeedFormulaDescriptionTest {

    @Test
    @DisplayName("keeps the description trimmed")
    void givenDescriptionWithSurroundingSpaces_whenCreating_thenKeepItTrimmed() {
        // given
        String padded = "  Milho, farelo de soja e calcário  ";

        // when
        Optional<FeedFormulaDescription> description = FeedFormulaDescription.optionalOf(padded);

        // then
        assertThat(description).map(FeedFormulaDescription::value).contains("Milho, farelo de soja e calcário");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("treats a missing or blank description as no description")
    void givenMissingOrBlankDescription_whenCreating_thenHaveNone(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Optional<FeedFormulaDescription> description = FeedFormulaDescription.optionalOf(raw);

        // then
        assertThat(description).isEmpty();
    }

    @Test
    @DisplayName("rejects a description longer than 500 characters, with the message the contract publishes")
    void givenDescriptionLongerThan500Characters_whenCreating_thenRejectAsTooLong() {
        // given
        String tooLong = "d".repeat(501);

        // when
        Map<String, String> details = detailsOf(() -> FeedFormulaDescription.optionalOf(tooLong));

        // then
        assertThat(details).containsExactly(Map.entry("description", "A descrição deve ter no máximo 500 caracteres."));
        assertThat(violationsOf(() -> FeedFormulaDescription.optionalOf(tooLong)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.FEED_FORMULA_DESCRIPTION_TOO_LONG);
    }

    @Test
    @DisplayName("counts characters, and not UTF-16 units: accepts 500 emojis")
    void givenFiveHundredCharactersOutsideTheBasicPlane_whenCreating_thenAcceptThem() {
        // given
        String fiveHundred = "🌽".repeat(500);

        // when
        Optional<FeedFormulaDescription> description = FeedFormulaDescription.optionalOf(fiveHundred);

        // then
        assertThat(description).map(FeedFormulaDescription::value).contains(fiveHundred);
    }
}
