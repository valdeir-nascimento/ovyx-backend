package io.github.ovyx.farm.infrastructure.persistence;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.application.formula.FeedFormulaDirectory;
import io.github.ovyx.farm.application.formula.FeedFormulaSummary;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.farm.domain.model.Status;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Adaptador da porta de leitura das formulas, direto da tabela {@code feed_formula}.
 *
 * <p>O custo por ave ao dia nao e calculado aqui: o modelo de leitura o deriva do preco e do consumo
 * esperado, com o arredondamento da feature (R-007 da 004).
 */
@Repository
public class JdbcFeedFormulaDirectory implements FeedFormulaDirectory {

    private static final String SELECT = """
            select id, name, description, price_per_kg, expected_intake, status, created_at, updated_at
              from feed_formula
            """;

    private final JdbcClient jdbcClient;

    JdbcFeedFormulaDirectory(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<FeedFormulaSummary> list(StatusFilter status) {
        // So entra na consulta o filtro pedido: com "todas", nenhuma condicao de situacao.
        String where = status == StatusFilter.ALL ? "" : " where status = :status";
        JdbcClient.StatementSpec statement = jdbcClient.sql(SELECT + where + " order by lower(name), id");
        if (status != StatusFilter.ALL) {
            statement = statement.param("status", status.name());
        }
        return statement.query((rs, rowNumber) -> summaryOf(rs)).list();
    }

    @Override
    public Optional<FeedFormulaSummary> findById(FeedFormulaId id) {
        return jdbcClient
                .sql(SELECT + " where id = :id")
                .param("id", id.value())
                .query((rs, rowNumber) -> summaryOf(rs))
                .optional();
    }

    private static FeedFormulaSummary summaryOf(ResultSet rs) throws SQLException {
        return FeedFormulaSummary.of(
                FeedFormulaId.of(rs.getObject("id", UUID.class)),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBigDecimal("price_per_kg"),
                rs.getInt("expected_intake"),
                Status.valueOf(rs.getString("status")),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant());
    }
}
