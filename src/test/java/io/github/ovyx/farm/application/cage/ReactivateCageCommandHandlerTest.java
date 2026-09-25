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

/** Reativação de gaiola (US3, cenários 4 e 5; FR-015, FR-016). */
@DisplayName("ReactivateCageCommandHandler")
class ReactivateCageCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-25T14:03:51Z");
    private final InMemorySectorRepository repository = new InMemorySectorRepository();
    private final ReactivateCageCommandHandler handler = new ReactivateCageCommandHandler(repository, clock);

    private Sector sector;
    private Cage b07;

    private void givenSectorWithAnInactiveB07() {
        sector = aSector().withInactiveCage("B", 7, 50).withClock(clock).build();
        repository.save(sector);
        b07 = sector.cages().getFirst();
    }

    @Test
    @DisplayName("reactivates the cage and saves the sector")
    void givenInactiveCage_whenReactivating_thenSaveItActive() {
        // given
        givenSectorWithAnInactiveB07();

        // when
        Result<CageId> result =
                handler.handle(new ReactivateCageCommand(sector.id().toString(), b07.id().toString()));

        // then
        assertThat(repository.findById(sector.id()).orElseThrow().cage(result.value()).orElseThrow().isActive())
                .isTrue();
    }

    @Test
    @DisplayName("fails as conflict when a new active cage took the battery and number")
    void givenNewActiveB07_whenReactivatingTheOldOne_thenFailAsConflict() {
        // given
        givenSectorWithAnInactiveB07();
        sector.registerCage("B", "7", "40", clock);

        // when
        Result<CageId> result =
                handler.handle(new ReactivateCageCommand(sector.id().toString(), b07.id().toString()));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("CAGE_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("fails as conflict, with the sector inactive as the reason, for a cage of an inactive sector")
    void givenInactiveSector_whenReactivatingACageAlone_thenFailAsSectorInactive() {
        // given
        // A A-01 era ativa e saiu com a cascata: é ela que a pessoa tentaria reativar sozinha.
        sector = aSector().withCage("A", 1, 50).withInactiveCage("B", 7, 50).withClock(clock).build();
        repository.save(sector);
        Cage a01 = sector.cages().getFirst();
        sector.deactivate(clock);

        // when
        Result<CageId> result =
                handler.handle(new ReactivateCageCommand(sector.id().toString(), a01.id().toString()));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_INACTIVE");
        assertThat(result.error().message())
                .isEqualTo("O setor está inativo. Reative o setor antes de mexer nas gaiolas dele.");
    }

    @Test
    @DisplayName("fails as sector not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenReactivating_thenFailAsSectorNotFound() {
        // given
        givenSectorWithAnInactiveB07();

        // when
        Result<CageId> unknown = handler.handle(
                new ReactivateCageCommand("9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44", b07.id().toString()));
        Result<CageId> malformed = handler.handle(new ReactivateCageCommand("galpao-9", b07.id().toString()));

        // then
        assertThat(unknown.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(unknown.error().code()).isEqualTo("SECTOR_NOT_FOUND");
        assertThat(malformed.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as cage not found for a cage the sector does not have, a malformed one, or one of another sector")
    void givenUnknownMalformedOrForeignCage_whenReactivating_thenFailAsCageNotFound() {
        // given
        givenSectorWithAnInactiveB07();
        Sector another = aSector().named("Poedeiras brancas — Galpão 2").withInactiveCage("B", 7, 50).withClock(clock).build();
        repository.save(another);

        // when
        Result<CageId> unknown =
                handler.handle(new ReactivateCageCommand(sector.id().toString(), CageId.generate().toString()));
        Result<CageId> malformed = handler.handle(new ReactivateCageCommand(sector.id().toString(), "b-07"));
        Result<CageId> foreign = handler.handle(
                new ReactivateCageCommand(sector.id().toString(), another.cages().getFirst().id().toString()));

        // then
        assertThat(unknown.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(unknown.error().code()).isEqualTo("CAGE_NOT_FOUND");
        assertThat(malformed.error().code()).isEqualTo("CAGE_NOT_FOUND");
        assertThat(foreign.error().code()).isEqualTo("CAGE_NOT_FOUND");
        assertThat(repository.saves()).isEqualTo(2);
    }

    @Test
    @DisplayName("saves nothing when the cage is already active")
    void givenActiveCage_whenReactivating_thenSaveNothing() {
        // given
        sector = aSector().withCage("A", 1, 50).withClock(clock).build();
        repository.save(sector);

        // when
        Result<CageId> result = handler.handle(
                new ReactivateCageCommand(sector.id().toString(), sector.cages().getFirst().id().toString()));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(repository.saves()).isEqualTo(1);
    }
}
