package io.github.ovyx.production.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * O identificador do relatório lido do endereço: o malformado vira "nenhum", e recebe a mesma resposta
 * do inexistente, "relatório não encontrado".
 */
@DisplayName("DailyReportId")
class DailyReportIdTest {

    @Test
    @DisplayName("reads a well-formed identifier")
    void givenWellFormedIdentifier_whenParsing_thenFindTheReport() {
        // given
        UUID value = UUID.fromString("6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55");

        // when
        Optional<DailyReportId> id = DailyReportId.parse(value.toString());

        // then
        assertThat(id).contains(DailyReportId.of(value));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @NullSource
    @ValueSource(strings = {"24-09", "", "   ", "6b1d3f5a-7c9e-4a2b", "1-2-3-4-5"})
    @DisplayName("reads a malformed identifier as no identifier")
    void givenMalformedIdentifier_whenParsing_thenFindNone(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Optional<DailyReportId> id = DailyReportId.parse(raw);

        // then
        assertThat(id).isEmpty();
    }

    @Test
    @DisplayName("generates a new identifier each time")
    void givenTwoGenerations_whenComparing_thenFindThemDifferent() {
        // given
        DailyReportId first = DailyReportId.generate();

        // when
        DailyReportId second = DailyReportId.generate();

        // then
        assertThat(first).isNotEqualTo(second);
    }
}
