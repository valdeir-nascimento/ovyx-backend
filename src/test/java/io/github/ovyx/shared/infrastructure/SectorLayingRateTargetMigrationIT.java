package io.github.ovyx.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * A V10 sobre setores que ja existiam (FR-004 e R-005 da 008): cada um fica com a meta de 85%, sem nenhuma
 * acao do administrador.
 *
 * <p>O banco do contexto ja esta na ultima versao, e por isso o teste migra um schema proprio em dois passos:
 * ate a V9, onde cadastra os setores como a 007 os deixaria, e depois ate a ultima. O schema e apagado no fim.
 */
@DisplayName("Sector laying rate target migration")
class SectorLayingRateTargetMigrationIT extends IntegrationTestSupport {

    private final String schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
    private final DriverManagerDataSource dataSource =
            new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

    private Flyway flywayUpTo(String target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    private void insertSector(JdbcTemplate jdbc, String name) {
        jdbc.update(
                "insert into " + schema + ".sector (id, name, status, created_at, updated_at)"
                        + " values (?, ?, 'ACTIVE', now(), now())",
                UUID.randomUUID(),
                name);
    }

    @AfterEach
    void dropSchema() {
        new JdbcTemplate(dataSource).execute("drop schema if exists " + schema + " cascade");
    }

    @Test
    @DisplayName("gives the sectors registered before the migration a target of 85%")
    void givenSectorsRegisteredBeforeTheMigration_whenMigrating_thenGiveEachATargetOf85() {
        // given
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        flywayUpTo("9").migrate();
        insertSector(jdbc, "Codornas — Galpão 1");
        insertSector(jdbc, "Poedeiras — Galpão 2");

        // when
        flywayUpTo("latest").migrate();

        // then
        List<BigDecimal> targets =
                jdbc.queryForList("select laying_rate_target from " + schema + ".sector", BigDecimal.class);
        assertThat(targets).containsExactly(new BigDecimal("85.0"), new BigDecimal("85.0"));
    }
}
