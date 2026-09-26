package io.github.ovyx.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import io.github.ovyx.IntegrationTestSupport;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    private static final String PUBLIC_TABLES = """
            SELECT lower(table_name)
              FROM information_schema.tables
             WHERE table_schema = 'public'
            """;

    private static final String APPLIED_VERSIONS = """
            SELECT version
              FROM flyway_schema_history
             WHERE success = true
               AND version IS NOT NULL
             ORDER BY installed_rank
            """;

    private static final String CARETAKER_INDEX = """
            SELECT indexdef
              FROM pg_indexes
             WHERE tablename = 'caretaker'
               AND indexname = ?
            """;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("creates every table of the slice")
    void givenMigratedDatabase_whenListingTables_thenFindEveryTableOfTheSlice() {
        // given — o contentor compartilhado, migrado na subida

        // when
        List<String> tables = jdbc().queryForList(PUBLIC_TABLES, String.class);

        // then
        assertThat(tables)
                .contains("caretaker", "access_event", "sign_in_attempt", "spring_session", "spring_session_attributes");
    }

    @Test
    @DisplayName("records the seven migrations as successfully applied")
    void givenMigratedDatabase_whenReadingTheHistory_thenFindTheSevenMigrationsApplied() {
        // given — o contentor compartilhado, migrado na subida

        // when
        List<String> versions = jdbc().queryForList(APPLIED_VERSIONS, String.class);

        // then
        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6", "7");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ux_caretaker_email_active", "ux_caretaker_mobile_active"})
    @DisplayName("email and mobile phone indexes are partial, restricted to active caretakers")
    void givenIdentifierIndex_whenReadingItsDefinition_thenRestrictUniquenessToActiveCaretakers(String index) {
        // given — indice vindo do @ValueSource

        // when
        List<String> definitions = jdbc().queryForList(CARETAKER_INDEX, String.class, index);

        // then
        assertThat(definitions)
                .singleElement(STRING)
                .contains("UNIQUE")
                .contains("WHERE ((status)::text = 'ACTIVE'::text)");
    }

    @Test
    @DisplayName("the CPF index is unique regardless of status")
    void givenCpfIndex_whenReadingItsDefinition_thenKeepItUniqueRegardlessOfStatus() {
        // given
        String index = "ux_caretaker_cpf";

        // when
        List<String> definitions = jdbc().queryForList(CARETAKER_INDEX, String.class, index);

        // then
        assertThat(definitions).singleElement(STRING).contains("UNIQUE").doesNotContain("WHERE");
    }

    @Test
    @DisplayName("Hibernate never creates tables automatically")
    void givenMigratedDatabase_whenListingTables_thenFindOnlyTablesCreatedByMigrations() {
        // given
        // ddl-auto: none. Se o Hibernate estivesse criando schema, apareceriam tabelas fora da
        // lista das migracoes -- e a migracao versionada deixaria de ser a unica fonte do schema.

        // when
        List<String> tables = jdbc().queryForList(PUBLIC_TABLES, String.class);

        // then
        assertThat(tables)
                .containsExactlyInAnyOrder(
                        "caretaker",
                        "access_event",
                        "sign_in_attempt",
                        "spring_session",
                        "spring_session_attributes",
                        "sector",
                        "cage",
                        "daily_report",
                        "report_cage",
                        "flyway_schema_history");
    }
}
