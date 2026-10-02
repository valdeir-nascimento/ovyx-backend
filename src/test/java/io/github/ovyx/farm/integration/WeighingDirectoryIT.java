package io.github.ovyx.farm.integration;

import java.time.DayOfWeek;
import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aUniqueSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.farm.application.weighing.WeighedCage;
import io.github.ovyx.farm.application.weighing.WeighedSector;
import io.github.ovyx.farm.application.weighing.WeighingDetail;
import io.github.ovyx.farm.application.weighing.WeighingDirectory;
import io.github.ovyx.farm.application.weighing.WeighingEntry;
import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.domain.model.Weighing;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.farm.domain.port.WeighingRepository;
import io.github.ovyx.farm.domain.valueobject.ReferenceWeight;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** A leitura das pesagens para o acompanhamento, direto do banco (R-008 da 005). */
@DisplayName("Weighing directory")
class WeighingDirectoryIT extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);
    private static final Instant NOW = Instant.parse("2026-09-24T10:12:40Z");
    private static final Actor MARINA = new Actor(UUID.fromString("5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22"), "Marina Alves");

    @Autowired
    private WeighingDirectory directory;

    @Autowired
    private WeighingRepository weighings;

    @Autowired
    private SectorRepository sectors;

    @Autowired
    private JdbcTemplate jdbc;

    private Sector saved(Sector sector) {
        sectors.save(sector);
        return sector;
    }

    private Weighing recorded(Sector sector, CageId cage, String day, String weight) {
        Weighing weighing = Weighing.record(sector, cage, day, weight, TODAY, weighings, MARINA, NOW);
        weighings.save(weighing);
        return weighing;
    }

    @Test
    @DisplayName("reads the sector and the cage the overview shows")
    void givenSectorWithCage_whenReadingThem_thenFindWhatTheOverviewShows() {
        // given
        Sector sector = saved(aUniqueSector().withCage("A", 1, 48).build());
        CageId cage = sector.cages().get(0).id();

        // when
        Optional<WeighedSector> readSector = directory.sectorOf(sector.id());
        Optional<WeighedCage> readCage = directory.cageOf(sector.id(), cage);

        // then
        assertThat(readSector).hasValueSatisfying(found -> {
            assertThat(found.name()).isEqualTo(sector.name().value());
            assertThat(found.status()).isEqualTo(Status.ACTIVE);
        });
        assertThat(readCage)
                .contains(new WeighedCage(cage, "A-01", "A", 1, 48, Status.ACTIVE));
    }

    @Test
    @DisplayName("finds no cage of another sector, and no sector that does not exist")
    void givenCageOfAnotherSector_whenReadingIt_thenFindNothing() {
        // given
        Sector sector = saved(aUniqueSector().withCage("A", 1, 48).build());
        Sector other = saved(aUniqueSector().withCage("A", 1, 48).build());

        // when
        Optional<WeighedCage> cage = directory.cageOf(sector.id(), other.cages().get(0).id());
        Optional<WeighedSector> unknown = directory.sectorOf(SectorId.generate());

        // then
        assertThat(cage).isEmpty();
        assertThat(unknown).isEmpty();
    }

    @Test
    @DisplayName("finds a valid weighing of the cage, with who recorded it and when")
    void givenRecordedWeighing_whenFindingIt_thenFindItsDetail() {
        // given
        Sector sector = saved(aUniqueSector().withCage("A", 1, 48).withCage("B", 7, 50).build());
        CageId a01 = sector.cages().get(0).id();
        Weighing weighing = recorded(sector, a01, "2026-09-24", "161,4");

        // when
        Optional<WeighingDetail> found = directory.findWeighing(a01, weighing.id());
        Optional<WeighingDetail> fromAnotherCage = directory.findWeighing(sector.cages().get(1).id(), weighing.id());

        // then
        assertThat(found).hasValueSatisfying(detail -> {
            assertThat(detail.weighedOn()).isEqualTo(TODAY);
            assertThat(detail.averageWeight()).isEqualByComparingTo("161.4");
            assertThat(detail.recordedBy()).isEqualTo(MARINA);
            assertThat(detail.recordedAt()).isEqualTo(NOW);
            assertThat(detail.lastCorrectedBy()).isNull();
        });
        assertThat(fromAnotherCage).isEmpty();
    }

    @Test
    @DisplayName("finds no voided weighing")
    void givenVoidedWeighing_whenFindingIt_thenFindNothing() {
        // given
        Sector sector = saved(aUniqueSector().withCage("A", 1, 48).build());
        CageId a01 = sector.cages().get(0).id();
        Weighing weighing = recorded(sector, a01, "2026-09-24", "999");
        jdbc.update(
                "update weighing set status = 'VOIDED', voided_by_id = ?, voided_by_name = 'Marina Alves',"
                        + " voided_at = now() where id = ?",
                MARINA.id(),
                weighing.id().value());

        // when
        Optional<WeighingDetail> found = directory.findWeighing(a01, weighing.id());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("reads only the valid weighings of that cage")
    void givenWeighingsOfTwoCagesAndAVoidedOne_whenReadingOneCage_thenFindOnlyItsValidWeighings() {
        // given
        Sector sector = saved(aUniqueSector().withCage("A", 1, 48).withCage("B", 7, 50).build());
        CageId a01 = sector.cages().get(0).id();
        CageId b07 = sector.cages().get(1).id();
        Weighing kept = recorded(sector, a01, "2026-09-24", "161,4");
        recorded(sector, b07, "2026-09-24", "158");
        Weighing voided = recorded(sector, a01, "2026-09-17", "999");
        jdbc.update(
                "update weighing set status = 'VOIDED', voided_by_id = ?, voided_by_name = 'Marina Alves',"
                        + " voided_at = now() where id = ?",
                MARINA.id(),
                voided.id().value());

        // when
        List<WeighingEntry> read = directory.weighingsOf(a01);

        // then
        assertThat(read).singleElement().satisfies(entry -> {
            assertThat(entry.id()).isEqualTo(kept.id());
            assertThat(entry.weighedOn()).isEqualTo(TODAY);
            assertThat(entry.averageWeight()).isEqualByComparingTo("161.4");
            assertThat(entry.recordedBy()).isEqualTo(MARINA);
            assertThat(entry.lastCorrectedBy()).isNull();
        });
    }

    // ---------------------------------------------------------------- acompanhamento (US3)

    @Test
    @DisplayName("reads the reference weight range of the sector, absent without range")
    void givenSectorsWithAndWithoutRange_whenReadingThem_thenFindTheRangeOnlyWhereItExists() {
        // given
        Sector withRange = saved(aUniqueSector().withReferenceWeight(155, 175).withCage("A", 1, 48).build());
        Sector withoutRange = saved(aUniqueSector().withCage("A", 1, 48).build());

        // when
        WeighedSector read = directory.sectorOf(withRange.id()).orElseThrow();
        WeighedSector readWithout = directory.sectorOf(withoutRange.id()).orElseThrow();

        // then
        assertThat(read.referenceWeight()).isEqualTo(new ReferenceWeight(155, 175));
        assertThat(readWithout.referenceWeight()).isNull();
    }

    // ---------------------------------------------------------------- agenda de pesagem (010)

    @Test
    @DisplayName("reads the weighing day of the sector of the weighed cage, and none for a sector without it")
    void givenSectorsWithAndWithoutWeighingDay_whenReadingTheSector_thenFindTheDayOnlyWhereItExists() {
        // given
        Sector fridays = saved(aUniqueSector().withWeighingDay("FRIDAY").withCage("A", 1, 48).build());
        Sector everySevenDays = saved(aUniqueSector().withCage("A", 1, 48).build());

        // when
        WeighedSector ofFridays = directory.sectorOf(fridays.id()).orElseThrow();
        WeighedSector ofEverySevenDays = directory.sectorOf(everySevenDays.id()).orElseThrow();

        // then
        assertThat(ofFridays.weighingDay()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(ofEverySevenDays.weighingDay()).isNull();
    }
}
