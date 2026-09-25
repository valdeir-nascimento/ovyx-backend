package io.github.ovyx.farm.integration;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aUniqueSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.application.sector.DeactivateSectorCommand;
import io.github.ovyx.farm.application.sector.FindSectorByIdQuery;
import io.github.ovyx.farm.application.sector.ListSectorsQuery;
import io.github.ovyx.farm.application.sector.ReactivateSectorCommand;
import io.github.ovyx.farm.application.sector.SectorDetail;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.domain.FixedClock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A cascata da inativação e da reativação contra PostgreSQL real (US3; FR-012, FR-015; SC-003, SC-005;
 * S-07 e S-13).
 *
 * <p>O setor tem 4 gaiolas ativas e 1 inativa desde antes: a inativação leva as 4, a reativação traz as
 * 4 de volta, e a quinta fica como estava. E nada é apagado: as linhas continuam, com os dados de antes.
 */
@DisplayName("Sector deactivation")
class SectorDeactivationIT extends IntegrationTestSupport {

    private static final String CAGES_OF = """
            select battery, number, bird_count, status, deactivated_with_sector, created_at
              from cage
             where sector_id = ?
             order by battery, number
            """;

    @Autowired
    private Dispatcher dispatcher;

    @Autowired
    private SectorRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    private final FixedClock clock = FixedClock.at("2026-09-25T13:10:42Z");

    private Sector sectorWithFourActiveCagesAndOneInactive() {
        Sector sector = aUniqueSector()
                .withCage("A", 1, 50)
                .withCage("A", 2, 48)
                .withCage("B", 1, 50)
                .withCage("B", 2, 0)
                .withInactiveCage("B", 3, 30)
                .withClock(clock)
                .build();
        repository.save(sector);
        return sector;
    }

    private List<Map<String, Object>> cagesOf(Sector sector) {
        return jdbc.queryForList(CAGES_OF, sector.id().value());
    }

    /** O que a inativação não pode mudar numa gaiola: tudo, fora a situação, a marca e a última alteração. */
    private static List<List<Object>> dataOf(List<Map<String, Object>> cages) {
        return cages.stream()
                .map(cage -> List.of(cage.get("battery"), cage.get("number"), cage.get("bird_count"), cage.get("created_at")))
                .toList();
    }

    private SectorDetail detailOf(Sector sector) {
        return dispatcher.ask(new FindSectorByIdQuery(sector.id().toString())).value();
    }

    @Test
    @DisplayName("deactivates the sector and its four active cages, and marks only those")
    void givenSectorWithFourActiveCages_whenDeactivating_thenDeactivateAndMarkTheFour() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();

        // when
        dispatcher.dispatch(new DeactivateSectorCommand(sector.id().toString()));

        // then
        List<Map<String, Object>> cages = cagesOf(sector);
        assertThat(cages).allSatisfy(cage -> assertThat(cage).containsEntry("status", "INACTIVE"));
        assertThat(cages).filteredOn(cage -> (Boolean) cage.get("deactivated_with_sector")).hasSize(4);
        assertThat(detailOf(sector).status().name()).isEqualTo("INACTIVE");
        assertThat(detailOf(sector).activeCageCount()).isZero();
        assertThat(detailOf(sector).birdCount()).isZero();
    }

    @Test
    @DisplayName("brings back exactly the four cages on reactivation, and the totals with them")
    void givenDeactivatedSector_whenReactivating_thenBringBackTheFourCagesAndTheTotals() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        dispatcher.dispatch(new DeactivateSectorCommand(sector.id().toString()));

        // when
        dispatcher.dispatch(new ReactivateSectorCommand(sector.id().toString()));

        // then
        List<Map<String, Object>> cages = cagesOf(sector);
        assertThat(cages).filteredOn(cage -> "ACTIVE".equals(cage.get("status"))).hasSize(4);
        assertThat(cages.getLast()).containsEntry("battery", "B").containsEntry("status", "INACTIVE");
        assertThat(cages).noneMatch(cage -> (Boolean) cage.get("deactivated_with_sector"));
        assertThat(detailOf(sector).activeCageCount()).isEqualTo(4);
        assertThat(detailOf(sector).birdCount()).isEqualTo(148);
    }

    @Test
    @DisplayName("deletes nothing: every row stays, with the data it had before the deactivation")
    void givenSectorAndCages_whenDeactivatingAndReactivating_thenKeepEveryRowWithItsData() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        List<Map<String, Object>> before = cagesOf(sector);

        // when
        dispatcher.dispatch(new DeactivateSectorCommand(sector.id().toString()));
        List<Map<String, Object>> whileInactive = cagesOf(sector);
        dispatcher.dispatch(new ReactivateSectorCommand(sector.id().toString()));

        // then
        assertThat(whileInactive).hasSize(5);
        assertThat(jdbc.queryForObject("select count(*) from sector where id = ?", Integer.class, sector.id().value()))
                .isEqualTo(1);
        assertThat(dataOf(whileInactive)).isEqualTo(dataOf(before));
        assertThat(dataOf(cagesOf(sector))).isEqualTo(dataOf(before));
    }

    @Test
    @DisplayName("keeps the inactive sector consultable with its data, through the list of inactive sectors")
    void givenDeactivatedSector_whenListingTheInactive_thenFindItWithItsData() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();

        // when
        dispatcher.dispatch(new DeactivateSectorCommand(sector.id().toString()));

        // then
        assertThat(dispatcher.ask(new ListSectorsQuery(StatusFilter.INACTIVE)).value())
                .anySatisfy(listed -> assertThat(listed.name()).isEqualTo(sector.name().value()));
    }
}
