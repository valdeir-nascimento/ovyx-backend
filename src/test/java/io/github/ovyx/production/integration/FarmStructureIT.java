package io.github.ovyx.production.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.production.domain.model.FarmCage;
import io.github.ovyx.production.domain.model.FarmSector;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.production.domain.port.FarmStructure;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * A estrutura da granja como o production a lê (R-004): o setor, a situação dele e as gaiolas ativas,
 * pelas tabelas do farm, sem depender do código dele.
 */
@Transactional
@DisplayName("FarmStructure")
class FarmStructureIT extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private FarmStructure farmStructure;

    private UUID insertSector(String name, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into sector (id, name, status, created_at, updated_at) values (?, ?, ?, now(), now())",
                id,
                name,
                status);
        return id;
    }

    private UUID insertCage(UUID sectorId, String battery, int number, int birdCount, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into cage (id, sector_id, battery, number, bird_count, status, created_at, updated_at)"
                        + " values (?, ?, ?, ?, ?, ?, now(), now())",
                id,
                sectorId,
                battery,
                number,
                birdCount,
                status);
        return id;
    }

    @Test
    @DisplayName("reads an active sector with its active cages, by battery and number")
    void givenActiveSectorWithActiveAndInactiveCages_whenReadingIt_thenFindOnlyTheActiveCagesInOrder() {
        // given
        String name = "Codornas — Galpão " + UUID.randomUUID();
        UUID sectorId = insertSector(name, "ACTIVE");
        UUID b03 = insertCage(sectorId, "B", 3, 40, "ACTIVE");
        UUID a10 = insertCage(sectorId, "A", 10, 48, "ACTIVE");
        UUID a02 = insertCage(sectorId, "A", 2, 50, "ACTIVE");
        insertCage(sectorId, "B", 7, 50, "INACTIVE");

        // when
        Optional<FarmSector> sector = farmStructure.sectorOf(SectorId.of(sectorId));

        // then
        assertThat(sector).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(SectorId.of(sectorId));
            assertThat(found.name()).isEqualTo(name);
            assertThat(found.active()).isTrue();
            assertThat(found.activeCages())
                    .containsExactly(
                            new FarmCage(CageId.of(a02), "A", 2, 50),
                            new FarmCage(CageId.of(a10), "A", 10, 48),
                            new FarmCage(CageId.of(b03), "B", 3, 40));
        });
    }

    @Test
    @DisplayName("reads an inactive sector without active cages")
    void givenInactiveSector_whenReadingIt_thenFindItInactiveWithoutActiveCages() {
        // given
        UUID sectorId = insertSector("Poedeiras — Galpão " + UUID.randomUUID(), "INACTIVE");
        insertCage(sectorId, "A", 1, 50, "INACTIVE");

        // when
        Optional<FarmSector> sector = farmStructure.sectorOf(SectorId.of(sectorId));

        // then
        assertThat(sector).hasValueSatisfying(found -> {
            assertThat(found.active()).isFalse();
            assertThat(found.activeCages()).isEmpty();
        });
    }

    @Test
    @DisplayName("finds nothing for a sector that does not exist")
    void givenUnknownSector_whenReadingIt_thenFindNothing() {
        // given
        SectorId unknown = SectorId.of(UUID.randomUUID());

        // when
        Optional<FarmSector> sector = farmStructure.sectorOf(unknown);

        // then
        assertThat(sector).isEmpty();
    }
}
