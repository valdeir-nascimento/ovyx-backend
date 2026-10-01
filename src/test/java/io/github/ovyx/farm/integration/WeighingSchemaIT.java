package io.github.ovyx.farm.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.IntegrationTestSupport;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * As restrições da faixa de peso de {@code sector} e da tabela {@code weighing} (R-012 da 005;
 * data-model.md, seção Tabelas).
 *
 * <p>O banco repete as regras do domínio como defesa, e não como fonte delas. Cada restrição tem um caso
 * de recusa e um de aceite no limite, para que uma faixa trocada no SQL apareça.
 *
 * <p>Transacional: cada caso é desfeito ao fim, e as listas conferidas por outros testes, no mesmo banco,
 * não recebem as linhas daqui.
 */
@Transactional
@DisplayName("Weighing schema")
class WeighingSchemaIT extends IntegrationTestSupport {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 24);

    @Autowired
    private JdbcTemplate jdbc;

    private UUID insertSector(Integer minimum, Integer maximum) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into sector (id, name, status, reference_weight_min, reference_weight_max,"
                        + " laying_rate_target, created_at, updated_at) values (?, ?, 'ACTIVE', ?, ?, 85.0, now(), now())",
                id,
                "Galpão " + id,
                minimum,
                maximum);
        return id;
    }

    /** Um setor sem faixa e uma gaiola nele; devolve o setor e a gaiola. */
    private UUID[] insertSectorWithCage() {
        UUID sectorId = insertSector(null, null);
        UUID cageId = UUID.randomUUID();
        jdbc.update(
                "insert into cage (id, sector_id, battery, number, bird_count, status, created_at, updated_at)"
                        + " values (?, ?, 'A', 1, 48, 'ACTIVE', now(), now())",
                cageId,
                sectorId);
        return new UUID[] {sectorId, cageId};
    }

    private UUID insertWeighing(UUID[] sectorAndCage, LocalDate day, String weight, String status) {
        UUID id = UUID.randomUUID();
        boolean voided = "VOIDED".equals(status);
        jdbc.update(
                "insert into weighing (id, sector_id, cage_id, weighed_on, average_weight, status, recorded_by_id,"
                        + " recorded_by_name, recorded_at, voided_by_id, voided_by_name, voided_at)"
                        + " values (?, ?, ?, ?, ?, ?, ?, 'Marina Alves', now(), ?, ?, ?)",
                id,
                sectorAndCage[0],
                sectorAndCage[1],
                day,
                new BigDecimal(weight),
                status,
                UUID.randomUUID(),
                voided ? UUID.randomUUID() : null,
                voided ? "Marina Alves" : null,
                voided ? Timestamp.valueOf("2026-09-24 10:00:00") : null);
        return id;
    }

    private UUID insertWeighing(UUID[] sectorAndCage, String weight) {
        return insertWeighing(sectorAndCage, DAY, weight, "VALID");
    }

    // ---------------------------------------------------------------- faixa do setor

    @ParameterizedTest(name = "minimum {0}, maximum {1}")
    @CsvSource(value = {"155, NULL", "NULL, 175"}, nullValues = "NULL")
    @DisplayName("rejects a reference weight with only one limit")
    void givenOnlyOneLimit_whenInsertingSector_thenRejectIt(Integer minimum, Integer maximum) {
        // given
        Integer[] limits = {minimum, maximum};

        // when
        ThrowingCallable insertion = () -> insertSector(limits[0], limits[1]);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "minimum {0}, maximum {1}")
    @CsvSource({"0, 175", "155, 10001", "175, 155", "160, 160"})
    @DisplayName("rejects a reference weight out of range or inverted")
    void givenLimitsOutOfRangeOrInverted_whenInsertingSector_thenRejectIt(int minimum, int maximum) {
        // given
        int[] limits = {minimum, maximum};

        // when
        ThrowingCallable insertion = () -> insertSector(limits[0], limits[1]);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "minimum {0}, maximum {1}")
    @CsvSource({"1, 2", "9999, 10000", "155, 175"})
    @DisplayName("accepts a reference weight at the limits")
    void givenLimitsAtTheEdges_whenInsertingSector_thenAcceptThem(int minimum, int maximum) {
        // given
        UUID id = insertSector(minimum, maximum);

        // when
        Integer stored = jdbc.queryForObject("select reference_weight_max from sector where id = ?", Integer.class, id);

        // then
        assertThat(stored).isEqualTo(maximum);
    }

    @Test
    @DisplayName("accepts a sector without reference weight")
    void givenNoLimits_whenInsertingSector_thenAcceptIt() {
        // given
        UUID id = insertSector(null, null);

        // when
        Integer minimum = jdbc.queryForObject("select reference_weight_min from sector where id = ?", Integer.class, id);

        // then
        assertThat(minimum).isNull();
    }

    // ---------------------------------------------------------------- pesagem

    @Test
    @DisplayName("starts a weighing at version zero, with the weight at one decimal place")
    void givenWeighingInsertedWithoutVersion_whenReadingIt_thenFindZeroAndOneDecimal() {
        // given
        UUID id = insertWeighing(insertSectorWithCage(), "158.4");

        // when
        Long version = jdbc.queryForObject("select version from weighing where id = ?", Long.class, id);
        BigDecimal weight =
                jdbc.queryForObject("select average_weight from weighing where id = ?", BigDecimal.class, id);

        // then
        assertThat(version).isZero();
        assertThat(weight).isEqualByComparingTo("158.4").hasScaleOf(1);
    }

    @ParameterizedTest(name = "{0} g")
    @ValueSource(strings = {"0.9", "10000.1", "-1"})
    @DisplayName("rejects an average weight out of 1 to 10,000 g")
    void givenWeightOutOfRange_whenInsertingWeighing_thenRejectIt(String weight) {
        // given
        UUID[] sectorAndCage = insertSectorWithCage();

        // when
        ThrowingCallable insertion = () -> insertWeighing(sectorAndCage, weight);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "{0} g")
    @ValueSource(strings = {"1", "10000"})
    @DisplayName("accepts an average weight at the limits")
    void givenWeightAtTheLimits_whenInsertingWeighing_thenAcceptIt(String weight) {
        // given
        UUID[] sectorAndCage = insertSectorWithCage();

        // when
        UUID id = insertWeighing(sectorAndCage, weight);

        // then
        assertThat(jdbc.queryForObject("select count(*) from weighing where id = ?", Integer.class, id))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rejects a status other than VALID or VOIDED")
    void givenUnknownStatus_whenInsertingWeighing_thenRejectIt() {
        // given
        UUID[] sectorAndCage = insertSectorWithCage();

        // when
        ThrowingCallable insertion = () -> insertWeighing(sectorAndCage, DAY, "158.0", "DELETED");

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("rejects a valid weighing that carries the marks of a voiding")
    void givenValidWeighingWithVoidingMarks_whenInserting_thenRejectIt() {
        // given
        UUID[] sectorAndCage = insertSectorWithCage();

        // when
        ThrowingCallable insertion = () -> jdbc.update(
                "insert into weighing (id, sector_id, cage_id, weighed_on, average_weight, status, recorded_by_id,"
                        + " recorded_by_name, recorded_at, voided_by_id, voided_by_name, voided_at)"
                        + " values (?, ?, ?, ?, 158.0, 'VALID', ?, 'Marina Alves', now(), ?, 'Marina Alves', now())",
                UUID.randomUUID(),
                sectorAndCage[0],
                sectorAndCage[1],
                DAY,
                UUID.randomUUID(),
                UUID.randomUUID());

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("rejects a voided weighing without who voided it")
    void givenVoidedWeighingWithoutMarks_whenInserting_thenRejectIt() {
        // given
        UUID[] sectorAndCage = insertSectorWithCage();

        // when
        ThrowingCallable insertion = () -> jdbc.update(
                "insert into weighing (id, sector_id, cage_id, weighed_on, average_weight, status, recorded_by_id,"
                        + " recorded_by_name, recorded_at) values (?, ?, ?, ?, 158.0, 'VOIDED', ?, 'Marina Alves', now())",
                UUID.randomUUID(),
                sectorAndCage[0],
                sectorAndCage[1],
                DAY,
                UUID.randomUUID());

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("rejects a correction mark without the time it was made")
    void givenCorrectionMarkWithoutTime_whenInserting_thenRejectIt() {
        // given
        UUID[] sectorAndCage = insertSectorWithCage();

        // when
        ThrowingCallable insertion = () -> jdbc.update(
                "insert into weighing (id, sector_id, cage_id, weighed_on, average_weight, status, recorded_by_id,"
                        + " recorded_by_name, recorded_at, last_corrected_by_id, last_corrected_by_name)"
                        + " values (?, ?, ?, ?, 158.0, 'VALID', ?, 'Marina Alves', now(), ?, 'Marina Alves')",
                UUID.randomUUID(),
                sectorAndCage[0],
                sectorAndCage[1],
                DAY,
                UUID.randomUUID(),
                UUID.randomUUID());

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("rejects a weighing of a cage that does not exist")
    void givenUnknownCage_whenInsertingWeighing_thenRejectIt() {
        // given
        UUID[] sectorAndUnknownCage = {insertSectorWithCage()[0], UUID.randomUUID()};

        // when
        ThrowingCallable insertion = () -> insertWeighing(sectorAndUnknownCage, "158.0");

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------------------------------------------------------- uma válida por gaiola e data

    @Test
    @DisplayName("rejects a second valid weighing of the same cage on the same day")
    void givenValidWeighing_whenInsertingAnotherValidOnTheSameDay_thenRejectIt() {
        // given
        UUID[] sectorAndCage = insertSectorWithCage();
        insertWeighing(sectorAndCage, DAY, "158.0", "VALID");

        // when
        ThrowingCallable insertion = () -> insertWeighing(sectorAndCage, DAY, "161.0", "VALID");

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("accepts a valid weighing on the day of a voided one")
    void givenVoidedWeighing_whenInsertingAValidOnTheSameDay_thenAcceptIt() {
        // given
        UUID[] sectorAndCage = insertSectorWithCage();
        insertWeighing(sectorAndCage, DAY, "999.0", "VOIDED");

        // when
        insertWeighing(sectorAndCage, DAY, "160.0", "VALID");

        // then
        assertThat(jdbc.queryForObject(
                        "select count(*) from weighing where cage_id = ? and weighed_on = ?",
                        Integer.class,
                        sectorAndCage[1],
                        DAY))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("accepts valid weighings of the same cage on different days")
    void givenValidWeighing_whenInsertingAnotherOnTheNextDay_thenAcceptIt() {
        // given
        UUID[] sectorAndCage = insertSectorWithCage();
        insertWeighing(sectorAndCage, DAY, "158.0", "VALID");

        // when
        insertWeighing(sectorAndCage, DAY.plusDays(7), "161.0", "VALID");

        // then
        assertThat(jdbc.queryForObject("select count(*) from weighing where cage_id = ?", Integer.class, sectorAndCage[1]))
                .isEqualTo(2);
    }
}
