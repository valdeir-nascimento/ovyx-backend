package io.github.ovyx.farm.infrastructure.persistence;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.application.sector.SectorDetail;
import io.github.ovyx.farm.application.sector.SectorDirectory;
import io.github.ovyx.farm.application.sector.SectorSummary;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Adaptador da porta de leitura dos setores (R-006).
 *
 * <p>Uma consulta so, com os totais das gaiolas ativas calculados nela: a lista nao carrega agregado
 * nenhum, e os totais nao tem como divergir das gaiolas.
 */
@Repository
public class JdbcSectorDirectory implements SectorDirectory {

    private static final String SELECT = """
            select s.id, s.name, s.description, s.status, s.created_at, s.updated_at,
                   count(c.id) filter (where c.status = 'ACTIVE') as active_cage_count,
                   coalesce(sum(c.bird_count) filter (where c.status = 'ACTIVE'), 0) as bird_count
              from sector s
              left join cage c on c.sector_id = s.id
            """;

    private static final String GROUP = " group by s.id";

    private final JdbcClient jdbcClient;

    JdbcSectorDirectory(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<SectorSummary> list(StatusFilter status) {
        // So entra na consulta o filtro pedido: com "todos", nenhuma condicao de situacao.
        String where = status == StatusFilter.ALL ? "" : " where s.status = :status";
        JdbcClient.StatementSpec statement = jdbcClient.sql(
                SELECT + where + GROUP
                        // O id desempata nomes iguais entre um ativo e inativos.
                        + " order by lower(s.name), s.id");
        if (status != StatusFilter.ALL) {
            statement = statement.param("status", status.name());
        }
        return statement.query((rs, rowNumber) -> summaryOf(rs)).list();
    }

    @Override
    public Optional<SectorDetail> findDetail(SectorId id) {
        String withBatteries = SELECT.replace(
                " from sector s",
                // As baterias de todas as gaiolas do setor, ativas ou nao: o filtro da lista de gaiolas
                // tambem serve para achar as inativas.
                ", (select coalesce(array_agg(distinct b.battery order by b.battery), '{}')"
                        + " from cage b where b.sector_id = s.id) as batteries\n  from sector s");
        return jdbcClient
                .sql(withBatteries + " where s.id = :id" + GROUP)
                .param("id", id.value())
                .query((rs, rowNumber) -> new SectorDetail(
                        SectorId.of(rs.getObject("id", UUID.class)),
                        rs.getString("name"),
                        rs.getString("description"),
                        Status.valueOf(rs.getString("status")),
                        rs.getInt("active_cage_count"),
                        rs.getInt("bird_count"),
                        Arrays.asList((String[]) rs.getArray("batteries").getArray()),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                        rs.getObject("updated_at", OffsetDateTime.class).toInstant()))
                .optional();
    }

    private static SectorSummary summaryOf(ResultSet rs) throws SQLException {
        return new SectorSummary(
                SectorId.of(rs.getObject("id", UUID.class)),
                rs.getString("name"),
                rs.getString("description"),
                Status.valueOf(rs.getString("status")),
                rs.getInt("active_cage_count"),
                rs.getInt("bird_count"));
    }
}
