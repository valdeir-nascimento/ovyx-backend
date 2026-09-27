package io.github.ovyx.farm.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** O identificador da gaiola lido do endereço: o malformado recebe a mesma resposta do inexistente. */
@DisplayName("CageId")
class CageIdTest {

    @Test
    @DisplayName("reads a well-formed identifier")
    void givenWellFormedIdentifier_whenParsing_thenFindTheCage() {
        // given
        UUID value = UUID.fromString("9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44");

        // when
        Optional<CageId> id = CageId.parse(value.toString());

        // then
        assertThat(id).contains(CageId.of(value));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @NullSource
    @ValueSource(strings = {"b-07", "", "   ", "1-2-3-4-5", "9d2e4f6a-1b3c-4d5e-8f7a"})
    @DisplayName("reads a malformed or abbreviated identifier as no identifier")
    void givenMalformedOrAbbreviatedIdentifier_whenParsing_thenFindNone(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource
        // "1-2-3-4-5" o UUID.fromString aceita, abreviado; só a forma canônica aponta uma gaiola.

        // when
        Optional<CageId> id = CageId.parse(raw);

        // then
        assertThat(id).isEmpty();
    }
}
