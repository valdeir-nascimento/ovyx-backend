package io.github.ovyx.farm.integration;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aSector;
import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aUniqueSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.application.sector.SectorDetail;
import io.github.ovyx.farm.application.sector.SectorDirectory;
import io.github.ovyx.farm.application.sector.SectorSummary;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.shared.domain.FixedClock;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A leitura dos setores, direto do banco (R-006; FR-004, FR-005).
 *
 * <p>O banco é compartilhado com os outros testes: cada caso olha só os setores que ele mesmo criou,
 * e confere que os que não deviam aparecer não aparecem.
 */
@DisplayName("Sector directory")
class SectorDirectoryIT extends IntegrationTestSupport {

    @Autowired
    private SectorDirectory directory;

    @Autowired
    private SectorRepository repository;

    /** Instantes inteiros: o banco guarda microssegundos, e o relógio do sistema pode ter mais. */
    private final FixedClock clock = FixedClock.at("2026-09-25T13:02:11Z");

    private Sector saved(Sector sector) {
        repository.save(sector);
        return sector;
    }

    private Sector active(String name) {
        return saved(aSector().named(name).withRoster(repository).withClock(clock).build());
    }

    private Sector inactive() {
        return saved(aUniqueSector().inactive().withClock(clock).build());
    }

    private static List<SectorId> idsAmong(List<SectorSummary> listed, Sector... mine) {
        Set<SectorId> wanted = Arrays.stream(mine).map(Sector::id).collect(Collectors.toSet());
        return listed.stream().map(SectorSummary::id).filter(wanted::contains).toList();
    }

    @Test
    @DisplayName("lists the active sectors by name, whatever the case, and leaves the inactive out")
    void givenActiveAndInactiveSectors_whenListingTheActive_thenFindTheActiveByName() {
        // given
        String prefix = "Ordem " + UUID.randomUUID().toString().substring(0, 8);
        Sector zeta = active(prefix + " — Zeta");
        Sector alfa = active(prefix + " — alfa");
        Sector beta = active(prefix + " — Beta");
        Sector inactive = inactive();

        // when
        List<SectorSummary> listed = directory.list(StatusFilter.ACTIVE);

        // then
        assertThat(idsAmong(listed, zeta, alfa, beta, inactive)).containsExactly(alfa.id(), beta.id(), zeta.id());
    }

    @Test
    @DisplayName("lists only the inactive sectors when asked")
    void givenActiveAndInactiveSectors_whenListingTheInactive_thenFindOnlyTheInactive() {
        // given
        Sector active = saved(aUniqueSector().withRoster(repository).withClock(clock).build());
        Sector inactive = inactive();

        // when
        List<SectorSummary> listed = directory.list(StatusFilter.INACTIVE);

        // then
        assertThat(idsAmong(listed, active, inactive)).containsExactly(inactive.id());
        assertThat(listed).allSatisfy(summary -> assertThat(summary.status()).isEqualTo(Status.INACTIVE));
    }

    @Test
    @DisplayName("lists every sector when asked for all")
    void givenActiveAndInactiveSectors_whenListingAll_thenFindBoth() {
        // given
        Sector active = saved(aUniqueSector().withRoster(repository).withClock(clock).build());
        Sector inactive = inactive();

        // when
        List<SectorSummary> listed = directory.list(StatusFilter.ALL);

        // then
        assertThat(idsAmong(listed, active, inactive)).containsExactlyInAnyOrder(active.id(), inactive.id());
    }

    @Test
    @DisplayName("shows a sector without cages with zero cages and zero birds")
    void givenSectorWithoutCages_whenListing_thenFindZeroCagesAndZeroBirds() {
        // given
        Sector sector = saved(aUniqueSector()
                .describedAs("Codornas japonesas em postura")
                .withRoster(repository)
                .withClock(clock)
                .build());

        // when
        SectorSummary summary = directory.list(StatusFilter.ACTIVE).stream()
                .filter(listed -> listed.id().equals(sector.id()))
                .findFirst()
                .orElseThrow();

        // then
        assertThat(summary.name()).isEqualTo(sector.name().value());
        assertThat(summary.description()).isEqualTo("Codornas japonesas em postura");
        assertThat(summary.activeCageCount()).isZero();
        assertThat(summary.birdCount()).isZero();
    }

    @Test
    @DisplayName("finds the detail of a sector with the instants of creation and of the last change")
    void givenSavedSector_whenFindingTheDetail_thenFindTheInstants() {
        // given
        Sector sector = saved(aUniqueSector().withRoster(repository).withClock(clock).build());

        // when
        SectorDetail detail = directory.findDetail(sector.id()).orElseThrow();

        // then
        assertThat(detail.createdAt()).isEqualTo(sector.createdAt());
        assertThat(detail.updatedAt()).isEqualTo(sector.updatedAt());
        assertThat(detail.status()).isEqualTo(Status.ACTIVE);
        assertThat(detail.activeCageCount()).isZero();
    }

    @Test
    @DisplayName("finds no detail for an identifier of no sector")
    void givenIdentifierOfNoSector_whenFindingTheDetail_thenFindNothing() {
        // given
        SectorId unknown = SectorId.generate();

        // when
        Optional<SectorDetail> detail = directory.findDetail(unknown);

        // then
        assertThat(detail).isEmpty();
    }

    @Test
    @DisplayName("counts only the active cages of the sector, and their birds")
    void givenSectorWithActiveAndInactiveCages_whenListing_thenCountOnlyTheActiveOnes() {
        // given
        Sector sector = saved(aUniqueSector()
                .withCage("A", 1, 50)
                .withCage("A", 2, 48)
                .withInactiveCage("B", 3, 30)
                .withCage("C", 1, 0)
                .withClock(clock)
                .build());
        saved(aUniqueSector().withCage("A", 1, 500).withClock(clock).build());

        // when
        SectorSummary summary = directory.list(StatusFilter.ACTIVE).stream()
                .filter(listed -> listed.id().equals(sector.id()))
                .findFirst()
                .orElseThrow();

        // then
        assertThat(summary.activeCageCount()).isEqualTo(3);
        assertThat(summary.birdCount()).isEqualTo(98);
    }

    @Test
    @DisplayName("tells the batteries its cages use, active or not, in order and without repeating")
    void givenSectorWithCagesInThreeBatteries_whenFindingTheDetail_thenFindTheBatteriesInOrder() {
        // given
        Sector sector = saved(aUniqueSector()
                .withCage("C", 1, 10)
                .withCage("A", 1, 10)
                .withCage("A", 2, 10)
                .withInactiveCage("B", 1, 10)
                .withClock(clock)
                .build());

        // when
        SectorDetail detail = directory.findDetail(sector.id()).orElseThrow();

        // then
        assertThat(detail.batteries()).containsExactly("A", "B", "C");
        assertThat(detail.activeCageCount()).isEqualTo(3);
        assertThat(detail.birdCount()).isEqualTo(30);
    }

    @Test
    @DisplayName("tells no battery for a sector without cages")
    void givenSectorWithoutCages_whenFindingTheDetail_thenFindNoBattery() {
        // given
        Sector sector = saved(aUniqueSector().withRoster(repository).withClock(clock).build());

        // when
        SectorDetail detail = directory.findDetail(sector.id()).orElseThrow();

        // then
        assertThat(detail.batteries()).isEmpty();
    }
}
