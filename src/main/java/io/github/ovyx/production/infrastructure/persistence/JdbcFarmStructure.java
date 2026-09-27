package io.github.ovyx.production.infrastructure.persistence;

import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.production.domain.model.FarmCage;
import io.github.ovyx.production.domain.model.FarmSector;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.production.domain.port.FarmStructure;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Adaptador da porta {@link FarmStructure}: a camada anticorrupcao entre o production e o farm (R-004).
 *
 * <p>Le as tabelas {@code sector} e {@code cage} do farm, e so aqui, na infraestrutura do production. O
 * codigo dos dois contextos continua independente: o farm nao sabe que o production existe, e o dominio
 * do production so conhece as palavras dele ({@link FarmSector}, {@link FarmCage}).
 */
@Repository
public class JdbcFarmStructure implements FarmStructure {

    private final JdbcClient jdbcClient;

    JdbcFarmStructure(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<FarmSector> sectorOf(SectorId sectorId) {
        return jdbcClient
                .sql("select name, status from sector where id = :id")
                .param("id", sectorId.value())
                .query((row, index) -> new FarmSector(
                        sectorId,
                        row.getString("name"),
                        "ACTIVE".equals(row.getString("status")),
                        activeCagesOf(sectorId)))
                .optional();
    }

    private List<FarmCage> activeCagesOf(SectorId sectorId) {
        return jdbcClient
                .sql("select id, battery, number, bird_count from cage"
                        + " where sector_id = :sectorId and status = 'ACTIVE' order by battery, number")
                .param("sectorId", sectorId.value())
                .query((row, index) -> new FarmCage(
                        CageId.of(row.getObject("id", UUID.class)),
                        row.getString("battery"),
                        row.getInt("number"),
                        row.getInt("bird_count")))
                .list();
    }
}
