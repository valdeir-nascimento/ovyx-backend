package io.github.ovyx.farm.integration;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aUniqueSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.farm.application.cage.CageDetail;
import io.github.ovyx.farm.application.cage.CageDirectory;
import io.github.ovyx.farm.application.cage.CageLastWeighing;
import io.github.ovyx.farm.application.cage.CageSummary;
import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.Cage;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.SectorTestDataBuilder;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.domain.model.Weighing;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.farm.domain.port.WeighingRepository;
import io.github.ovyx.shared.application.PageResponse;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

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

    // ---------------------------------------------------------------- última pesagem (005, US3)

    @Autowired
    private WeighingRepository weighings;

    @Autowired
    private JdbcTemplate jdbc;

    private void weighed(Sector sector, CageId cage, String day, String weight) {
        weighings.save(Weighing.record(
                sector,
                cage,
                day,
                weight,
                LocalDate.of(2026, 9, 24),
                weighings,
                new Actor(UUID.randomUUID(), "Marina Alves"),
                clock.instant()));
    }

    @Test
    @DisplayName("brings the most recent valid weighing of each cage, and none for a cage never weighed")
    void givenWeighedAndUnweighedCages_whenSearching_thenBringTheLastValidWeighingOfEach() {
        // given
        Sector sector = saved(aUniqueSector().withCage("A", 1, 48).withCage("B", 7, 50));
        CageId a01 = sector.cages().get(0).id();
        weighed(sector, a01, "2026-09-17", "158");
        weighed(sector, a01, "2026-09-24", "161,4");
        weighed(sector, a01, "2026-09-20", "999");
        jdbc.update(
                "update weighing set status = 'VOIDED', voided_by_id = ?, voided_by_name = 'Marina Alves',"
                        + " voided_at = now() where cage_id = ? and weighed_on = ?",
                UUID.randomUUID(),
                a01.value(),
                LocalDate.of(2026, 9, 20));
        jdbc.update(
                "update weighing set status = 'VOIDED', voided_by_id = ?, voided_by_name = 'Marina Alves',"
                        + " voided_at = now() where cage_id = ? and weighed_on = ?",
                UUID.randomUUID(),
                a01.value(),
                LocalDate.of(2026, 9, 24));

        // when
        PageResponse<CageSummary> page = directory.search(sector.id(), null, null, StatusFilter.ACTIVE, 0, 100);

        // then
        assertThat(page.content()).extracting(CageSummary::code).containsExactly("A-01", "B-07");
        CageLastWeighing last = page.content().get(0).lastWeighing();
        assertThat(last.weighedOn()).isEqualTo(LocalDate.of(2026, 9, 17));
        assertThat(last.averageWeight()).isEqualByComparingTo("158.0");
        assertThat(page.content().get(1).lastWeighing()).isNull();
    }

    @Test
    @DisplayName("brings the most recent of several valid weighings, whatever the order they were recorded in")
    void givenSeveralValidWeighings_whenSearching_thenBringTheMostRecent() {
        // given
        Sector sector = saved(aUniqueSector().withCage("A", 1, 48));
        CageId a01 = sector.cages().get(0).id();
        weighed(sector, a01, "2026-09-17", "158");
        weighed(sector, a01, "2026-09-24", "161,4");
        weighed(sector, a01, "2026-09-10", "156");

        // when
        PageResponse<CageSummary> page = directory.search(sector.id(), null, null, StatusFilter.ACTIVE, 0, 100);

        // then
        CageLastWeighing last = page.content().get(0).lastWeighing();
        assertThat(last.weighedOn()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(last.averageWeight()).isEqualByComparingTo("161.4");
    }
}
