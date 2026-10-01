package io.github.ovyx.farm.application.sector;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.valueobject.ReferenceWeight;
import io.github.ovyx.farm.fixtures.InMemorySectorRepository;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Edição de setor pelo administrador (US1, cenário 4; FR-003). */
@DisplayName("UpdateSectorCommandHandler")
class UpdateSectorCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-25T13:02:11Z");
    private final InMemorySectorRepository repository = new InMemorySectorRepository();
    private final UpdateSectorCommandHandler handler = new UpdateSectorCommandHandler(repository, clock);

    private Sector saved(String name) {
        Sector sector = aSector().named(name).withRoster(repository).withClock(clock).build();
        repository.save(sector);
        return sector;
    }

    @Test
    @DisplayName("updates the name and the description, and saves them")
    void givenValidData_whenUpdating_thenSaveTheNewData() {
        // given
        Sector sector = saved("Codornas — Galpão 1");
        clock.advance(Duration.ofHours(1));
        UpdateSectorCommand command = new UpdateSectorCommand(
                sector.id().toString(), "Codornas — Galpão 1 (norte)", "Baterias A a D, ala norte", null, null, "85");

        // when
        Result<SectorId> result = handler.handle(command);

        // then
        Sector updated = repository.findById(result.value()).orElseThrow();
        assertThat(updated.name().value()).isEqualTo("Codornas — Galpão 1 (norte)");
        assertThat(updated.updatedAt()).isEqualTo(clock.instant());
        assertThat(repository.saves()).isEqualTo(2);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44", "galpao-9"})
    @DisplayName("fails as not found for an unknown or malformed identifier")
    void givenUnknownOrMalformedIdentifier_whenUpdating_thenFailAsNotFound(String sectorId) {
        // given
        UpdateSectorCommand command = new UpdateSectorCommand(sectorId, "Codornas — Galpão 1", null, null, null, "85");

        // when
        Result<SectorId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
        assertThat(result.error().message()).isEqualTo("Setor não encontrado.");
    }

    @Test
    @DisplayName("fails as validation with every invalid field, and saves nothing")
    void givenMissingNameAndLongDescription_whenUpdating_thenFailAsValidationAndSaveNothing() {
        // given
        Sector sector = saved("Codornas — Galpão 1");
        UpdateSectorCommand command = new UpdateSectorCommand(sector.id().toString(), " ", "d".repeat(501), null, null, "85");

        // when
        Result<SectorId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details()).containsOnlyKeys("name", "description");
        assertThat(repository.saves()).isEqualTo(1);
    }

    @Test
    @DisplayName("fails as conflict with the name of another active sector")
    void givenNameOfAnotherActiveSector_whenUpdating_thenFailAsConflict() {
        // given
        saved("Poedeiras brancas — Galpão 2");
        Sector sector = saved("Codornas — Galpão 1");
        UpdateSectorCommand command =
                new UpdateSectorCommand(sector.id().toString(), "Poedeiras Brancas — Galpão 2", null, null, null, "85");

        // when
        Result<SectorId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_NAME_IN_USE");
        assertThat(repository.saves()).isEqualTo(2);
    }

    // ---------------------------------------------------------------- faixa de peso de referência (005)

    @Test
    @DisplayName("sets the reference weight range of the sector")
    void givenReferenceWeight_whenUpdating_thenSaveTheRange() {
        // given
        Sector sector = saved("Codornas — Galpão 1");
        UpdateSectorCommand command =
                new UpdateSectorCommand(sector.id().toString(), sector.name().value(), null, "155", "175", "85");

        // when
        handler.handle(command);

        // then
        assertThat(repository.findById(sector.id()).orElseThrow().referenceWeight())
                .contains(new ReferenceWeight(155, 175));
    }

    // ---------------------------------------------------------------- meta de produtividade (008)

    @Test
    @DisplayName("changes the laying rate target of the sector")
    void givenSectorWith85_whenUpdatingTheTargetTo72_thenSave72() {
        // given
        Sector sector = saved("Codornas — Galpão 1");
        UpdateSectorCommand command =
                new UpdateSectorCommand(sector.id().toString(), sector.name().value(), null, null, null, "72");

        // when
        Result<SectorId> result = handler.handle(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(repository.findById(sector.id()).orElseThrow().layingRateTarget().value())
                .isEqualByComparingTo("72.0");
    }

    @Test
    @DisplayName("fails as validation without a target, and keeps the one saved")
    void givenMissingTarget_whenUpdating_thenFailAsValidationAndKeepTheTarget() {
        // given
        Sector sector = saved("Codornas — Galpão 1");
        UpdateSectorCommand command =
                new UpdateSectorCommand(sector.id().toString(), sector.name().value(), null, null, null, null);

        // when
        Result<SectorId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details())
                .containsOnly(Map.entry("layingRateTarget", "Informe a meta de produtividade, de 1 a 100%."));
        assertThat(repository.findById(sector.id()).orElseThrow().layingRateTarget().value())
                .isEqualByComparingTo("85.0");
    }
}
