package io.github.ovyx.farm.application.cage;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aSector;
import static org.assertj.core.api.Assertions.assertThat;

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

/** Edição de gaiola (US2, cenário 6; FR-009). */
@DisplayName("UpdateCageCommandHandler")
class UpdateCageCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-25T13:10:42Z");
    private final InMemorySectorRepository repository = new InMemorySectorRepository();
    private final UpdateCageCommandHandler handler = new UpdateCageCommandHandler(repository, clock);

    private Sector sector;
    private CageId b07;

    private void givenSectorWithB07AndA12() {
        sector = aSector().withCage("A", 12, 40).withClock(clock).build();
        b07 = sector.registerCage("B", "7", "50", clock);
        repository.save(sector);
    }

    @Test
    @DisplayName("corrects the birds of a cage and saves the sector")
    void givenCageWith50Birds_whenCorrectingTo48_thenSaveTheNewCount() {
        // given
        givenSectorWithB07AndA12();

        // when
        Result<CageId> result =
                handler.handle(new UpdateCageCommand(sector.id().toString(), b07.toString(), "B", "7", "48"));

        // then
        assertThat(repository.findById(sector.id()).orElseThrow().cage(result.value()).orElseThrow()
                        .birdCount()
                        .value())
                .isEqualTo(48);
        assertThat(repository.saves()).isEqualTo(2);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44", "b-07"})
    @DisplayName("fails as not found for a cage the sector does not have, or a malformed identifier")
    void givenUnknownOrMalformedCage_whenUpdating_thenFailAsCageNotFound(String cageId) {
        // given
        givenSectorWithB07AndA12();

        // when
        Result<CageId> result =
                handler.handle(new UpdateCageCommand(sector.id().toString(), cageId, "B", "7", "48"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("CAGE_NOT_FOUND");
        assertThat(result.error().message()).isEqualTo("Gaiola não encontrada.");
    }

    @Test
    @DisplayName("fails as not found for a sector that does not exist")
    void givenUnknownSector_whenUpdating_thenFailAsSectorNotFound() {
        // given
        givenSectorWithB07AndA12();

        // when
        Result<CageId> result = handler.handle(
                new UpdateCageCommand("9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44", b07.toString(), "B", "7", "48"));

        // then
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as validation with every invalid field, and saves nothing")
    void givenInvalidFields_whenUpdating_thenFailAsValidation() {
        // given
        givenSectorWithB07AndA12();

        // when
        Result<CageId> result = handler.handle(
                new UpdateCageCommand(sector.id().toString(), b07.toString(), "ABCD", "sete", "12.5"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details()).containsOnlyKeys("battery", "number", "birdCount");
        assertThat(repository.saves()).isEqualTo(1);
    }

    @Test
    @DisplayName("fails as conflict with the code of another active cage")
    void givenTwoCages_whenUpdatingOneToTheCodeOfTheOther_thenFailAsConflict() {
        // given
        givenSectorWithB07AndA12();

        // when
        Result<CageId> result =
                handler.handle(new UpdateCageCommand(sector.id().toString(), b07.toString(), "A", "12", "50"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("CAGE_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("fails as conflict, with the sector inactive as the reason, for a cage of an inactive sector")
    void givenInactiveSector_whenUpdatingACage_thenFailAsSectorInactive() {
        // given
        givenSectorWithB07AndA12();
        sector.deactivate(clock);

        // when
        Result<CageId> result =
                handler.handle(new UpdateCageCommand(sector.id().toString(), b07.toString(), "B", "7", "48"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_INACTIVE");
        assertThat(repository.saves()).isEqualTo(1);
    }
}
