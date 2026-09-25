package io.github.ovyx.farm.application.sector;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.fixtures.InMemorySectorRepository;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Reativação de setor, com as gaiolas que a inativação dele levou (US3, cenários 5 e 6; FR-015, FR-016). */
@DisplayName("ReactivateSectorCommandHandler")
class ReactivateSectorCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-25T14:03:51Z");
    private final InMemorySectorRepository repository = new InMemorySectorRepository();
    private final ReactivateSectorCommandHandler handler = new ReactivateSectorCommandHandler(repository, clock);

    private Sector deactivated(String name) {
        Sector sector = aSector().named(name).withCage("A", 1, 50).withRoster(repository).withClock(clock).build();
        sector.deactivate(clock);
        repository.save(sector);
        return sector;
    }

    @Test
    @DisplayName("reactivates the sector with its cages and saves it")
    void givenDeactivatedSector_whenReactivating_thenSaveItActiveWithItsCages() {
        // given
        Sector sector = deactivated("Codornas — Galpão 1");

        // when
        Result<SectorId> result = handler.handle(new ReactivateSectorCommand(sector.id().toString()));

        // then
        Sector saved = repository.findById(result.value()).orElseThrow();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.cages()).allMatch(cage -> cage.isActive());
    }

    @Test
    @DisplayName("fails as conflict when another active sector took the name")
    void givenNameTakenWhileInactive_whenReactivating_thenFailAsConflict() {
        // given
        Sector sector = deactivated("Codornas — Galpão 1");
        repository.save(aSector().named("Codornas — Galpão 1").withRoster(repository).withClock(clock).build());

        // when
        Result<SectorId> result = handler.handle(new ReactivateSectorCommand(sector.id().toString()));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_NAME_IN_USE");
        assertThat(repository.saves()).isEqualTo(2);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44", "galpao-9"})
    @DisplayName("fails as not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenReactivating_thenFailAsSectorNotFound(String sectorId) {
        // given
        ReactivateSectorCommand command = new ReactivateSectorCommand(sectorId);

        // when
        Result<SectorId> result = handler.handle(command);

        // then
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @Test
    @DisplayName("saves nothing when the sector is already active")
    void givenActiveSector_whenReactivating_thenSaveNothing() {
        // given
        Sector sector = aSector().withRoster(repository).withClock(clock).build();
        repository.save(sector);

        // when
        Result<SectorId> result = handler.handle(new ReactivateSectorCommand(sector.id().toString()));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(repository.saves()).isEqualTo(1);
    }
}
