package io.github.ovyx.farm.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Descrição do setor: opcional; aparada; até 500 caracteres; vazia vira ausente (data-model.md). */
@DisplayName("SectorDescription")
class SectorDescriptionTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("turns a missing or blank description into no description")
    void givenMissingOrBlankDescription_whenCreating_thenFindNoDescription(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Optional<SectorDescription> description = SectorDescription.optionalOf(raw);

        // then
        assertThat(description).isEmpty();
    }

    @Test
    @DisplayName("keeps the description trimmed")
    void givenDescriptionWithSurroundingSpaces_whenCreating_thenKeepItTrimmed() {
        // given
        String padded = "  Codornas japonesas em postura, baterias A e B  ";

        // when
        Optional<SectorDescription> description = SectorDescription.optionalOf(padded);

        // then
        assertThat(description)
                .map(SectorDescription::value)
                .contains("Codornas japonesas em postura, baterias A e B");
    }

    @Test
    @DisplayName("accepts exactly 500 characters")
    void givenDescriptionOf500Characters_whenCreating_thenAcceptIt() {
        // given
        String raw = "d".repeat(500);

        // when
        Optional<SectorDescription> description = SectorDescription.optionalOf(raw);

        // then
        assertThat(description).map(SectorDescription::value).contains(raw);
    }

    @Test
    @DisplayName("rejects more than 500 characters, with the message the contract publishes")
    void givenDescriptionOf501Characters_whenCreating_thenRejectAsTooLong() {
        // given
        String tooLong = "d".repeat(501);

        // when
        Map<String, String> details = detailsOf(() -> SectorDescription.optionalOf(tooLong));

        // then
        assertThat(details)
                .containsExactly(Map.entry("description", "A descrição deve ter no máximo 500 caracteres."));
    }
}
