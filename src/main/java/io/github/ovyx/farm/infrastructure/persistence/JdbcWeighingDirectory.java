package io.github.ovyx.farm.infrastructure.persistence;

import io.github.ovyx.farm.application.weighing.WeighedCage;
import io.github.ovyx.farm.application.weighing.WeighedSector;
import io.github.ovyx.farm.application.weighing.WeighingDetail;
import io.github.ovyx.farm.application.weighing.WeighingDirectory;
import io.github.ovyx.farm.application.weighing.WeighingEntry;
import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.Cage;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.domain.model.WeighingId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * A leitura das pesagens por JDBC, sem passar pelo agregado (principio V; R-008 da 005). As contas do
 * acompanhamento ficam na aplicacao, onde se testam sem banco.
 */
@Repository
public class JdbcWeighingDirectory implements WeighingDirectory {

    private final JdbcClient jdbcClient;

    JdbcWeighingDirectory(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<WeighedSector> sectorOf(SectorId sectorId) {
        return jdbcClient
                .sql("select id, name, status, reference_weight_min, reference_weight_max from sector where id = :id")
                .param("id", sectorId.value())
                .query((rs, rowNumber) -> new WeighedSector(
                        SectorId.of(rs.getObject("id", UUID.class)),
                        rs.getString("name"),
                        Status.valueOf(rs.getString("status")),
                        JdbcSectorDirectory.referenceWeightOf(rs)))
                .optional();
    }

    @Override
    public Optional<WeighedCage> cageOf(SectorId sectorId, CageId cageId) {
        return jdbcClient
                .sql("select id, battery, number, bird_count, status from cage where id = :id and sector_id = :sectorId")
                .param("id", cageId.value())
                .param("sectorId", sectorId.value())
                .query((rs, rowNumber) -> {
                    String battery = rs.getString("battery");
                    int number = rs.getInt("number");
                    return new WeighedCage(
                            CageId.of(rs.getObject("id", UUID.class)),
                            Cage.codeOf(battery, number),
                            battery,
                            number,
                            rs.getInt("bird_count"),
                            Status.valueOf(rs.getString("status")));
                })
                .optional();
    }

    @Override
    public List<WeighingEntry> weighingsOf(CageId cageId) {
        return jdbcClient
                .sql("""
                        select id, weighed_on, average_weight, recorded_by_id, recorded_by_name,
                               last_corrected_by_id, last_corrected_by_name
                          from weighing
                         where cage_id = :cageId and status = 'VALID'
                         order by weighed_on
                        """)
                .param("cageId", cageId.value())
                .query((rs, rowNumber) -> entryOf(rs))
                .list();
    }

    @Override
    public Optional<WeighingDetail> findWeighing(CageId cageId, WeighingId weighingId) {
        return jdbcClient
                .sql("""
                        select id, weighed_on, average_weight, recorded_by_id, recorded_by_name, recorded_at,
                               last_corrected_by_id, last_corrected_by_name, last_corrected_at
                          from weighing
                         where id = :id and cage_id = :cageId and status = 'VALID'
                        """)
                .param("id", weighingId.value())
                .param("cageId", cageId.value())
                .query((rs, rowNumber) -> new WeighingDetail(
                        WeighingId.of(rs.getObject("id", UUID.class)),
                        rs.getObject("weighed_on", LocalDate.class),
                        rs.getBigDecimal("average_weight"),
                        new Actor(rs.getObject("recorded_by_id", UUID.class), rs.getString("recorded_by_name")),
                        rs.getObject("recorded_at", OffsetDateTime.class).toInstant(),
                        correctorOf(rs),
                        instantOf(rs, "last_corrected_at")))
                .optional();
    }

    private static Actor correctorOf(ResultSet rs) throws SQLException {
        UUID id = rs.getObject("last_corrected_by_id", UUID.class);
        return id == null ? null : new Actor(id, rs.getString("last_corrected_by_name"));
    }

    private static Instant instantOf(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static WeighingEntry entryOf(ResultSet rs) throws SQLException {
        return new WeighingEntry(
                WeighingId.of(rs.getObject("id", UUID.class)),
                rs.getObject("weighed_on", LocalDate.class),
                rs.getBigDecimal("average_weight"),
                new Actor(rs.getObject("recorded_by_id", UUID.class), rs.getString("recorded_by_name")),
                correctorOf(rs));
    }
}
