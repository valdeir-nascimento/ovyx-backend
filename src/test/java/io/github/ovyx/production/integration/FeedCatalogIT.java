package io.github.ovyx.production.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.production.domain.model.CatalogFormula;
import io.github.ovyx.production.domain.model.FeedFormulaId;
import io.github.ovyx.production.domain.port.FeedCatalog;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * O catálogo de fórmulas lido da tabela do farm (R-004 da 004): o production sabe da fórmula só o que o
 * lançamento de ração precisa, pela camada anticorrupção, como a {@code FarmStructure}.
 */
@DisplayName("Feed catalog")
class FeedCatalogIT extends IntegrationTestSupport {

    @Autowired
    private FeedCatalog catalog;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private ProductionFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new ProductionFixtures(jdbc);
    }

    @Test
    @DisplayName("reads an active formula with its name, its price and its expected intake")
    void givenActiveFormula_whenReadingIt_thenFindItsPriceAndIntake() {
        // given
        UUID id = fixtures.formula("2.85", 28, "ACTIVE");

        // when
        Optional<CatalogFormula> formula = catalog.formulaOf(FeedFormulaId.of(id));

        // then
        assertThat(formula).hasValueSatisfying(found -> {
            assertThat(found.name()).isEqualTo(fixtures.formulaNameOf(id));
            assertThat(found.active()).isTrue();
            assertThat(found.pricePerKg()).isEqualByComparingTo("2.85");
            assertThat(found.expectedIntake()).isEqualTo(28);
        });
    }

    @Test
    @DisplayName("reads an inactive formula as inactive")
    void givenInactiveFormula_whenReadingIt_thenFindItInactive() {
        // given
        UUID id = fixtures.formula("3.10", 24, "INACTIVE");

        // when
        Optional<CatalogFormula> formula = catalog.formulaOf(FeedFormulaId.of(id));

        // then
        assertThat(formula).hasValueSatisfying(found -> assertThat(found.active()).isFalse());
    }

    @Test
    @DisplayName("keeps the formula read by a feed locked until the feed commits, so it cannot change meanwhile")
    void givenFormulaReadInAnOpenTransaction_whenAnotherConnectionTriesToChangeIt_thenItWaitsForTheCommit() {
        // given
        UUID id = fixtures.formula("2.85", 28, "ACTIVE");
        TransactionTemplate feed = new TransactionTemplate(transactionManager);

        // when
        Throwable whileTheFeedIsOpen = feed.execute(status -> {
            catalog.formulaOf(FeedFormulaId.of(id));
            return catchThrowable(() -> lockFromAnotherConnection(id));
        });
        Throwable afterTheCommit = catchThrowable(() -> lockFromAnotherConnection(id));

        // then
        assertThat(whileTheFeedIsOpen)
                .hasRootCauseInstanceOf(SQLException.class)
                .rootCause()
                .satisfies(cause -> assertThat(((SQLException) cause).getSQLState()).isEqualTo("55P03"));
        assertThat(afterTheCommit).isNull();
    }

    /**
     * O que a inativacao ou a edicao da formula faria, numa conexao e numa thread de fora da transacao do
     * lancamento: travar a linha para escrever, sem esperar ({@code nowait}), para a espera virar erro.
     */
    private void lockFromAnotherConnection(UUID id) {
        CompletableFuture.runAsync(() -> jdbc.queryForList(
                        "select id from feed_formula where id = ? for update nowait", id))
                .join();
    }

    @Test
    @DisplayName("finds nothing for a formula that does not exist")
    void givenUnknownFormula_whenReadingIt_thenFindNothing() {
        // given
        FeedFormulaId unknown = FeedFormulaId.of(UUID.randomUUID());

        // when
        Optional<CatalogFormula> formula = catalog.formulaOf(unknown);

        // then
        assertThat(formula).isEmpty();
    }
}
