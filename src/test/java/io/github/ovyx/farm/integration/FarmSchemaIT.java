package io.github.ovyx.farm.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.IntegrationTestSupport;
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
 * As restrições das tabelas {@code sector} e {@code cage} (R-005, R-010; data-model.md, seção Tabelas).
 *
 * <p>O banco repete as regras do domínio como defesa, e não como fonte delas: um defeito no agregado,
 * ou uma corrida entre dois cadastros, ainda esbarra aqui. Cada restrição tem um caso de recusa e um
 * de aceite no limite, para que uma faixa trocada no SQL apareça.
 *
 * <p>Transacional: cada caso é desfeito ao fim, e as listas conferidas por outros testes, no mesmo
 * banco, não recebem as linhas daqui.
 */
@Transactional
@DisplayName("Farm schema")
class FarmSchemaIT extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    private UUID insertSector(String name, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into sector (id, name, status, created_at, updated_at) values (?, ?, ?, now(), now())",
                id,
                name,
                status);
        return id;
    }

    private UUID insertActiveSector() {
        return insertSector("Galpão " + UUID.randomUUID(), "ACTIVE");
    }

    private void insertCage(UUID sectorId, String battery, int number, int birdCount, String status) {
        jdbc.update(
                "insert into cage (id, sector_id, battery, number, bird_count, status, created_at, updated_at)"
                        + " values (?, ?, ?, ?, ?, ?, now(), now())",
                UUID.randomUUID(),
                sectorId,
                battery,
                number,
                birdCount,
                status);
    }

    @Test
    @DisplayName("rejects a sector name of one character")
    void givenSectorNameOfOneCharacter_whenInserting_thenRejectIt() {
        // given
        String name = "A";

        // when
        ThrowingCallable insertion = () -> insertSector(name, "ACTIVE");

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "{0} characters")
    @ValueSource(ints = {2, 80})
    @DisplayName("accepts a sector name at the limits of the length")
    void givenSectorNameAtTheLimitOfTheLength_whenInserting_thenAcceptIt(int length) {
        // given
        String name = "G".repeat(length);

        // when
        UUID id = insertSector(name, "ACTIVE");

        // then
        assertThat(jdbc.queryForObject("select name from sector where id = ?", String.class, id))
                .isEqualTo(name);
    }

    @Test
    @DisplayName("rejects a sector status outside ACTIVE and INACTIVE")
    void givenUnknownSectorStatus_whenInserting_thenRejectIt() {
        // given
        String status = "DELETED";

        // when
        ThrowingCallable insertion = () -> insertSector("Galpão " + UUID.randomUUID(), status);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("starts the sector version at zero")
    void givenSectorInsertedWithoutVersion_whenReadingIt_thenFindVersionZero() {
        // given
        UUID id = insertActiveSector();

        // when
        Long version = jdbc.queryForObject("select version from sector where id = ?", Long.class, id);

        // then
        assertThat(version).isZero();
    }

    @Test
    @DisplayName("rejects two active sectors whose names differ only in case")
    void givenActiveSectorNamed_whenInsertingAnotherActiveWithTheSameNameInOtherCase_thenRejectIt() {
        // given
        String name = "Galpão " + UUID.randomUUID();
        insertSector(name, "ACTIVE");

        // when
        ThrowingCallable insertion = () -> insertSector(name.toUpperCase(), "ACTIVE");

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("accepts the name of an inactive sector for an active one")
    void givenInactiveSectorNamed_whenInsertingAnActiveWithTheSameName_thenAcceptIt() {
        // given
        String name = "Galpão " + UUID.randomUUID();
        insertSector(name, "INACTIVE");

        // when
        insertSector(name, "ACTIVE");

        // then
        assertThat(jdbc.queryForObject("select count(*) from sector where lower(name) = lower(?)", Integer.class, name))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("rejects a cage of a sector that does not exist")
    void givenUnknownSector_whenInsertingACage_thenRejectIt() {
        // given
        UUID unknownSector = UUID.randomUUID();

        // when
        ThrowingCallable insertion = () -> insertCage(unknownSector, "B", 7, 50, "ACTIVE");

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "battery \"{0}\"")
    @ValueSource(strings = {"b", "AB-", "", "Ç"})
    @DisplayName("rejects a battery outside one to three capital letters or digits")
    void givenBatteryOutsideThePattern_whenInsertingACage_thenRejectIt(String battery) {
        // given
        UUID sector = insertActiveSector();

        // when
        ThrowingCallable insertion = () -> insertCage(sector, battery, 7, 50, "ACTIVE");

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "battery \"{0}\"")
    @ValueSource(strings = {"A", "B1", "C12"})
    @DisplayName("accepts a battery of one to three capital letters or digits")
    void givenBatteryInsideThePattern_whenInsertingACage_thenAcceptIt(String battery) {
        // given
        UUID sector = insertActiveSector();

        // when
        insertCage(sector, battery, 7, 50, "ACTIVE");

        // then
        assertThat(jdbc.queryForObject("select battery from cage where sector_id = ?", String.class, sector))
                .isEqualTo(battery);
    }

    @ParameterizedTest(name = "number {0}")
    @ValueSource(ints = {0, 1000})
    @DisplayName("rejects a cage number outside 1 to 999")
    void givenCageNumberOutsideTheRange_whenInsertingACage_thenRejectIt(int number) {
        // given
        UUID sector = insertActiveSector();

        // when
        ThrowingCallable insertion = () -> insertCage(sector, "B", number, 50, "ACTIVE");

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "number {0}")
    @ValueSource(ints = {1, 999})
    @DisplayName("accepts a cage number at the limits of the range")
    void givenCageNumberAtTheLimitOfTheRange_whenInsertingACage_thenAcceptIt(int number) {
        // given
        UUID sector = insertActiveSector();

        // when
        insertCage(sector, "B", number, 50, "ACTIVE");

        // then
        assertThat(jdbc.queryForObject("select number from cage where sector_id = ?", Integer.class, sector))
                .isEqualTo(number);
    }

    @ParameterizedTest(name = "{0} birds")
    @ValueSource(ints = {-1, 1001})
    @DisplayName("rejects a bird count outside 0 to 1,000")
    void givenBirdCountOutsideTheRange_whenInsertingACage_thenRejectIt(int birdCount) {
        // given
        UUID sector = insertActiveSector();

        // when
        ThrowingCallable insertion = () -> insertCage(sector, "B", 7, birdCount, "ACTIVE");

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "{0} birds")
    @ValueSource(ints = {0, 1000})
    @DisplayName("accepts a bird count at the limits of the range, zero being an empty cage")
    void givenBirdCountAtTheLimitOfTheRange_whenInsertingACage_thenAcceptIt(int birdCount) {
        // given
        UUID sector = insertActiveSector();

        // when
        insertCage(sector, "B", 7, birdCount, "ACTIVE");

        // then
        assertThat(jdbc.queryForObject("select bird_count from cage where sector_id = ?", Integer.class, sector))
                .isEqualTo(birdCount);
    }

    @Test
    @DisplayName("rejects a cage status outside ACTIVE and INACTIVE")
    void givenUnknownCageStatus_whenInsertingACage_thenRejectIt() {
        // given
        UUID sector = insertActiveSector();

        // when
        ThrowingCallable insertion = () -> insertCage(sector, "B", 7, 50, "DELETED");

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("marks a new cage as not deactivated with its sector")
    void givenCageInsertedWithoutTheMark_whenReadingIt_thenFindItFalse() {
        // given
        UUID sector = insertActiveSector();
        insertCage(sector, "B", 7, 50, "ACTIVE");

        // when
        Boolean mark = jdbc.queryForObject(
                "select deactivated_with_sector from cage where sector_id = ?", Boolean.class, sector);

        // then
        assertThat(mark).isFalse();
    }

    @Test
    @DisplayName("rejects two active cages with the same battery and number in one sector")
    void givenActiveCage_whenInsertingAnotherActiveWithTheSameBatteryAndNumberInTheSameSector_thenRejectIt() {
        // given
        UUID sector = insertActiveSector();
        insertCage(sector, "B", 7, 50, "ACTIVE");

        // when
        ThrowingCallable insertion = () -> insertCage(sector, "B", 7, 48, "ACTIVE");

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("accepts the same battery and number in another sector")
    void givenActiveCage_whenInsertingTheSameBatteryAndNumberInAnotherSector_thenAcceptIt() {
        // given
        UUID sector = insertActiveSector();
        UUID another = insertActiveSector();
        insertCage(sector, "B", 7, 50, "ACTIVE");

        // when
        insertCage(another, "B", 7, 50, "ACTIVE");

        // then
        assertThat(jdbc.queryForObject(
                        "select count(*) from cage where sector_id in (?, ?)", Integer.class, sector, another))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("accepts the same battery and number when one of the cages is inactive")
    void givenInactiveCage_whenInsertingAnActiveWithTheSameBatteryAndNumber_thenAcceptIt() {
        // given
        UUID sector = insertActiveSector();
        insertCage(sector, "B", 7, 50, "INACTIVE");

        // when
        insertCage(sector, "B", 7, 50, "ACTIVE");

        // then
        assertThat(jdbc.queryForObject("select count(*) from cage where sector_id = ?", Integer.class, sector))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("indexes the cages by sector, status, battery and number for the paged list")
    void givenMigratedSchema_whenReadingTheCageIndexes_thenFindTheOneOfThePagedList() {
        // given
        String table = "cage";

        // when
        List<String> definitions = jdbc.queryForList(
                "select indexdef from pg_indexes where tablename = ?", String.class, table);

        // then
        assertThat(definitions)
                .anyMatch(definition -> definition.contains("(sector_id, status, battery, number)")
                        && !definition.contains("UNIQUE"));
    }
}
