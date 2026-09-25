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

/** Nome do setor: obrigatório; aparado; de 2 a 80 caracteres (data-model.md; FR-001, FR-002). */
@DisplayName("SectorName")
class SectorNameTest {

    @Test
    @DisplayName("keeps the name trimmed")
    void givenNameWithSurroundingSpaces_whenCreating_thenKeepItTrimmed() {
        // given
        String padded = " Codornas — Galpão 4 ";

        // when
        SectorName name = SectorName.of(padded);

        // then
        assertThat(name.value()).isEqualTo("Codornas — Galpão 4");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("rejects a missing name")
    void givenMissingName_whenCreating_thenRejectAsRequired(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        List<Violation> violations = violationsOf(() -> SectorName.of(raw));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("name", FarmErrorCode.SECTOR_NAME_REQUIRED));
    }

    @Test
    @DisplayName("rejects a name of one character after trimming, with the message the contract publishes")
    void givenNameOfOneCharacterAfterTrimming_whenCreating_thenRejectAsTooShort() {
        // given
        String tooShort = "  A  ";

        // when
        Map<String, String> details = detailsOf(() -> SectorName.of(tooShort));

        // then
        assertThat(details).containsExactly(Map.entry("name", "O nome do setor deve ter ao menos 2 caracteres."));
    }

    @Test
    @DisplayName("counts characters, and not UTF-16 units, as the database does")
    void givenNameOfOneCharacterOutsideTheBasicPlane_whenCreating_thenRejectAsTooShort() {
        // given
        // Um caractere que o Java guarda em duas unidades. Contando unidades, o nome passava aqui e o
        // CHECK do banco, que conta caracteres, recusava a gravação com erro técnico.
        String oneCharacter = "🐔";

        // when
        List<Violation> violations = violationsOf(() -> SectorName.of(oneCharacter));

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(FarmErrorCode.SECTOR_NAME_TOO_SHORT);
    }

    @Test
    @DisplayName("rejects a name longer than 80 characters")
    void givenNameLongerThan80Characters_whenCreating_thenRejectAsTooLong() {
        // given
        String tooLong = "G".repeat(81);

        // when
        List<Violation> violations = violationsOf(() -> SectorName.of(tooLong));

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(FarmErrorCode.SECTOR_NAME_TOO_LONG);
    }

    @ParameterizedTest(name = "accepts exactly {0} characters")
    @ValueSource(ints = {2, 80})
    @DisplayName("accepts exactly 2 and exactly 80 characters")
    void givenNameAtTheLimitOfTheLength_whenCreating_thenAcceptIt(int length) {
        // given
        String raw = "G".repeat(length);

        // when
        SectorName name = SectorName.of(raw);

        // then
        assertThat(name.value()).hasSize(length);
    }

    @Test
    @DisplayName("treats names differing only in case as the same name")
    void givenNamesDifferingOnlyInCase_whenComparing_thenFindTheSameName() {
        // given
        SectorName original = SectorName.of("Codornas — Galpão 4");

        // when
        boolean same = original.sameAs(SectorName.of(" codornas — GALPÃO 4 "));

        // then
        assertThat(same).isTrue();
    }

    @Test
    @DisplayName("treats different names as different")
    void givenDifferentNames_whenComparing_thenFindDifferentNames() {
        // given
        SectorName original = SectorName.of("Codornas — Galpão 4");

        // when
        boolean same = original.sameAs(SectorName.of("Codornas — Galpão 5"));

        // then
        assertThat(same).isFalse();
    }
}
