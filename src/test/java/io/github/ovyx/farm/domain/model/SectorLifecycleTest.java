package io.github.ovyx.farm.domain.model;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aSector;
import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalCodeOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalMessageOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.farm.fixtures.InMemorySectorRepository;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Inativar e reativar sem perder o histórico (US3; FR-012 a FR-016; invariante 3 e R-004 do
 * data-model.md). Sem mock: o quadro dos setores é o repositório em memória.
 */
@DisplayName("Sector lifecycle")
class SectorLifecycleTest {

    private static final String SECTOR_INACTIVE =
            "O setor está inativo. Reative o setor antes de mexer nas gaiolas dele.";

    private final FixedClock clock = FixedClock.at("2026-09-25T14:03:51Z");
    private final InMemorySectorRepository roster = new InMemorySectorRepository();

    /** Um setor com 4 gaiolas ativas e a B-03 inativada sozinha, antes. */
    private Sector sectorWithFourActiveCagesAndOneInactive() {
        Sector sector = aSector()
                .withCage("A", 1, 50)
                .withCage("A", 2, 48)
                .withCage("B", 1, 50)
                .withCage("B", 2, 0)
                .withInactiveCage("B", 3, 30)
                .withRoster(roster)
                .withClock(clock)
                .build();
        roster.save(sector);
        return sector;
    }

    private static Cage cageCoded(Sector sector, String code) {
        return sector.cages().stream().filter(cage -> cage.code().equals(code)).findFirst().orElseThrow();
    }

    // -------------------------------------------------------------------------------- setor

    @Test
    @DisplayName("deactivates the sector and, with it, each active cage, marked as deactivated with the sector")
    void givenSectorWithActiveCages_whenDeactivating_thenDeactivateTheActiveCagesWithIt() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        clock.advance(Duration.ofHours(1));

        // when
        sector.deactivate(clock);

