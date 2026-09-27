package io.github.ovyx.production.integration;

import java.math.BigDecimal;
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
                "insert into sector (id, name, status, created_at, updated_at) values (?, ?, ?, now(), now())",
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

    void deactivate(UUID sectorId) {
        jdbc.update("update sector set status = 'INACTIVE' where id = ?", sectorId);
        jdbc.update("update cage set status = 'INACTIVE' where sector_id = ?", sectorId);
    }
}
