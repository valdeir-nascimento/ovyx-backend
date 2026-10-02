package io.github.ovyx.production.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Setores e gaiolas inseridos por SQL, para os testes do production: o production não conhece o código
 * do farm (R-004), e os testes dele também não. Cada setor tem nome próprio, para os testes não dependerem
 * uns dos outros no mesmo banco.
 */
final class ProductionFixtures {

    private final JdbcTemplate jdbc;

    ProductionFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    UUID sector(String status) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into sector (id, name, status, laying_rate_target, created_at, updated_at)"
                        + " values (?, ?, ?, 85.0, now(), now())",
                id,
                "Codornas — Galpão " + id.toString().substring(0, 8),
                status);
        return id;
    }

    UUID activeSector() {
        return sector("ACTIVE");
    }

    UUID cage(UUID sectorId, String battery, int number, int birdCount, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into cage (id, sector_id, battery, number, bird_count, status, created_at, updated_at)"
                        + " values (?, ?, ?, ?, ?, ?, now(), now())",
                id,
                sectorId,
                battery,
                number,
                birdCount,
                status);
        return id;
    }

    UUID activeCage(UUID sectorId, String battery, int number, int birdCount) {
        return cage(sectorId, battery, number, birdCount, "ACTIVE");
    }

    /**
     * Uma pesagem valida da gaiola no dia dado: com ela, a gaiola fica em dia na agenda de pesagem e nao entra no
     * aviso de pesagem do painel (feature 010).
     */
    void weighing(UUID sectorId, UUID cageId, LocalDate day) {
        jdbc.update(
                "insert into weighing (id, sector_id, cage_id, weighed_on, average_weight, status, recorded_by_id,"
                        + " recorded_by_name, recorded_at) values (?, ?, ?, ?, 160.0, 'VALID', ?, 'Marina Alves', now())",
                UUID.randomUUID(),
                sectorId,
                cageId,
                day,
                UUID.randomUUID());
    }

    /** Um setor ativo com a A-01 (48 aves) e a B-07 (50 aves). */
    UUID sectorWithTwoCages() {
        UUID sectorId = activeSector();
        activeCage(sectorId, "A", 1, 48);
        activeCage(sectorId, "B", 7, 50);
        return sectorId;
    }

    String nameOf(UUID sectorId) {
        return jdbc.queryForObject("select name from sector where id = ?", String.class, sectorId);
    }

    /** Uma fórmula de ração com nome próprio, pela tabela do farm (feature 004). */
    UUID formula(String price, int expectedIntake, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into feed_formula (id, name, price_per_kg, expected_intake, status, created_at, updated_at)"
                        + " values (?, ?, ?, ?, ?, now(), now())",
                id,
                "Fórmula " + id.toString().substring(0, 8),
                new BigDecimal(price),
                expectedIntake,
                status);
        return id;
    }

    /** A Postura Plus: R$ 2,85 o quilo, 28 g por ave ao dia. */
    UUID posturaPlus() {
        return formula("2.85", 28, "ACTIVE");
    }

    String formulaNameOf(UUID formulaId) {
        return jdbc.queryForObject("select name from feed_formula where id = ?", String.class, formulaId);
    }

    /** Muda o preço da fórmula no farm, como a edição do administrador. */
    void repriceFormula(UUID formulaId, String price) {
        jdbc.update("update feed_formula set price_per_kg = ? where id = ?", new BigDecimal(price), formulaId);
    }

    void deactivateFormula(UUID formulaId) {
        jdbc.update("update feed_formula set status = 'INACTIVE' where id = ?", formulaId);
    }

    /** Muda a meta de produtividade do setor no farm, como a edição do administrador (feature 008). */
    void layingRateTarget(UUID sectorId, String value) {
        jdbc.update("update sector set laying_rate_target = ? where id = ?", new BigDecimal(value), sectorId);
    }

    void deactivate(UUID sectorId) {
        jdbc.update("update sector set status = 'INACTIVE' where id = ?", sectorId);
        jdbc.update("update cage set status = 'INACTIVE' where sector_id = ?", sectorId);
    }

    // ---------------------------------------------------------------- relatórios por SQL (painel, 006)

    /** Um setor com o nome dado, seguido de um pedaço do identificador para não repetir entre testes. */
    UUID sectorNamed(String name, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into sector (id, name, status, laying_rate_target, created_at, updated_at)"
                        + " values (?, ?, ?, 85.0, now(), now())",
                id,
                name + " " + id.toString().substring(0, 8),
                status);
        return id;
    }

    /** Um relatório sem gaiolas, aberto às 6h30 por Marina Alves. */
    UUID report(UUID sectorId, LocalDate date, int openingBirds, boolean noMortalityConfirmed) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into daily_report (id, sector_id, collection_date, collection_time, opening_bird_count,"
                        + " flock_age, no_mortality_confirmed, opened_by_id, opened_by_name, opened_at)"
                        + " values (?, ?, ?, ?, ?, 20, ?, ?, 'Marina Alves', now())",
                id,
                sectorId,
                date,
                LocalTime.of(6, 30),
                openingBirds,
                noMortalityConfirmed,
                UUID.randomUUID());
        return id;
    }

    /** A gaiola no relatório, com as aves dadas e sem nenhum lançamento. */
    void cageIn(UUID reportId, UUID cageId, int birds) {
        jdbc.update(
                "insert into report_cage (report_id, cage_id, battery, number, bird_count)"
                        + " select ?, id, battery, number, ? from cage where id = ?",
                reportId,
                birds,
                cageId);
    }

    /** A produção da gaiola: os ovos e as seis classes, na ordem pequenos, jumbo, sujos, trincados, sangue, anormais. */
    void production(UUID reportId, UUID cageId, int eggs, int... grades) {
        jdbc.update(
                "update report_cage set eggs = ?, small = ?, jumbo = ?, dirty = ?, cracked = ?, blood_spot = ?,"
                        + " abnormal = ? where report_id = ? and cage_id = ?",
                eggs,
                grades[0],
                grades[1],
                grades[2],
                grades[3],
                grades[4],
                grades[5],
                reportId,
                cageId);
    }

    void mortality(UUID reportId, UUID cageId, int deaths, int culls) {
        jdbc.update(
                "update report_cage set deaths = ?, culls = ? where report_id = ? and cage_id = ?",
                deaths,
                culls,
                reportId,
                cageId);
    }

    /** Um relatório de uma gaiola, completo: 44 ovos, a ração da fórmula e o dia sem ocorrência confirmado. */
    UUID completeReportOf(UUID sectorId, UUID cageId, LocalDate date, UUID formulaId) {
        UUID reportId = report(sectorId, date, 48, true);
        cageIn(reportId, cageId, 48);
        production(reportId, cageId, 44, 0, 0, 0, 0, 0, 0);
        feed(reportId, cageId, formulaId, 1344);
        return reportId;
    }

    /** A ração da gaiola, com o preço e o esperado da fórmula guardados, como o lançamento faz. */
    void feed(UUID reportId, UUID cageId, UUID formulaId, int consumption) {
        jdbc.update(
                "update report_cage set feed_formula_id = f.id, feed_price_per_kg = f.price_per_kg,"
                        + " feed_expected_intake = f.expected_intake, feed_consumption = ?"
                        + " from feed_formula f where f.id = ? and report_id = ? and cage_id = ?",
                consumption,
                formulaId,
                reportId,
                cageId);
    }
}
