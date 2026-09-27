package io.github.ovyx.production.infrastructure.persistence;

import io.github.ovyx.production.domain.model.CatalogFormula;
import io.github.ovyx.production.domain.model.FeedFormulaId;
import io.github.ovyx.production.domain.port.FeedCatalog;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Adaptador da porta {@link FeedCatalog}: a camada anticorrupcao entre o production e as formulas do farm
 * (R-004 da 004), como a {@link JdbcFarmStructure}.
 *
 * <p>Le a tabela {@code feed_formula} do farm, e so aqui. O farm nao sabe que o production existe, e o
 * dominio do production so conhece a {@link CatalogFormula}.
 *
 * <p>A linha da formula fica travada para leitura ate o fim da transacao: a formula inativada ou com outro
 * preco enquanto um lancamento a usa espera o lancamento gravar, e o lancamento nunca guarda o preco de
 * uma formula que ja mudou.
 */
@Repository
public class JdbcFeedCatalog implements FeedCatalog {

    private final JdbcClient jdbcClient;

    JdbcFeedCatalog(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<CatalogFormula> formulaOf(FeedFormulaId id) {
        return jdbcClient
                .sql("select name, status, price_per_kg, expected_intake from feed_formula where id = :id for share")
                .param("id", id.value())
                .query((row, index) -> new CatalogFormula(
                        id,
                        row.getString("name"),
                        "ACTIVE".equals(row.getString("status")),
                        row.getBigDecimal("price_per_kg"),
                        row.getInt("expected_intake")))
                .optional();
    }
}
