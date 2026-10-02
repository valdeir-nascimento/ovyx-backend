package io.github.ovyx.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.IntegrationTestSupport;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * A V12 sobre responsáveis que já existiam (FR-006 e R-002 da 011): cada um fica com o tema igual ao sistema, sem
 * nenhuma ação, e a coluna só aceita os três temas.
 *
 * <p>O banco do contexto já está na última versão, e por isso o teste migra um schema próprio em dois passos: até a
 * V11, onde cadastra os responsáveis como a 010 os deixaria, e depois até a última. O schema é apagado no fim.
 */
@DisplayName("Caretaker theme migration")
class CaretakerThemeMigrationIT extends IntegrationTestSupport {

    private final String schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
    private final DriverManagerDataSource dataSource =
            new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    private final JdbcTemplate jdbc = new JdbcTemplate(dataSource);

    private Flyway flywayUpTo(String target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    private UUID insertCaretaker(String cpf) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into " + schema + ".caretaker (id, full_name, cpf, email, mobile_phone, password_hash, role,"
                        + " status, created_at, updated_at)"
                        + " values (?, 'Maria Silva', ?, ?, '91988887777', '{argon2}x', 'USER', 'INACTIVE', now(), now())",
                id,
                cpf,
                "maria." + cpf + "@ovyx.com.br");
        return id;
    }

    @AfterEach
    void dropSchema() {
        jdbc.execute("drop schema if exists " + schema + " cascade");
    }

    @Test
    @DisplayName("gives the caretakers registered before the migration the theme of the system")
    void givenCaretakersRegisteredBeforeTheMigration_whenMigrating_thenGiveEachTheThemeOfTheSystem() {
        // given
        flywayUpTo("11").migrate();
        insertCaretaker("52998224725");
        insertCaretaker("11144477735");

        // when
        flywayUpTo("latest").migrate();

        // then
        List<String> themes = jdbc.queryForList("select theme_preference from " + schema + ".caretaker", String.class);
        assertThat(themes).containsExactly("SYSTEM", "SYSTEM");
    }

    @Test
    @DisplayName("keeps the theme in a required column of up to 6 characters")
    void givenMigratedSchema_whenReadingTheThemeColumn_thenFindARequiredVarcharOf6() {
        // given
        flywayUpTo("latest").migrate();

        // when
        List<String> column = jdbc.queryForObject(
                "select data_type, character_maximum_length, is_nullable from information_schema.columns"
                        + " where table_schema = ? and table_name = 'caretaker' and column_name = 'theme_preference'",
                (rs, row) -> List.of(rs.getString(1), rs.getString(2), rs.getString(3)),
                schema);

        // then
        assertThat(column).containsExactly("character varying", "6", "NO");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"LIGHT", "DARK", "SYSTEM"})
    @DisplayName("accepts each of the three themes")
    void givenTheme_whenStoringIt_thenAcceptIt(String theme) {
        // given
        flywayUpTo("latest").migrate();
        UUID id = insertCaretaker("52998224725");

        // when / then
        assertThatCode(() -> jdbc.update(
                        "update " + schema + ".caretaker set theme_preference = ? where id = ?", theme, id))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"AZUL", "dark", ""})
    @DisplayName("rejects what is not one of the three themes in capitals")
    void givenTextThatIsNotATheme_whenStoringIt_thenRejectIt(String theme) {
        // given
        flywayUpTo("latest").migrate();
        UUID id = insertCaretaker("52998224725");

        // when / then
        assertThatThrownBy(() -> jdbc.update(
                        "update " + schema + ".caretaker set theme_preference = ? where id = ?", theme, id))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
