package io.github.ovyx.farm.integration;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aUniqueSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.application.cage.CageDetail;
import io.github.ovyx.farm.application.cage.CageDirectory;
import io.github.ovyx.farm.application.cage.CageSummary;
import io.github.ovyx.farm.domain.model.Cage;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.SectorTestDataBuilder;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.shared.application.PageResponse;
import io.github.ovyx.shared.domain.FixedClock;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A pesquisa das gaiolas de um setor, direto do banco (R-006; FR-008, FR-010, FR-011; S-05).
 *
 * <p>Cada busca tem gaiolas que não devem aparecer, e sempre há um segundo setor com gaiolas de mesmo
 * código: a pesquisa nunca pode atravessar o setor.
 */
@DisplayName("Cage directory")
class CageDirectoryIT extends IntegrationTestSupport {

    @Autowired
    private CageDirectory directory;

    @Autowired
    private SectorRepository repository;

    /** Instantes inteiros: o banco guarda microssegundos, e o relógio do sistema pode ter mais. */
    private final FixedClock clock = FixedClock.at("2026-09-25T13:10:42Z");

    private Sector saved(SectorTestDataBuilder builder) {
        Sector sector = builder.withClock(clock).build();
        repository.save(sector);
        return sector;
    }

    /** Um setor com A-01 a A-10, B-01 a B-10, A-107 e a B-07 inativa desde antes da B-07 ativa. */
    private Sector sectorWithTwoBatteries() {
        SectorTestDataBuilder builder = aUniqueSector().withInactiveCage("B", 7, 30).withCage("A", 107, 10);
        for (int number = 1; number <= 10; number++) {
            builder.withCage("A", number, 50).withCage("B", number, 40);
        }
        return saved(builder);
    }

    private List<String> codesOf(PageResponse<CageSummary> page) {
        return page.content().stream().map(CageSummary::code).toList();
    }

    @ParameterizedTest(name = "\"{0}\"")
    @CsvSource(
            delimiter = '|',
            value = {
                "b-0  | B-01,B-02,B-03,B-04,B-05,B-06,B-07,B-08,B-09",
                "07   | A-07,B-07,A-107",
                "B-07 | B-07",
                "b-7  | B-07",
                "B%   | ''"
            })
    @DisplayName("finds the cages by a piece of the code, whatever the case, and only them")
    void givenCagesOfTwoBatteries_whenSearchingByAPieceOfTheCode_thenFindOnlyTheMatchingOnes(
            String code, String expected) {
        // given
        Sector sector = sectorWithTwoBatteries();
        saved(aUniqueSector().withCage("B", 7, 50).withCage("A", 7, 50));

        // when
        PageResponse<CageSummary> page = directory.search(sector.id(), code, null, StatusFilter.ACTIVE, 0, 100);

        // then
        List<String> codes = expected.isEmpty() ? List.of() : List.of(expected.split(","));
        assertThat(codesOf(page)).containsExactlyInAnyOrderElementsOf(codes);
        assertThat(page.content()).allSatisfy(cage -> assertThat(cage.sectorId()).isEqualTo(sector.id()));
    }

    @Test
    @DisplayName("filters by battery, ordered by battery and number")
    void givenCagesOfTwoBatteries_whenFilteringByBattery_thenFindOnlyThatBatteryInOrder() {
        // given
        Sector sector = sectorWithTwoBatteries();

        // when
        PageResponse<CageSummary> page = directory.search(sector.id(), null, "B", StatusFilter.ACTIVE, 0, 100);

        // then
        assertThat(codesOf(page))
                .containsExactly("B-01", "B-02", "B-03", "B-04", "B-05", "B-06", "B-07", "B-08", "B-09", "B-10");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"ACTIVE, 21, 1", "INACTIVE, 1, 0", "ALL, 22, 1"})
    @DisplayName("filters by status")
    void givenActiveAndInactiveCages_whenFilteringByStatus_thenFindOnlyThatStatus(
            StatusFilter status, int total, int activeB07) {
        // given
        Sector sector = sectorWithTwoBatteries();

        // when
        PageResponse<CageSummary> page = directory.search(sector.id(), "B-07", null, status, 0, 100);
        PageResponse<CageSummary> all = directory.search(sector.id(), null, null, status, 0, 100);

        // then
        assertThat(all.metadata().totalElements()).isEqualTo(total);
        assertThat(page.content()).filteredOn(cage -> cage.status() == Status.ACTIVE).hasSize(activeB07);
    }

    @Test
    @DisplayName("orders by battery and number, and pages them: 21 to 30 of 30")
    void givenThirtyCages_whenReadingTheSecondPageOf20_thenFindTheLastTen() {
        // given
        SectorTestDataBuilder builder = aUniqueSector();
        for (int number = 15; number >= 1; number--) {
            builder.withCage("B", number, 40).withCage("A", number, 50);
        }
        Sector sector = saved(builder);

        // when
        PageResponse<CageSummary> first = directory.search(sector.id(), null, null, StatusFilter.ACTIVE, 0, 20);
        PageResponse<CageSummary> second = directory.search(sector.id(), null, null, StatusFilter.ACTIVE, 1, 20);

        // then
        assertThat(first.metadata().totalElements()).isEqualTo(30);
        assertThat(first.metadata().totalPages()).isEqualTo(2);
        assertThat(codesOf(first)).startsWith("A-01", "A-02").endsWith("B-05");
        assertThat(codesOf(second)).hasSize(10).startsWith("B-06").endsWith("B-15");
    }

    @Test
    @DisplayName("finds the detail of a cage of the sector, with its code and instants")
    void givenCageOfTheSector_whenFindingTheDetail_thenFindItsData() {
        // given
        Sector sector = saved(aUniqueSector().withCage("C", 120, 48));
        Cage cage = sector.cages().getFirst();

        // when
        CageDetail detail = directory.findDetail(sector.id(), cage.id()).orElseThrow();

        // then
        assertThat(detail.code()).isEqualTo("C-120");
        assertThat(detail.battery()).isEqualTo("C");
        assertThat(detail.number()).isEqualTo(120);
        assertThat(detail.birdCount()).isEqualTo(48);
        assertThat(detail.status()).isEqualTo(Status.ACTIVE);
        assertThat(detail.createdAt()).isEqualTo(clock.instant());
        assertThat(detail.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("finds no detail for a cage through another sector")
    void givenCageOfOneSector_whenFindingItThroughAnotherSector_thenFindNothing() {
        // given
        Sector sector = saved(aUniqueSector().withCage("B", 7, 50));
        Sector another = saved(aUniqueSector().withCage("B", 7, 50));

        // when
        boolean found = directory.findDetail(another.id(), sector.cages().getFirst().id()).isPresent();

        // then
        assertThat(found).isFalse();
    }

    @Test
    @DisplayName("tells whether a sector exists")
    void givenSavedSector_whenAskingWhetherItExists_thenFindIt() {
        // given
        Sector sector = saved(aUniqueSector().withCage("B", 7, 50));

        // when
        boolean exists = directory.sectorExists(sector.id());
        boolean unknown = directory.sectorExists(SectorId.generate());

        // then
        assertThat(exists).isTrue();
        assertThat(unknown).isFalse();
    }
}
