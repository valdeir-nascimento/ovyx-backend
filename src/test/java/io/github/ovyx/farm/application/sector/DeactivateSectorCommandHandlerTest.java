package io.github.ovyx.farm.application.sector;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.fixtures.InMemorySectorRepository;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Inativação de setor, com as gaiolas ativas dele (US3, cenário 3; FR-015). */
@DisplayName("DeactivateSectorCommandHandler")
class DeactivateSectorCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-25T14:03:51Z");
    private final InMemorySectorRepository repository = new InMemorySectorRepository();
    private final DeactivateSectorCommandHandler handler = new DeactivateSectorCommandHandler(repository, clock);

    @Test
    @DisplayName("deactivates the sector with its active cages and saves it")
    void givenSectorWithActiveCages_whenDeactivating_thenSaveItInactiveWithItsCages() {
        // given
        Sector sector = aSector().withCage("A", 1, 50).withCage("B", 7, 50).withClock(clock).build();
        repository.save(sector);

        // when
        Result<SectorId> result = handler.handle(new DeactivateSectorCommand(sector.id().toString()));

        // then
        Sector saved = repository.findById(result.value()).orElseThrow();
        assertThat(saved.status()).isEqualTo(Status.INACTIVE);
        assertThat(saved.cages()).noneMatch(cage -> cage.isActive());
        assertThat(repository.saves()).isEqualTo(2);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44", "galpao-9"})
    @DisplayName("fails as not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenDeactivating_thenFailAsSectorNotFound(String sectorId) {
        // given
        DeactivateSectorCommand command = new DeactivateSectorCommand(sectorId);

        // when
        Result<SectorId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @Test
    @DisplayName("saves nothing when the sector is already inactive")
    void givenInactiveSector_whenDeactivating_thenSaveNothing() {
        // given
        Sector sector = aSector().inactive().withClock(clock).build();
        repository.save(sector);

        // when
        Result<SectorId> result = handler.handle(new DeactivateSectorCommand(sector.id().toString()));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(repository.saves()).isEqualTo(1);
    }
}
