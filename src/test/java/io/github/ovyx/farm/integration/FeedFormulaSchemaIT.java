package io.github.ovyx.farm.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.IntegrationTestSupport;
import java.math.BigDecimal;
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
 * As restrições da tabela {@code feed_formula} e das colunas de ração de {@code report_cage} (R-012;
 * data-model.md, seção Tabelas).
 *
 * <p>O banco repete as regras do domínio como defesa, e não como fonte delas. Cada restrição tem um
 * caso de recusa e um de aceite no limite, para que uma faixa trocada no SQL apareça.
 *
 * <p>Transacional: cada caso é desfeito ao fim, e as listas conferidas por outros testes, no mesmo
 * banco, não recebem as linhas daqui.
 */
@Transactional
@DisplayName("Feed formula schema")
class FeedFormulaSchemaIT extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    private UUID insertFormula(String name, String price, int intake, String description, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into feed_formula (id, name, price_per_kg, expected_intake, description, status,"
                        + " created_at, updated_at) values (?, ?, ?, ?, ?, ?, now(), now())",
                id,
                name,
                new BigDecimal(price),
                intake,
                description,
                status);
        return id;
    }

    private UUID insertFormula(String name) {
        return insertFormula(name, "2.85", 28, null, "ACTIVE");
    }

    private static String uniqueName() {
        return "Fórmula " + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Um setor, uma gaiola e um relatório com ela; devolve o relatório e a gaiola. */
    private UUID[] insertReportWithCage() {
        UUID sectorId = UUID.randomUUID();
        jdbc.update(
                "insert into sector (id, name, status, created_at, updated_at) values (?, ?, 'ACTIVE', now(), now())",
                sectorId,
                "Galpão " + sectorId);
        UUID cageId = UUID.randomUUID();
        jdbc.update(
                "insert into cage (id, sector_id, battery, number, bird_count, status, created_at, updated_at)"
                        + " values (?, ?, 'A', 1, 50, 'ACTIVE', now(), now())",
                cageId,
                sectorId);
        UUID reportId = UUID.randomUUID();
        jdbc.update(
                "insert into daily_report (id, sector_id, collection_date, collection_time, opening_bird_count,"
                        + " flock_age, opened_by_id, opened_by_name, opened_at)"
                        + " values (?, ?, ?, time '06:30', 50, 20, ?, 'Marina Alves', now())",
                reportId,
                sectorId,
                LocalDate.of(2026, 9, 24),
                UUID.randomUUID());
        return new UUID[] {reportId, cageId};
    }

    /** A gaiola no relatório com as quatro colunas de ração dadas; nulo é "não lançada". */
    private void insertReportCageFeed(
            UUID[] reportAndCage, UUID formulaId, String price, Integer intake, Integer consumption) {
        jdbc.update(
                "insert into report_cage (report_id, cage_id, battery, number, bird_count, feed_formula_id,"
                        + " feed_price_per_kg, feed_expected_intake, feed_consumption)"
                        + " values (?, ?, 'A', 1, 50, ?, ?, ?, ?)",
                reportAndCage[0],
                reportAndCage[1],
                formulaId,
                price == null ? null : new BigDecimal(price),
                intake,
                consumption);
    }

    @Test
    @DisplayName("starts a formula at version zero")
    void givenFormulaInsertedWithoutVersion_whenReadingIt_thenFindZero() {
        // given
        UUID id = insertFormula(uniqueName());

        // when
        Long version = jdbc.queryForObject("select version from feed_formula where id = ?", Long.class, id);

        // then
        assertThat(version).isZero();
    }

    @Test
    @DisplayName("rejects a formula without a name")
    void givenNoName_whenInsertingFormula_thenRejectIt() {
        // given
        String name = null;

        // when
        ThrowingCallable insertion = () -> insertFormula(name);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    /** Um nome único de exatamente 80 caracteres. */
    private static String longestName() {
        return "F" + UUID.randomUUID().toString().replace("-", "") + "x".repeat(47);
    }

    @Test
    @DisplayName("rejects a name longer than 80 characters")
    void givenNameLongerThanEighty_whenInsertingFormula_thenRejectIt() {
        // given
        String tooLong = longestName() + "x";

        // when
        ThrowingCallable insertion = () -> insertFormula(tooLong);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("accepts a name of 80 characters")
    void givenNameOfEighty_whenInsertingFormula_thenAcceptIt() {
        // given
        String longest = longestName();

        // when
        UUID id = insertFormula(longest);

        // then
        assertThat(jdbc.queryForObject("select length(name) from feed_formula where id = ?", Integer.class, id))
                .isEqualTo(80);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"ACTIVE", "INACTIVE"})
    @DisplayName("rejects a repeated name regardless of case, against an active or an inactive formula")
    void givenFormulaWithTheName_whenInsertingTheSameNameInAnotherCase_thenRejectIt(String existingStatus) {
        // given
        String name = uniqueName();
        insertFormula(name, "2.85", 28, null, existingStatus);

        // when
        ThrowingCallable insertion = () -> insertFormula(name.toUpperCase());

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "R$ {0}")
    @ValueSource(strings = {"0.00", "1000.01"})
    @DisplayName("rejects a price outside 0.01 to 1,000.00")
    void givenPriceOutOfRange_whenInsertingFormula_thenRejectIt(String price) {
        // given
        String name = uniqueName();

        // when
        ThrowingCallable insertion = () -> insertFormula(name, price, 28, null, "ACTIVE");

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "R$ {0}")
    @ValueSource(strings = {"0.01", "1000.00"})
    @DisplayName("accepts a price at the limits")
    void givenPriceAtTheLimits_whenInsertingFormula_thenAcceptIt(String price) {
        // given
        String name = uniqueName();

        // when
        UUID id = insertFormula(name, price, 28, null, "ACTIVE");

        // then
        assertThat(jdbc.queryForObject("select price_per_kg from feed_formula where id = ?", BigDecimal.class, id))
                .isEqualByComparingTo(price);
    }

    @Test
    @DisplayName("keeps the price with two decimal places")
    void givenPriceWithThreeDecimals_whenInsertingFormula_thenStoreItWithTwo() {
        // given
        String name = uniqueName();

        // when
        UUID id = insertFormula(name, "2.855", 28, null, "ACTIVE");

        // then
        assertThat(jdbc.queryForObject("select price_per_kg from feed_formula where id = ?", BigDecimal.class, id))
                .hasScaleOf(2);
    }

    @ParameterizedTest(name = "{0} g")
    @ValueSource(ints = {0, 201})
    @DisplayName("rejects an expected intake outside 1 to 200 grams")
    void givenExpectedIntakeOutOfRange_whenInsertingFormula_thenRejectIt(int intake) {
        // given
        String name = uniqueName();

        // when
        ThrowingCallable insertion = () -> insertFormula(name, "2.85", intake, null, "ACTIVE");

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "{0} g")
    @ValueSource(ints = {1, 200})
    @DisplayName("accepts an expected intake at the limits")
    void givenExpectedIntakeAtTheLimits_whenInsertingFormula_thenAcceptIt(int intake) {
        // given
        String name = uniqueName();

        // when
        UUID id = insertFormula(name, "2.85", intake, null, "ACTIVE");

        // then
        assertThat(jdbc.queryForObject("select expected_intake from feed_formula where id = ?", Integer.class, id))
                .isEqualTo(intake);
    }

    @Test
    @DisplayName("rejects a description longer than 500 characters")
    void givenDescriptionLongerThanFiveHundred_whenInsertingFormula_thenRejectIt() {
        // given
        String name = uniqueName();

        // when
        ThrowingCallable insertion = () -> insertFormula(name, "2.85", 28, "d".repeat(501), "ACTIVE");

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("accepts a description of 500 characters")
    void givenDescriptionOfFiveHundred_whenInsertingFormula_thenAcceptIt() {
        // given
        String description = "d".repeat(500);

        // when
        UUID id = insertFormula(uniqueName(), "2.85", 28, description, "ACTIVE");

        // then
        assertThat(jdbc.queryForObject("select length(description) from feed_formula where id = ?", Integer.class, id))
                .isEqualTo(500);
    }

    @Test
    @DisplayName("rejects a status other than ACTIVE or INACTIVE")
    void givenUnknownStatus_whenInsertingFormula_thenRejectIt() {
        // given
        String name = uniqueName();

        // when
        ThrowingCallable insertion = () -> insertFormula(name, "2.85", 28, null, "DELETED");

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("accepts a report cage with no feed recorded")
    void givenNoFeedColumns_whenInsertingReportCage_thenAcceptIt() {
        // given
        UUID[] reportAndCage = insertReportWithCage();

        // when
        insertReportCageFeed(reportAndCage, null, null, null, null);

        // then
        assertThat(jdbc.queryForObject(
                        "select count(*) from report_cage where report_id = ? and feed_formula_id is null",
                        Integer.class,
                        reportAndCage[0]))
                .isOne();
    }

    @ParameterizedTest(name = "{0} g")
    @ValueSource(ints = {0, 50_000})
    @DisplayName("accepts the feed of a cage with the consumption at the limits")
    void givenConsumptionAtTheLimits_whenInsertingReportCage_thenAcceptIt(int consumption) {
        // given
        UUID[] reportAndCage = insertReportWithCage();
        UUID formulaId = insertFormula(uniqueName());

        // when
        insertReportCageFeed(reportAndCage, formulaId, "2.85", 28, consumption);

        // then
        assertThat(jdbc.queryForObject(
                        "select feed_consumption from report_cage where report_id = ?",
                        Integer.class,
                        reportAndCage[0]))
                .isEqualTo(consumption);
    }

    @ParameterizedTest(name = "{0} g")
    @ValueSource(ints = {-1, 50_001})
    @DisplayName("rejects a consumption outside 0 to 50,000 grams")
    void givenConsumptionOutOfRange_whenInsertingReportCage_thenRejectIt(int consumption) {
        // given
        UUID[] reportAndCage = insertReportWithCage();
        UUID formulaId = insertFormula(uniqueName());

        // when
        ThrowingCallable insertion = () -> insertReportCageFeed(reportAndCage, formulaId, "2.85", 28, consumption);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "formula {0}, price {1}, intake {2}, consumption {3}")
    @CsvSource(
            nullValues = "null",
            value = {
                "true, null, 28, 1344",
                "true, 2.85, null, 1344",
                "true, 2.85, 28, null",
                "false, 2.85, 28, 1344",
                "false, null, null, 1344"
            })
    @DisplayName("rejects feed columns filled only in part")
    void givenFeedColumnsPartlyFilled_whenInsertingReportCage_thenRejectIt(
            boolean withFormula, String price, Integer intake, Integer consumption) {
        // given
        UUID[] reportAndCage = insertReportWithCage();
        UUID formulaId = withFormula ? insertFormula(uniqueName()) : null;

        // when
        ThrowingCallable insertion =
                () -> insertReportCageFeed(reportAndCage, formulaId, price, intake, consumption);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("rejects the feed of a formula that does not exist")
    void givenUnknownFormula_whenInsertingReportCageFeed_thenRejectIt() {
        // given
        UUID[] reportAndCage = insertReportWithCage();

        // when
        ThrowingCallable insertion =
                () -> insertReportCageFeed(reportAndCage, UUID.randomUUID(), "2.85", 28, 1344);

        // then
        assertThatThrownBy(insertion).isInstanceOf(DataIntegrityViolationException.class);
    }
}
