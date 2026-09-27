package io.github.ovyx.production.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.shared.domain.Violation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A mortalidade de uma gaiola (US3; FR-011, FR-012): mortes e descartes, cada um de 0 a 1.000, em branco
 * valendo zero, e uma observação de até 500 caracteres. Os valores chegam como texto cru, e todas as
 * violações vêm de uma vez (R-010).
 */
@DisplayName("MortalityEntry")
class MortalityEntryTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("takes blank deaths and culls as zero")
    void givenBlankDeathsAndCulls_whenCreating_thenTakeThemAsZero(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        MortalityEntry entry = MortalityEntry.of(raw, raw, null);

        // then
        assertThat(entry.deaths()).isZero();
        assertThat(entry.culls()).isZero();
    }

    @Test
    @DisplayName("keeps the deaths, the culls and the note, trimmed")
    void givenDeathsCullsAndNote_whenCreating_thenKeepThem() {
        // given
        String note = "  Prostração e penas eriçadas.  ";

        // when
        MortalityEntry entry = MortalityEntry.of("2", "1", note);

        // then
        assertThat(entry.deaths()).isEqualTo(2);
        assertThat(entry.culls()).isEqualTo(1);
        assertThat(entry.note()).isEqualTo(new MortalityNote("Prostração e penas eriçadas."));
        assertThat(entry.removals()).isEqualTo(3);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("takes a missing or blank note as no note")
    void givenMissingOrBlankNote_whenCreating_thenFindNone(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        MortalityEntry entry = MortalityEntry.of("1", "0", raw);

        // then
        assertThat(entry.note()).isNull();
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource({
        "'1,5', 0, deaths, As mortes devem ser um número inteiro.",
        "0, '1,5', culls, Os descartes devem ser um número inteiro."
    })
    @DisplayName("rejects deaths or culls that are not an integer, in their own field")
    void givenQuantityThatIsNotAnInteger_whenCreating_thenRejectInItsField(
            String deaths, String culls, String field, String message) {
        // given — valores brutos vindos do @CsvSource

        // when
        Map<String, String> details = detailsOf(() -> MortalityEntry.of(deaths, culls, null));

        // then
        assertThat(details).containsExactly(Map.entry(field, message));
    }

    @ParameterizedTest(name = "{0} deaths and {1} culls")
    @CsvSource({
        "-1, 0, deaths, As mortes devem ficar entre 0 e 1.000.",
        "1001, 0, deaths, As mortes devem ficar entre 0 e 1.000.",
        "0, -1, culls, Os descartes devem ficar entre 0 e 1.000.",
        "0, 1001, culls, Os descartes devem ficar entre 0 e 1.000."
    })
    @DisplayName("rejects deaths or culls outside 0 to 1,000, in their own field")
    void givenQuantityOutOfRange_whenCreating_thenRejectInItsField(
            String deaths, String culls, String field, String message) {
        // given — valores brutos vindos do @CsvSource

        // when
        Map<String, String> details = detailsOf(() -> MortalityEntry.of(deaths, culls, null));

        // then
        assertThat(details).containsExactly(Map.entry(field, message));
    }

    @Test
    @DisplayName("accepts a note of 500 characters")
    void givenNoteOfFiveHundredCharacters_whenCreating_thenAcceptIt() {
        // given
        String note = "b".repeat(500);

        // when
        MortalityEntry entry = MortalityEntry.of("1", "0", note);

        // then
        assertThat(entry.note()).isEqualTo(new MortalityNote(note));
    }

    @Test
    @DisplayName("counts characters in the note, and not UTF-16 units: 500 emojis are accepted")
    void givenNoteOfFiveHundredEmojis_whenCreating_thenAcceptIt() {
        // given
        String note = "🐦".repeat(500);

        // when
        MortalityEntry entry = MortalityEntry.of("1", "0", note);

        // then
        assertThat(entry.note()).isEqualTo(new MortalityNote(note));
    }

    @Test
    @DisplayName("rejects a note of 501 characters")
    void givenNoteOfFiveHundredAndOneCharacters_whenCreating_thenRejectAsTooLong() {
        // given
        String note = "b".repeat(501);

        // when
        Map<String, String> details = detailsOf(() -> MortalityEntry.of("1", "0", note));

        // then
        assertThat(details).containsExactly(Map.entry("note", "A observação deve ter no máximo 500 caracteres."));
    }

    @Test
    @DisplayName("rejects every invalid field at once, each with the code of its rule")
    void givenThreeInvalidFields_whenCreating_thenRejectTheThreeTogether() {
        // given
        String note = "b".repeat(501);

        // when
        List<Violation> violations = violationsOf(() -> MortalityEntry.of("1,5", "-1", note));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactlyInAnyOrder(
                        tuple("deaths", ProductionErrorCode.DEATHS_NOT_INTEGER),
                        tuple("culls", ProductionErrorCode.CULLS_OUT_OF_RANGE),
                        tuple("note", ProductionErrorCode.MORTALITY_NOTE_TOO_LONG));
    }

    @Test
    @DisplayName("has no occurrence with zero deaths and zero culls")
    void givenZeroDeathsAndZeroCulls_whenAskingForAnOccurrence_thenFindNone() {
        // given
        MortalityEntry entry = MortalityEntry.of("0", "", null);

        // when
        boolean occurrence = entry.hasOccurrence();

        // then
        assertThat(occurrence).isFalse();
    }

    @ParameterizedTest(name = "{0} deaths and {1} culls")
    @CsvSource({"1, 0", "0, 1"})
    @DisplayName("has an occurrence with any death or cull")
    void givenOneDeathOrOneCull_whenAskingForAnOccurrence_thenFindOne(String deaths, String culls) {
        // given
        MortalityEntry entry = MortalityEntry.of(deaths, culls, null);

        // when
        boolean occurrence = entry.hasOccurrence();

        // then
        assertThat(occurrence).isTrue();
    }
}
