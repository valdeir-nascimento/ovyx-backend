package io.github.ovyx.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * A V11 sobre setores que ja existiam (FR-002 e R-003 da 010): cada um fica sem dia da pesagem, e segue o prazo
 * de 7 dias, sem nenhuma acao do administrador.
 *
 * <p>O banco do contexto ja esta na ultima versao, e por isso o teste migra um schema proprio em dois passos:
 * ate a V10, onde cadastra os setores como a 009 os deixaria, e depois ate a ultima. O schema e apagado no fim.
 */
@DisplayName("Sector weighing day migration")
class SectorWeighingDayMigrationIT extends IntegrationTestSupport {

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
                "insert into " + schema + ".sector (id, name, status, laying_rate_target, created_at, updated_at)"
                        + " values (?, ?, 'ACTIVE', 85.0, now(), now())",
                UUID.randomUUID(),
                name);
    }

    @AfterEach
    void dropSchema() {
        new JdbcTemplate(dataSource).execute("drop schema if exists " + schema + " cascade");
    }

    @Test
    @DisplayName("leaves the sectors registered before the migration without a weighing day")
    void givenSectorsRegisteredBeforeTheMigration_whenMigrating_thenLeaveEachWithoutWeighingDay() {
        // given
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        flywayUpTo("10").migrate();
        insertSector(jdbc, "Codornas — Galpão 1");
        insertSector(jdbc, "Poedeiras — Galpão 2");

        // when
        flywayUpTo("latest").migrate();

        // then
        List<String> days = jdbc.queryForList("select weighing_day from " + schema + ".sector", String.class);
        assertThat(days).hasSize(2).containsOnlyNulls();
    }
}
