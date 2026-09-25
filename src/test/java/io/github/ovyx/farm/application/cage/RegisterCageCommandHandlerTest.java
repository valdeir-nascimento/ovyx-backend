package io.github.ovyx.farm.application.cage;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.Cage;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.fixtures.InMemorySectorRepository;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Cadastro de gaiola num setor (US2, cenários 1 a 4 e 7; FR-006, FR-007, FR-017). */
@DisplayName("RegisterCageCommandHandler")
class RegisterCageCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-25T13:10:42Z");
    private final InMemorySectorRepository repository = new InMemorySectorRepository();
    private final RegisterCageCommandHandler handler = new RegisterCageCommandHandler(repository, clock);

    private Sector saved(Sector sector) {
        repository.save(sector);
        return sector;
    }

    @Test
    @DisplayName("registers the cage in the sector and saves the sector")
    void givenValidCage_whenRegistering_thenSaveItInTheSector() {
        // given
        Sector sector = saved(aSector().withClock(clock).build());

        // when
        Result<CageId> result =
                handler.handle(new RegisterCageCommand(sector.id().toString(), "b", "7", "50"));

        // then
        Cage cage = repository.findById(sector.id()).orElseThrow().cage(result.value()).orElseThrow();
        assertThat(cage.code()).isEqualTo("B-07");
        assertThat(repository.saves()).isEqualTo(2);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44", "galpao-9"})
    @DisplayName("fails as not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenRegistering_thenFailAsSectorNotFound(String sectorId) {
        // given
        RegisterCageCommand command = new RegisterCageCommand(sectorId, "B", "7", "50");

        // when
        Result<CageId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as validation with the three invalid fields, and saves nothing")
    void givenThreeInvalidFields_whenRegistering_thenFailAsValidationAndSaveNothing() {
        // given
        Sector sector = saved(aSector().withClock(clock).build());

        // when
        Result<CageId> result = handler.handle(new RegisterCageCommand(sector.id().toString(), "", "0", "-1"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details()).containsOnlyKeys("battery", "number", "birdCount");
        assertThat(repository.saves()).isEqualTo(1);
    }

    @Test
    @DisplayName("fails as conflict with the battery and number of another active cage")
    void givenActiveCageB07_whenRegisteringB07_thenFailAsConflict() {
        // given
        Sector sector = saved(aSector().withCage("B", 7, 50).withClock(clock).build());

        // when
        Result<CageId> result = handler.handle(new RegisterCageCommand(sector.id().toString(), "B", "7", "48"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("CAGE_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("fails as conflict, with the sector inactive as the reason, in an inactive sector, and saves nothing")
    void givenInactiveSector_whenRegisteringACage_thenFailAsSectorInactive() {
        // given
        Sector sector = saved(aSector().inactive().withClock(clock).build());

        // when
        Result<CageId> result = handler.handle(new RegisterCageCommand(sector.id().toString(), "B", "7", "50"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_INACTIVE");
        assertThat(repository.saves()).isEqualTo(1);
    }
}
