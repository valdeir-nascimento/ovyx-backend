package io.github.ovyx.production.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.IntegrationTestSupport;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * As restrições das tabelas {@code daily_report} e {@code report_cage} (R-005, R-012; data-model.md,
 * seção Tabelas).
 *
 * <p>O banco repete as regras do domínio como defesa, e não como fonte delas. Cada restrição tem um
 * caso de recusa e um de aceite no limite, para que uma faixa trocada no SQL apareça.
 *
 * <p>Transacional: cada caso é desfeito ao fim, e as listas conferidas por outros testes, no mesmo
 * banco, não recebem as linhas daqui.
 */
@Transactional
@DisplayName("Production schema")
class ProductionSchemaIT extends IntegrationTestSupport {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 24);

    @Autowired
    private JdbcTemplate jdbc;

    private UUID insertSector() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into sector (id, name, status, created_at, updated_at) values (?, ?, 'ACTIVE', now(), now())",
                id,
                "Galpão " + id);
        return id;
    }

    private UUID insertCage(UUID sectorId, int number) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into cage (id, sector_id, battery, number, bird_count, status, created_at, updated_at)"
                        + " values (?, ?, 'A', ?, 50, 'ACTIVE', now(), now())",
                id,
                sectorId,
                number);
        return id;
    }

    private UUID insertReport(UUID sectorId, LocalDate date, int openingBirdCount, int flockAge) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into daily_report (id, sector_id, collection_date, collection_time, opening_bird_count,"
                        + " flock_age, opened_by_id, opened_by_name, opened_at)"
                        + " values (?, ?, ?, time '06:30', ?, ?, ?, 'Marina Alves', now())",
                id,
                sectorId,
                date,
                openingBirdCount,
                flockAge,
                UUID.randomUUID());
        return id;
    }

    private UUID insertReport(UUID sectorId) {
        return insertReport(sectorId, DAY, 98, 20);
    }

    /** Uma gaiola no relatório, com a produção e a mortalidade dadas; nulo é "não lançada". */
    private void insertReportCage(
            UUID reportId,
            UUID cageId,
            int birdCount,
            Integer eggs,
            List<Integer> grades,
            Integer deaths,
            Integer culls) {
        jdbc.update(
                "insert into report_cage (report_id, cage_id, battery, number, bird_count, eggs, small, jumbo,"
                        + " dirty, cracked, blood_spot, abnormal, deaths, culls)"
                        + " values (?, ?, 'A', 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                reportId,
                cageId,
                birdCount,
                eggs,
                grades.get(0),
                grades.get(1),
                grades.get(2),
                grades.get(3),
                grades.get(4),
                grades.get(5),
                deaths,
                culls);
    }

    private static List<Integer> noGrades() {
        return Arrays.asList(null, null, null, null, null, null);
    }

    private static List<Integer> zeroGrades() {
        return List.of(0, 0, 0, 0, 0, 0);
    }

    @Test
    @DisplayName("rejects a report of a sector that does not exist")
    void givenUnknownSector_whenInsertingReport_thenRejectIt() {
        // given
        UUID unknownSector = UUID.randomUUID();

        // when
        ThrowingCallable insertion = () -> insertReport(unknownSector);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "{0} birds")
    @ValueSource(ints = {0, 1_000_001})
    @DisplayName("rejects an opening bird count outside 1 to 1,000,000")
    void givenOpeningBirdCountOutOfRange_whenInsertingReport_thenRejectIt(int birds) {
        // given
        UUID sectorId = insertSector();

        // when
        ThrowingCallable insertion = () -> insertReport(sectorId, DAY, birds, 20);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "{0} birds")
    @ValueSource(ints = {1, 1_000_000})
    @DisplayName("accepts an opening bird count at the limits")
    void givenOpeningBirdCountAtTheLimits_whenInsertingReport_thenAcceptIt(int birds) {
        // given
        UUID sectorId = insertSector();

        // when
        UUID id = insertReport(sectorId, DAY, birds, 20);

        // then
        assertThat(jdbc.queryForObject("select opening_bird_count from daily_report where id = ?", Integer.class, id))
                .isEqualTo(birds);
    }

    @ParameterizedTest(name = "{0} weeks")
    @ValueSource(ints = {0, 151})
    @DisplayName("rejects a flock age outside 1 to 150 weeks")
    void givenFlockAgeOutOfRange_whenInsertingReport_thenRejectIt(int weeks) {
        // given
        UUID sectorId = insertSector();

        // when
        ThrowingCallable insertion = () -> insertReport(sectorId, DAY, 98, weeks);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("starts a report unconfirmed and at version zero")
    void givenReportInsertedWithoutConfirmationAndVersion_whenReadingIt_thenFindFalseAndZero() {
        // given
        UUID id = insertReport(insertSector());

        // when
        Boolean confirmed = jdbc.queryForObject(
                "select no_mortality_confirmed from daily_report where id = ?", Boolean.class, id);
        Long version = jdbc.queryForObject("select version from daily_report where id = ?", Long.class, id);

        // then
        assertThat(confirmed).isFalse();
        assertThat(version).isZero();
    }

    @Test
    @DisplayName("rejects a second report of the same sector on the same date")
    void givenReportOfTheSectorOnTheDate_whenInsertingAnotherOnTheSameDate_thenRejectIt() {
        // given
        UUID sectorId = insertSector();
        insertReport(sectorId);

        // when
        ThrowingCallable insertion = () -> insertReport(sectorId);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("accepts reports of different sectors on the same date")
    void givenReportOfOneSectorOnTheDate_whenInsertingAnotherSectorOnTheSameDate_thenAcceptIt() {
        // given
        insertReport(insertSector());

        // when
        UUID id = insertReport(insertSector());

        // then
        assertThat(jdbc.queryForObject("select count(*) from daily_report where id = ?", Integer.class, id))
                .isOne();
    }

    @Test
    @DisplayName("serves the list by the unique index of sector and date, with no second index on the same columns")
    void givenMigratedDatabase_whenReadingTheIndexes_thenFindOnlyTheUniqueIndexOfSectorAndDate() {
        // given
        String table = "daily_report";

        // when
        List<String> definitions = jdbc.queryForList(
                "select indexdef from pg_indexes where tablename = ?", String.class, table);

        // then
        // A lista vem do dia mais recente: o PostgreSQL percorre o índice único ao contrário, e um segundo
        // índice nas mesmas colunas só custaria escrita (revisão da T110).
        assertThat(definitions)
                .filteredOn(definition -> definition.contains("(sector_id, collection_date"))
                .singleElement()
                .satisfies(definition -> assertThat(definition).startsWith("CREATE UNIQUE INDEX"));
    }

    @Test
    @DisplayName("rejects the same cage twice in a report")
    void givenCageInTheReport_whenInsertingItAgain_thenRejectIt() {
        // given
        UUID sectorId = insertSector();
        UUID reportId = insertReport(sectorId);
        UUID cageId = insertCage(sectorId, 1);
        insertReportCage(reportId, cageId, 50, null, noGrades(), null, null);

        // when
        ThrowingCallable insertion = () -> insertReportCage(reportId, cageId, 50, null, noGrades(), null, null);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("rejects a report cage that is not a cage of the farm")
    void givenUnknownCage_whenInsertingItInTheReport_thenRejectIt() {
        // given
        UUID reportId = insertReport(insertSector());

        // when
        ThrowingCallable insertion =
                () -> insertReportCage(reportId, UUID.randomUUID(), 50, null, noGrades(), null, null);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "{0} birds")
    @ValueSource(ints = {-1, 1001})
    @DisplayName("rejects a report cage bird count outside 0 to 1,000")
    void givenReportCageBirdCountOutOfRange_whenInserting_thenRejectIt(int birds) {
        // given
        UUID sectorId = insertSector();
        UUID reportId = insertReport(sectorId);
        UUID cageId = insertCage(sectorId, 1);

        // when
        ThrowingCallable insertion = () -> insertReportCage(reportId, cageId, birds, null, noGrades(), null, null);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("rejects collected eggs with a grade left empty")
    void givenEggsWithAnEmptyGrade_whenInserting_thenRejectIt() {
        // given
        UUID sectorId = insertSector();
        UUID reportId = insertReport(sectorId);
        UUID cageId = insertCage(sectorId, 1);
        List<Integer> grades = Arrays.asList(0, 0, 0, null, 0, 0);

        // when
        ThrowingCallable insertion = () -> insertReportCage(reportId, cageId, 50, 45, grades, null, null);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("rejects grades that add up to more than the collected eggs")
    void givenGradesAboveTheEggs_whenInserting_thenRejectIt() {
        // given
        UUID sectorId = insertSector();
        UUID reportId = insertReport(sectorId);
        UUID cageId = insertCage(sectorId, 1);

        // when
        ThrowingCallable insertion =
                () -> insertReportCage(reportId, cageId, 50, 30, List.of(10, 10, 10, 2, 1, 1), null, null);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("rejects more than 1,000 collected eggs in a cage")
    void givenEggsAboveOneThousand_whenInserting_thenRejectIt() {
        // given
        UUID sectorId = insertSector();
        UUID reportId = insertReport(sectorId);
        UUID cageId = insertCage(sectorId, 1);

        // when
        ThrowingCallable insertion = () -> insertReportCage(reportId, cageId, 50, 1001, zeroGrades(), null, null);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("accepts a cage with nothing recorded, and one with grades adding up to the eggs")
    void givenCageNotRecordedAndCageWithGradesEqualToTheEggs_whenInserting_thenAcceptBoth() {
        // given
        UUID sectorId = insertSector();
        UUID reportId = insertReport(sectorId);
        UUID notRecorded = insertCage(sectorId, 1);
        UUID recorded = insertCage(sectorId, 2);

        // when
        insertReportCage(reportId, notRecorded, 50, null, noGrades(), null, null);
        insertReportCage(reportId, recorded, 50, 30, List.of(10, 10, 5, 2, 2, 1), 0, 0);

        // then
        assertThat(jdbc.queryForObject("select count(*) from report_cage where report_id = ?", Integer.class, reportId))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("rejects deaths recorded without the culls")
    void givenDeathsWithoutCulls_whenInserting_thenRejectIt() {
        // given
        UUID sectorId = insertSector();
        UUID reportId = insertReport(sectorId);
        UUID cageId = insertCage(sectorId, 1);

        // when
        ThrowingCallable insertion = () -> insertReportCage(reportId, cageId, 50, null, noGrades(), 1, null);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("rejects deaths and culls above the birds of the cage")
    void givenRemovalsAboveTheCageBirds_whenInserting_thenRejectIt() {
        // given
        UUID sectorId = insertSector();
        UUID reportId = insertReport(sectorId);
        UUID cageId = insertCage(sectorId, 1);

        // when
        ThrowingCallable insertion = () -> insertReportCage(reportId, cageId, 50, null, noGrades(), 40, 15);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }
}
