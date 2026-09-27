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

/** Inativação de gaiola (US3, cenários 1 e 2; FR-012). */
@DisplayName("DeactivateCageCommandHandler")
class DeactivateCageCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-25T14:03:51Z");
    private final InMemorySectorRepository repository = new InMemorySectorRepository();
    private final DeactivateCageCommandHandler handler = new DeactivateCageCommandHandler(repository, clock);

    @Test
    @DisplayName("deactivates the cage and saves the sector")
    void givenActiveCage_whenDeactivating_thenSaveItInactive() {
        // given
        Sector sector = aSector().withCage("A", 2, 48).withClock(clock).build();
        repository.save(sector);
        Cage a02 = sector.cages().getFirst();

        // when
        Result<CageId> result =
                handler.handle(new DeactivateCageCommand(sector.id().toString(), a02.id().toString()));

        // then
        Cage saved = repository.findById(sector.id()).orElseThrow().cage(result.value()).orElseThrow();
        assertThat(saved.isActive()).isFalse();
        assertThat(saved.deactivatedWithSector()).isFalse();
    }

    @Test
    @DisplayName("fails as not found for a cage the sector does not have, a malformed one, or a sector that does not exist")
    void givenUnknownCageOrSector_whenDeactivating_thenFailAsNotFound() {
        // given
        Sector sector = aSector().withCage("A", 2, 48).withClock(clock).build();
        repository.save(sector);

        // when
        Result<CageId> unknownCage =
                handler.handle(new DeactivateCageCommand(sector.id().toString(), CageId.generate().toString()));
        Result<CageId> malformedCage = handler.handle(new DeactivateCageCommand(sector.id().toString(), "a-02"));
        Result<CageId> unknownSector = handler.handle(
                new DeactivateCageCommand("galpao-9", sector.cages().getFirst().id().toString()));

        // then
        assertThat(unknownCage.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(unknownCage.error().code()).isEqualTo("CAGE_NOT_FOUND");
        assertThat(malformedCage.error().code()).isEqualTo("CAGE_NOT_FOUND");
        assertThat(unknownSector.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @Test
    @DisplayName("saves nothing when the cage is already inactive")
    void givenInactiveCage_whenDeactivating_thenSaveNothing() {
        // given
        // Gravar sem mudar subiria a versao do setor, e uma escrita simultanea legitima se repetiria a toa.
        Sector sector = aSector().withInactiveCage("A", 2, 48).withClock(clock).build();
        repository.save(sector);

        // when
        Result<CageId> result = handler.handle(
                new DeactivateCageCommand(sector.id().toString(), sector.cages().getFirst().id().toString()));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(repository.saves()).isEqualTo(1);
    }
}
