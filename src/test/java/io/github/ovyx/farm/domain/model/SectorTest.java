package io.github.ovyx.farm.domain.model;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aSector;
import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalCodeOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalMessageOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.farm.domain.valueobject.SectorDescription;
import io.github.ovyx.farm.fixtures.InMemorySectorRepository;
import io.github.ovyx.shared.domain.FixedClock;
import io.github.ovyx.shared.domain.Violation;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O agregado {@link Sector}: cadastro e edição (US1; FR-001 a FR-003, FR-017; invariante 1 do
 * data-model.md). Sem mock: o quadro dos setores é o repositório em memória.
 */
@DisplayName("Sector")
class SectorTest {

    private static final String NAME_IN_USE = "Já existe um setor ativo com este nome.";

    private final FixedClock clock = FixedClock.at("2026-09-25T13:02:11Z");
    private final InMemorySectorRepository roster = new InMemorySectorRepository();

    private Sector saved(SectorTestDataBuilder builder) {
        Sector sector = builder.withRoster(roster).withClock(clock).build();
        roster.save(sector);
        return sector;
    }

    @Test
    @DisplayName("registers an active sector, without cages")
    void givenValidNameAndDescription_whenRegistering_thenCreateAnActiveSectorWithoutCages() {
        // given
        String name = "Codornas — Galpão 4";

        // when
        Sector sector = Sector.register(name, "Codornas japonesas em postura, baterias A e B", roster, clock);

        // then
        assertThat(sector.name().value()).isEqualTo(name);
        assertThat(sector.description())
                .map(SectorDescription::value)
                .contains("Codornas japonesas em postura, baterias A e B");
        assertThat(sector.status()).isEqualTo(Status.ACTIVE);
        assertThat(sector.createdAt()).isEqualTo(clock.instant());
        assertThat(sector.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("registers a sector with a blank description as having no description")
    void givenBlankDescription_whenRegistering_thenKeepNoDescription() {
        // given
        String blank = "   ";

        // when
        Sector sector = Sector.register("Codornas — Galpão 4", blank, roster, clock);

        // then
        assertThat(sector.description()).isEmpty();
    }

    @Test
    @DisplayName("refuses a short name and a long description at once, one violation per field")
    void givenShortNameAndLongDescription_whenRegistering_thenRefuseWithBothViolationsAtOnce() {
        // given
        String shortName = "A";
        String longDescription = "d".repeat(501);

        // when
        List<Violation> violations = violationsOf(() -> Sector.register(shortName, longDescription, roster, clock));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(
                        tuple("name", FarmErrorCode.SECTOR_NAME_TOO_SHORT),
                        tuple(
                                "description", FarmErrorCode.SECTOR_DESCRIPTION_TOO_LONG));
        assertThat(refusalCodeOf(() -> Sector.register(shortName, longDescription, roster, clock)))
                .isEqualTo(FarmErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("refuses the name of another active sector, differing only in case and spaces")
    void givenActiveSectorWithTheSameName_whenRegisteringWithOtherCaseAndSpaces_thenRefuseAsNameInUse() {
        // given
        saved(aSector().named("Codornas — Galpão 4"));

        // when
        Map<String, String> details =
                detailsOf(() -> Sector.register(" codornas — galpão 4 ", null, roster, clock));

        // then
        assertThat(details).containsExactly(Map.entry("name", NAME_IN_USE));
        assertThat(refusalCodeOf(() -> Sector.register(" codornas — galpão 4 ", null, roster, clock)))
                .isEqualTo(FarmErrorCode.SECTOR_NAME_IN_USE);
        assertThat(refusalMessageOf(() -> Sector.register(" codornas — galpão 4 ", null, roster, clock)))
                .isEqualTo(NAME_IN_USE);
    }

    @Test
    @DisplayName("accepts the name of an inactive sector")
    void givenInactiveSectorWithTheSameName_whenRegistering_thenAcceptIt() {
        // given
        saved(aSector().named("Codornas — Galpão 4").inactive());

        // when
        Sector sector = Sector.register("Codornas — Galpão 4", null, roster, clock);

        // then
        assertThat(sector.isActive()).isTrue();
    }

    @Test
    @DisplayName("refuses the fields before looking for the name in use")
    void givenNameInUseAndLongDescription_whenRegistering_thenRefuseForTheFieldsFirst() {
        // given
        // Mesma ordem da 001: primeiro o que a pessoa digitou errado, depois o conflito com os outros.
        saved(aSector().named("Codornas — Galpão 4"));
        String longDescription = "d".repeat(501);

        // when
        FarmErrorCode code = (FarmErrorCode)
                refusalCodeOf(() -> Sector.register("Codornas — Galpão 4", longDescription, roster, clock));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("updates the name and the description, and the instant of the last change")
    void givenValidData_whenUpdating_thenChangeNameAndDescription() {
        // given
        Sector sector = saved(aSector());
        clock.advance(Duration.ofHours(2));

        // when
        sector.update("Codornas — Galpão 1 (norte)", "Baterias A a D, ala norte", roster, clock);

        // then
        assertThat(sector.name().value()).isEqualTo("Codornas — Galpão 1 (norte)");
        assertThat(sector.description()).map(SectorDescription::value).contains("Baterias A a D, ala norte");
        assertThat(sector.updatedAt()).isEqualTo(clock.instant());
        assertThat(sector.createdAt()).isBefore(sector.updatedAt());
    }

    @Test
    @DisplayName("keeps its own name without conflicting with itself")
    void givenOwnNameInOtherCase_whenUpdating_thenAcceptIt() {
        // given
        Sector sector = saved(aSector().named("Codornas — Galpão 1"));

        // when
        sector.update("CODORNAS — GALPÃO 1", null, roster, clock);

        // then
        assertThat(sector.name().value()).isEqualTo("CODORNAS — GALPÃO 1");
        assertThat(sector.description()).isEmpty();
    }

    @Test
    @DisplayName("refuses the name of another active sector and keeps its data")
    void givenNameOfAnotherActiveSector_whenUpdating_thenRefuseAsNameInUseAndKeepTheData() {
        // given
        saved(aSector().named("Poedeiras brancas — Galpão 2"));
        Sector sector = saved(aSector().named("Codornas — Galpão 1"));

        // when
        FarmErrorCode code = (FarmErrorCode) refusalCodeOf(
                () -> sector.update("poedeiras brancas — galpão 2", null, roster, clock));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.SECTOR_NAME_IN_USE);
        assertThat(sector.name().value()).isEqualTo("Codornas — Galpão 1");
    }

    @Test
    @DisplayName("refuses every invalid field at once and keeps its data")
    void givenMissingNameAndLongDescription_whenUpdating_thenRefuseWithAllViolationsAndKeepTheData() {
        // given
        Sector sector = saved(aSector());

        // when
        Map<String, String> details = detailsOf(() -> sector.update("  ", "d".repeat(501), roster, clock));

        // then
        assertThat(details)
                .containsExactly(
                        Map.entry("name", "Informe o nome do setor."),
                        Map.entry("description", "A descrição deve ter no máximo 500 caracteres."));
        assertThat(sector.name().value()).isEqualTo("Codornas — Galpão 1");
    }
}