        // then
        assertThat(sector.status()).isEqualTo(Status.INACTIVE);
        assertThat(sector.updatedAt()).isEqualTo(clock.instant());
        assertThat(sector.cages()).allSatisfy(cage -> assertThat(cage.isActive()).isFalse());
        assertThat(sector.cages())
                .filteredOn(Cage::deactivatedWithSector)
                .extracting(Cage::code)
                .containsExactlyInAnyOrder("A-01", "A-02", "B-01", "B-02");
        assertThat(cageCoded(sector, "B-03").deactivatedWithSector()).isFalse();
    }

    @Test
    @DisplayName("changes nothing when deactivating a sector that is already inactive")
    void givenInactiveSector_whenDeactivatingAgain_thenChangeNothing() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        sector.deactivate(clock);
        Instant deactivatedAt = sector.updatedAt();
        clock.advance(Duration.ofHours(1));

        // when
        boolean changed = sector.deactivate(clock);

        // then
        assertThat(changed).isFalse();
        assertThat(sector.updatedAt()).isEqualTo(deactivatedAt);
        assertThat(sector.cages()).filteredOn(Cage::deactivatedWithSector).hasSize(4);
    }

    @Test
    @DisplayName("reactivates exactly the cages its deactivation took, and clears their mark")
    void givenSectorDeactivatedWithItsCages_whenReactivating_thenBringBackExactlyThoseCages() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        sector.deactivate(clock);
        clock.advance(Duration.ofHours(1));

        // when
        sector.reactivate(roster, clock);

        // then
        assertThat(sector.isActive()).isTrue();
        assertThat(sector.cages())
                .filteredOn(Cage::isActive)
                .extracting(Cage::code)
                .containsExactlyInAnyOrder("A-01", "A-02", "B-01", "B-02");
        assertThat(cageCoded(sector, "B-03").isActive()).isFalse();
        assertThat(sector.cages()).noneMatch(Cage::deactivatedWithSector);
        assertThat(cageCoded(sector, "A-01").updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("gives the same result after two rounds of deactivation and reactivation")
    void givenTwoRoundsOfDeactivationAndReactivation_whenReactivatingTheSecondTime_thenBringBackTheSameCages() {
        // given
        // Guardar a lista no setor, ou comparar datas, quebrava na segunda volta (R-004).
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        sector.deactivate(clock);
        sector.reactivate(roster, clock);
        sector.deactivate(clock);

        // when
        sector.reactivate(roster, clock);

        // then
        assertThat(sector.cages()).filteredOn(Cage::isActive).hasSize(4);
        assertThat(cageCoded(sector, "B-03").isActive()).isFalse();
    }

    @Test
    @DisplayName("refuses to reactivate a sector whose name another active sector took")
    void givenNameTakenWhileInactive_whenReactivating_thenRefuseAsNameInUseAndStayInactive() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        sector.deactivate(clock);
        roster.save(aSector().named(sector.name().value().toUpperCase()).withRoster(roster).build());

        // when
        FarmErrorCode code = (FarmErrorCode) refusalCodeOf(() -> sector.reactivate(roster, clock));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.SECTOR_NAME_IN_USE);
        assertThat(sector.isActive()).isFalse();
        assertThat(sector.cages()).noneMatch(Cage::isActive);
    }

    @Test
    @DisplayName("changes nothing when reactivating a sector that is already active")
    void givenActiveSector_whenReactivating_thenChangeNothing() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        Instant before = sector.updatedAt();
        clock.advance(Duration.ofHours(1));

        // when
        boolean changed = sector.reactivate(roster, clock);

        // then
        assertThat(changed).isFalse();
        assertThat(sector.updatedAt()).isEqualTo(before);
        assertThat(cageCoded(sector, "B-03").isActive()).isFalse();
    }

    // -------------------------------------------------------------------------------- gaiola

    @Test
    @DisplayName("deactivates a cage on its own, not marked as deactivated with the sector")
    void givenActiveCage_whenDeactivatingIt_thenDeactivateItWithoutTheMark() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        Cage a02 = cageCoded(sector, "A-02");
        clock.advance(Duration.ofMinutes(5));

        // when
        sector.deactivateCage(a02.id(), clock);

        // then
        assertThat(a02.isActive()).isFalse();
        assertThat(a02.deactivatedWithSector()).isFalse();
        assertThat(sector.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("changes nothing when deactivating a cage that is already inactive")
    void givenInactiveCage_whenDeactivatingItAgain_thenChangeNothing() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        Cage b03 = cageCoded(sector, "B-03");
        Instant sectorBefore = sector.updatedAt();
        Instant cageBefore = b03.updatedAt();
        clock.advance(Duration.ofHours(1));

        // when
        boolean changed = sector.deactivateCage(b03.id(), clock);

        // then
        assertThat(changed).isFalse();
        assertThat(sector.updatedAt()).isEqualTo(sectorBefore);
        assertThat(b03.updatedAt()).isEqualTo(cageBefore);
    }

    @Test
    @DisplayName("changes nothing when reactivating a cage that is already active")
    void givenActiveCage_whenReactivatingIt_thenChangeNothing() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        Cage a01 = cageCoded(sector, "A-01");
        Instant sectorBefore = sector.updatedAt();
        Instant cageBefore = a01.updatedAt();
        clock.advance(Duration.ofHours(1));

        // when
        boolean changed = sector.reactivateCage(a01.id(), clock);

        // then
        assertThat(changed).isFalse();
        assertThat(sector.updatedAt()).isEqualTo(sectorBefore);
        assertThat(a01.updatedAt()).isEqualTo(cageBefore);
    }

    @Test
    @DisplayName("reactivates a cage on its own")
    void givenInactiveCage_whenReactivatingIt_thenBringItBack() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        Cage b03 = cageCoded(sector, "B-03");

        // when
        sector.reactivateCage(b03.id(), clock);

        // then
        assertThat(b03.isActive()).isTrue();
    }

    @Test
    @DisplayName("refuses to reactivate a cage whose battery and number another active cage took")
    void givenNewActiveCageWithTheSameCode_whenReactivatingTheOldOne_thenRefuseAsAlreadyExisting() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        Cage old = cageCoded(sector, "B-03");
        sector.registerCage("B", "3", "40", clock);

        // when
        Map<String, String> details = detailsOf(() -> sector.reactivateCage(old.id(), clock));

        // then
        assertThat(details).containsOnlyKeys("battery", "number");
        assertThat(refusalCodeOf(() -> sector.reactivateCage(old.id(), clock)))
                .isEqualTo(FarmErrorCode.CAGE_ALREADY_EXISTS);
        assertThat(old.isActive()).isFalse();
    }

    @Test
    @DisplayName("refuses a cage the sector does not have")
    void givenCageOfNoSector_whenDeactivatingOrReactivating_thenRefuseAsNotFound() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        CageId unknown = CageId.generate();

        // when
        FarmErrorCode onDeactivation = (FarmErrorCode) refusalCodeOf(() -> sector.deactivateCage(unknown, clock));
        FarmErrorCode onReactivation = (FarmErrorCode) refusalCodeOf(() -> sector.reactivateCage(unknown, clock));

        // then
        assertThat(onDeactivation).isEqualTo(FarmErrorCode.CAGE_NOT_FOUND);
        assertThat(onReactivation).isEqualTo(FarmErrorCode.CAGE_NOT_FOUND);
    }

    // -------------------------------------------------------------------- setor inativo

    @Test
    @DisplayName("refuses a new cage in an inactive sector")
    void givenInactiveSector_whenRegisteringACage_thenRefuseAsSectorInactive() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        sector.deactivate(clock);

        // when
        FarmErrorCode code = (FarmErrorCode) refusalCodeOf(() -> sector.registerCage("C", "1", "50", clock));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.SECTOR_INACTIVE);
        assertThat(refusalMessageOf(() -> sector.registerCage("C", "1", "50", clock))).isEqualTo(SECTOR_INACTIVE);
        assertThat(sector.cages()).hasSize(5);
    }

    @Test
    @DisplayName("refuses to update a cage of an inactive sector")
    void givenInactiveSector_whenUpdatingACage_thenRefuseAsSectorInactive() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        Cage a01 = cageCoded(sector, "A-01");
        sector.deactivate(clock);

        // when
        FarmErrorCode code = (FarmErrorCode) refusalCodeOf(() -> sector.updateCage(a01.id(), "A", "1", "40", clock));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.SECTOR_INACTIVE);
        assertThat(a01.birdCount().value()).isEqualTo(50);
    }

    @Test
    @DisplayName("refuses to reactivate a cage alone while its sector is inactive")
    void givenInactiveSector_whenReactivatingACageAlone_thenRefuseAsSectorInactive() {
        // given
        Sector sector = sectorWithFourActiveCagesAndOneInactive();
        Cage a01 = cageCoded(sector, "A-01");
        sector.deactivate(clock);

        // when
        FarmErrorCode code = (FarmErrorCode) refusalCodeOf(() -> sector.reactivateCage(a01.id(), clock));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.SECTOR_INACTIVE);
        assertThat(a01.isActive()).isFalse();
    }
}
