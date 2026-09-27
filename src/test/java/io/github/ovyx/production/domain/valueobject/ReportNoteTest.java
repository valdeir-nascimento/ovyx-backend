package io.github.ovyx.production.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Observação do relatório: opcional, aparada, até 500 caracteres; vazia vira ausente (FR-001). */
@DisplayName("ReportNote")
class ReportNoteTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("takes a missing or blank note as no note")
    void givenMissingOrBlankNote_whenCreating_thenFindNone(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Optional<ReportNote> note = ReportNote.optionalOf(raw);

        // then
        assertThat(note).isEmpty();
    }

    @Test
    @DisplayName("accepts 500 characters, trimmed")
    void givenFiveHundredCharactersWithSpaces_whenCreating_thenKeepThemTrimmed() {
        // given
        String raw = "  " + "b".repeat(500) + "  ";

        // when
        Optional<ReportNote> note = ReportNote.optionalOf(raw);

        // then
        assertThat(note).map(ReportNote::value).contains("b".repeat(500));
    }

    @Test
    @DisplayName("counts characters, and not UTF-16 units: 500 emojis are accepted")
    void givenFiveHundredEmojis_whenCreating_thenAcceptThem() {
        // given
        String raw = "🥚".repeat(500);

        // when
        Optional<ReportNote> note = ReportNote.optionalOf(raw);

        // then
        assertThat(note).map(ReportNote::value).contains(raw);
    }

    @Test
    @DisplayName("rejects 501 characters")
    void givenFiveHundredAndOneCharacters_whenCreating_thenRejectAsTooLong() {
        // given
        String raw = "b".repeat(501);

        // when
        Map<String, String> details = detailsOf(() -> ReportNote.optionalOf(raw));

        // then
        assertThat(details).containsExactly(Map.entry("note", "A observação deve ter no máximo 500 caracteres."));
    }
}
