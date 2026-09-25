package io.github.ovyx.farm.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * O identificador do setor lido do endereço (spec, caso de borda "identificador inexistente ou
 * malformado").
 *
 * <p>O malformado vira "nenhum", e não exceção: quem pede um setor que não existe e quem digita um
 * endereço torto recebem a mesma resposta, "setor não encontrado".
 */
@DisplayName("SectorId")
class SectorIdTest {

    @Test
    @DisplayName("reads a well-formed identifier")
    void givenWellFormedIdentifier_whenParsing_thenFindTheSector() {
        // given
        UUID value = UUID.fromString("3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11");

        // when
        Optional<SectorId> id = SectorId.parse(value.toString());

        // then
        assertThat(id).contains(SectorId.of(value));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @NullSource
    @ValueSource(strings = {"galpao-9", "", "   ", "3f6c2b1a-8d4e-4c7f-9a2b", "1-2-3-4-5"})
    @DisplayName("reads a malformed identifier as no identifier")
    void givenMalformedIdentifier_whenParsing_thenFindNone(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Optional<SectorId> id = SectorId.parse(raw);

        // then
        assertThat(id).isEmpty();
    }
}
