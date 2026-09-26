package io.github.ovyx.production.integration;

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

    void deactivate(UUID sectorId) {
        jdbc.update("update sector set status = 'INACTIVE' where id = ?", sectorId);
        jdbc.update("update cage set status = 'INACTIVE' where sector_id = ?", sectorId);
    }
}
