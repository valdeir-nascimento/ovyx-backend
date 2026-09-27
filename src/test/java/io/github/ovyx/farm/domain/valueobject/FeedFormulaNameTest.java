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

/** Nome da fórmula: obrigatório; aparado; de 2 a 80 caracteres (data-model.md da 004; R-011). */
@DisplayName("FeedFormulaName")
class FeedFormulaNameTest {

    @Test
    @DisplayName("keeps the name trimmed")
    void givenNameWithSurroundingSpaces_whenCreating_thenKeepItTrimmed() {
        // given
        String padded = "  Postura Plus ";

        // when
        FeedFormulaName name = FeedFormulaName.of(padded);

        // then
        assertThat(name.value()).isEqualTo("Postura Plus");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("rejects a missing name, with the message the contract publishes")
    void givenMissingName_whenCreating_thenRejectAsRequired(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> FeedFormulaName.of(raw));

        // then
        assertThat(details).containsExactly(Map.entry("name", "Informe o nome da fórmula."));
        assertThat(violationsOf(() -> FeedFormulaName.of(raw)))
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("name", FarmErrorCode.FEED_FORMULA_NAME_REQUIRED));
    }

    @Test
    @DisplayName("rejects a name of one character after trimming")
    void givenNameOfOneCharacterAfterTrimming_whenCreating_thenRejectAsTooShort() {
        // given
        String tooShort = "  P  ";

        // when
        Map<String, String> details = detailsOf(() -> FeedFormulaName.of(tooShort));

        // then
        assertThat(details).containsExactly(Map.entry("name", "O nome da fórmula deve ter ao menos 2 caracteres."));
        assertThat(violationsOf(() -> FeedFormulaName.of(tooShort)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.FEED_FORMULA_NAME_TOO_SHORT);
    }

    @Test
    @DisplayName("counts characters, and not UTF-16 units, as the database does")
    void givenNameOfOneCharacterOutsideTheBasicPlane_whenCreating_thenRejectAsTooShort() {
        // given
        String oneCharacter = "🌾";

        // when
        List<Violation> violations = violationsOf(() -> FeedFormulaName.of(oneCharacter));

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(FarmErrorCode.FEED_FORMULA_NAME_TOO_SHORT);
    }

    @Test
    @DisplayName("rejects a name longer than 80 characters")
    void givenNameLongerThan80Characters_whenCreating_thenRejectAsTooLong() {
        // given
        String tooLong = "P".repeat(81);

        // when
        Map<String, String> details = detailsOf(() -> FeedFormulaName.of(tooLong));

        // then
        assertThat(details).containsExactly(Map.entry("name", "O nome da fórmula deve ter no máximo 80 caracteres."));
        assertThat(violationsOf(() -> FeedFormulaName.of(tooLong)))
                .extracting(Violation::code)
                .containsExactly(FarmErrorCode.FEED_FORMULA_NAME_TOO_LONG);
    }

    @ParameterizedTest(name = "accepts exactly {0} characters")
    @ValueSource(ints = {2, 80})
    @DisplayName("accepts exactly 2 and exactly 80 characters")
    void givenNameAtTheLimitOfTheLength_whenCreating_thenAcceptIt(int length) {
        // given
        String raw = "P".repeat(length);

        // when
        FeedFormulaName name = FeedFormulaName.of(raw);

        // then
        assertThat(name.value()).hasSize(length);
    }

    @Test
    @DisplayName("treats names differing only in case as the same name")
    void givenNamesDifferingOnlyInCase_whenComparing_thenFindTheSameName() {
        // given
        FeedFormulaName original = FeedFormulaName.of("Postura Plus");

        // when
        boolean same = original.sameAs(FeedFormulaName.of(" POSTURA plus "));

        // then
        assertThat(same).isTrue();
    }

    @Test
    @DisplayName("treats different names as different")
    void givenDifferentNames_whenComparing_thenFindDifferentNames() {
        // given
        FeedFormulaName original = FeedFormulaName.of("Postura Plus");

        // when
        boolean same = original.sameAs(FeedFormulaName.of("Postura"));

        // then
        assertThat(same).isFalse();
    }
}
