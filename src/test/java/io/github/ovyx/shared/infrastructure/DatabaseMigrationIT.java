package io.github.ovyx.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Comprova que as migracoes Flyway produzem o schema descrito em data-model.md, contra um
 * PostgreSQL real.
 *
 * <p>Verifica em especial os indices unicos <strong>parciais</strong> de e-mail e celular: e a
 * peca que faz FR-016 (unicidade entre ativos) conviver com FR-018 (historico do inativado
 * preservado). Um banco em memoria nao reproduziria esse comportamento.
 */
@DisplayName("Flyway migrations")
class DatabaseMigrationIT extends IntegrationTestSupport {

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("creates every table of the slice")
    void createsEveryTable() {
        List<String> tables = jdbc().queryForList(
                "SELECT lower(table_name) FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        assertThat(tables)
                .contains("caretaker", "access_event", "sign_in_attempt", "spring_session", "spring_session_attributes");
    }

    @Test
    @DisplayName("records the four migrations as successfully applied")
    void recordsEveryMigrationAsApplied() {
        List<String> versions = jdbc().queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = true AND version IS NOT NULL ORDER BY installed_rank",
                String.class);

        assertThat(versions).containsExactly("1", "2", "3", "4");
    }

    @Test
    @DisplayName("email and mobile phone indexes are partial, restricted to active caretakers")
    void emailAndMobileIndexesArePartial() {
        List<String> definitions = jdbc().queryForList(
                "SELECT indexdef FROM pg_indexes WHERE tablename = 'caretaker'", String.class);

        assertThat(definitions)
                .filteredOn(definition -> definition.contains("ux_caretaker_email_active"))
                .singleElement(org.assertj.core.api.InstanceOfAssertFactories.STRING)
                .contains("UNIQUE")
                .contains("WHERE ((status)::text = 'ACTIVE'::text)");

        assertThat(definitions)
                .filteredOn(definition -> definition.contains("ux_caretaker_mobile_active"))
                .singleElement(org.assertj.core.api.InstanceOfAssertFactories.STRING)
                .contains("UNIQUE")
                .contains("WHERE ((status)::text = 'ACTIVE'::text)");
    }

    @Test
    @DisplayName("the CPF index is unique regardless of status")
    void cpfIndexIsUnconditionallyUnique() {
        List<String> definitions = jdbc().queryForList(
                "SELECT indexdef FROM pg_indexes WHERE tablename = 'caretaker' AND indexname = 'ux_caretaker_cpf'",
                String.class);

        assertThat(definitions).singleElement(org.assertj.core.api.InstanceOfAssertFactories.STRING)
                .contains("UNIQUE")
                .doesNotContain("WHERE");
    }

    @Test
    @DisplayName("Hibernate never creates tables automatically")
    void hibernateNeverCreatesSchema() {
        // ddl-auto: none. Se o Hibernate estivesse criando schema, apareceriam tabelas fora da
        // lista das migracoes -- e a migracao versionada deixaria de ser a unica fonte do schema.
        List<String> tables = jdbc().queryForList(
                "SELECT lower(table_name) FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        assertThat(tables).containsExactlyInAnyOrder(
                "caretaker",
                "access_event",
                "sign_in_attempt",
                "spring_session",
                "spring_session_attributes",
                "flyway_schema_history");
    }
}
