package io.github.ovyx.production.integration;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.MARINA;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.aDailyReport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.production.domain.model.DailyReport;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.FarmSector;
import io.github.ovyx.production.domain.model.ReportCage;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.production.domain.port.DailyReportRepository;
import io.github.ovyx.production.domain.port.FarmStructure;
import io.github.ovyx.production.domain.valueobject.MortalityEntry;
import io.github.ovyx.production.domain.valueobject.MortalityNote;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * O repositório de relatórios contra PostgreSQL real (R-003, R-005).
 *
 * <p>Cada gravação e cada leitura passam pelo adaptador, em transações separadas: o que volta vem do
 * banco, e não de um cache do ORM.
 */
@DisplayName("Daily report repository")
class DailyReportRepositoryIT extends IntegrationTestSupport {

    @Autowired
    private DailyReportRepository repository;

    @Autowired
    private FarmStructure farmStructure;

    @Autowired
    private JdbcTemplate jdbc;

    private ProductionFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new ProductionFixtures(jdbc);
    }

    private FarmSector sectorWithTwoCages() {
        return farmStructure.sectorOf(SectorId.of(fixtures.sectorWithTwoCages())).orElseThrow();
    }

    @Test
    @DisplayName("saves the report with its cages and reads it back as it was")
    void givenOpenedReport_whenSavingAndReadingBack_thenFindTheSameData() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = aDailyReport().in(sector).withRoster(repository).build();

        // when
        repository.save(report);

        // then
        DailyReport read = repository.findById(sector.id(), report.id()).orElseThrow();
        assertThat(read.collectionDate()).isEqualTo(report.collectionDate());
        assertThat(read.collectionTime()).isEqualTo(report.collectionTime());
        assertThat(read.openingBirdCount()).isEqualTo(report.openingBirdCount());
        assertThat(read.flockAge()).isEqualTo(report.flockAge());
        assertThat(read.note()).isEqualTo(report.note());
        assertThat(read.openedBy()).isEqualTo(MARINA);
        assertThat(read.openedAt()).isEqualTo(report.openedAt());
        assertThat(read.cages())
                .extracting(ReportCage::battery, ReportCage::number, ReportCage::birdCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("A", 1, 48),
                        org.assertj.core.groups.Tuple.tuple("B", 7, 50));
    }

    @Test
    @DisplayName("stores the mortality of a cage and the confirmation of the day, and reads them back")
    void givenMortalityAndConfirmation_whenSavingAndReadingBack_thenFindThem() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = aDailyReport().in(sector).withRoster(repository).build();
        repository.save(report);
        DailyReport loaded = repository.findById(sector.id(), report.id()).orElseThrow();
        CageId first = loaded.cages().get(0).cageId();
        loaded.recordMortality(sector, first, "0", "0", "Nada fora do comum.", JOAO, Instant.parse("2026-09-24T11:05:12Z"));
        loaded.confirmNoMortality(sector, JOAO, Instant.parse("2026-09-24T11:06:00Z"));

        // when
        repository.save(loaded);

        // then
        DailyReport read = repository.findById(sector.id(), report.id()).orElseThrow();
        assertThat(read.noMortalityConfirmed()).isTrue();
        assertThat(read.cage(first).flatMap(ReportCage::mortality))
                .contains(new MortalityEntry(0, 0, new MortalityNote("Nada fora do comum.")));
        assertThat(read.cage(first).map(ReportCage::birdCount)).contains(48);
    }

    @Test
    @DisplayName("stores the date and the time of the collection as they are, without time zone conversion")
    void givenReportCollectedAtHalfPastSix_whenSaving_thenStoreTheLocalDateAndTime() {
        // given
        // O relatório é do dia e da hora da granja (R-006). Com o fuso do JDBC em UTC, a hora local era
        // gravada deslocada pelo fuso da JVM: 06:30 virava 09:30 no banco, e a leitura por JDBC a mostrava assim.
        DailyReport report = aDailyReport().in(sectorWithTwoCages()).withRoster(repository).build();

        // when
        repository.save(report);

        // then
        assertThat(jdbc.queryForMap(
                        "select collection_date::text as day, collection_time::text as hour from daily_report where id = ?",
                        report.id().value()))
                .containsEntry("day", "2026-09-24")
                .containsEntry("hour", "06:30:00");
    }

    @Test
    @DisplayName("finds nothing for a report read through another sector")
    void givenSavedReport_whenReadingItThroughAnotherSector_thenFindNothing() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = aDailyReport().in(sector).withRoster(repository).build();
        repository.save(report);

        // when / then
        assertThat(repository.findById(SectorId.of(UUID.randomUUID()), report.id())).isEmpty();
    }

    @Test
    @DisplayName("starts the version at zero")
    void givenSavedReport_whenReadingTheVersion_thenFindZero() {
        // given
        DailyReport report = aDailyReport().in(sectorWithTwoCages()).withRoster(repository).build();

        // when
        repository.save(report);

        // then
        assertThat(jdbc.queryForObject("select version from daily_report where id = ?", Long.class, report.id().value()))
                .isZero();
    }

    @Test
    @DisplayName("finds the date of another report of the sector, and ignores the report itself and other sectors")
    void givenSavedReport_whenAskingForItsDate_thenFindItOnlyForAnotherReportOfTheSameSector() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = aDailyReport().in(sector).withRoster(repository).build();
        repository.save(report);
        LocalDate date = report.collectionDate().value();

        // when
        boolean forAnother = repository.anotherReportOn(sector.id(), date, DailyReportId.generate());
        boolean forItself = repository.anotherReportOn(sector.id(), date, report.id());
        boolean forAnotherSector =
                repository.anotherReportOn(SectorId.of(UUID.randomUUID()), date, DailyReportId.generate());

        // then
        assertThat(forAnother).isTrue();
        assertThat(forItself).isFalse();
        assertThat(forAnotherSector).isFalse();
    }

    @Test
    @DisplayName("refuses in the database a second report of the sector on the same date")
    void givenSavedReport_whenSavingAnotherOnTheSameDateDirectly_thenFailWithUniqueViolation() {
        // given
        FarmSector sector = sectorWithTwoCages();
        repository.save(aDailyReport().in(sector).build());
        DailyReport duplicate = aDailyReport().in(sector).build();

        // when
        ThrowingCallable saving = () -> repository.save(duplicate);

        // then
        assertThatThrownBy(saving).isInstanceOf(DataIntegrityViolationException.class);
    }
}
