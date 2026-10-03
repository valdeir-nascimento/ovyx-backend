package io.github.ovyx.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.IntegrationTestSupport;
import java.util.List;
import java.util.Map;
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
 * A V13 sobre responsáveis que já existiam (data-model.md da 012): cada um fica sem link de recuperação, sem pedidos
 * contados e na geração de sessão zero; a auditoria passa a aceitar os resultados da recuperação.
 *
 * <p>O banco do contexto já está na última versão, e por isso o teste migra um schema próprio em dois passos: até a
 * V12, onde cadastra os responsáveis como a 011 os deixaria, e depois até a última. O schema é apagado no fim.
 */
@DisplayName("Password recovery migration")
class PasswordRecoveryMigrationIT extends IntegrationTestSupport {

    private static final String HASH = "a".repeat(64);

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

    private List<String> column(String table, String column) {
        return jdbc.queryForObject(
                "select data_type, coalesce(character_maximum_length::text, ''), is_nullable,"
                        + " coalesce(column_default, '') from information_schema.columns"
                        + " where table_schema = ? and table_name = ? and column_name = ?",
                (rs, row) -> List.of(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                schema,
                table,
                column);
    }

    private void insertAccessEvent(String outcome) {
        jdbc.update(
                "insert into " + schema + ".access_event (id, attempted_identifier, outcome, origin, occurred_at)"
                        + " values (?, 'maria.silva@ovyx.com.br', ?, '127.0.0.1', now())",
                UUID.randomUUID(),
                outcome);
    }

    @AfterEach
    void dropSchema() {
        jdbc.execute("drop schema if exists " + schema + " cascade");
    }

    @Test
    @DisplayName("leaves the caretakers registered before the migration without link, requests or new generation")
    void givenCaretakersRegisteredBeforeTheMigration_whenMigrating_thenLeaveThemWithoutRecoveryAndAtGenerationZero() {
        // given
        flywayUpTo("12").migrate();
        insertCaretaker("52998224725");
        insertCaretaker("11144477735");

        // when
        flywayUpTo("latest").migrate();

        // then
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select recovery_token_hash, recovery_expires_at, recovery_window_started_at,"
                        + " recovery_requests_in_window, session_generation from " + schema + ".caretaker");
        assertThat(rows).hasSize(2).allSatisfy(row -> {
            assertThat(row.get("recovery_token_hash")).isNull();
            assertThat(row.get("recovery_expires_at")).isNull();
            assertThat(row.get("recovery_window_started_at")).isNull();
            assertThat(((Number) row.get("recovery_requests_in_window")).intValue()).isZero();
            assertThat(((Number) row.get("session_generation")).intValue()).isZero();
        });
    }

    @Test
    @DisplayName("declares the new caretaker columns with their types, nullability and defaults")
    void givenMigratedSchema_whenReadingTheNewCaretakerColumns_thenFindTheirTypesNullabilityAndDefaults() {
        // given
        flywayUpTo("latest").migrate();

        // when / then
        assertThat(column("caretaker", "recovery_token_hash")).containsExactly("character", "64", "YES", "");
        assertThat(column("caretaker", "recovery_expires_at"))
                .containsExactly("timestamp with time zone", "", "YES", "");
        assertThat(column("caretaker", "recovery_window_started_at"))
                .containsExactly("timestamp with time zone", "", "YES", "");
        assertThat(column("caretaker", "recovery_requests_in_window")).containsExactly("smallint", "", "NO", "0");
        assertThat(column("caretaker", "session_generation")).containsExactly("integer", "", "NO", "0");
    }

    @Test
    @DisplayName("refuses a link digest without its expiry, and an expiry without its digest")
    void givenHalfALink_whenStoringIt_thenRejectIt() {
        // given
        flywayUpTo("latest").migrate();
        UUID id = insertCaretaker("52998224725");

        // when / then
        assertThatThrownBy(() -> jdbc.update(
                        "update " + schema + ".caretaker set recovery_token_hash = ? where id = ?", HASH, id))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "update " + schema + ".caretaker set recovery_expires_at = now() where id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> jdbc.update(
                        "update " + schema + ".caretaker set recovery_token_hash = ?, recovery_expires_at = now()"
                                + " where id = ?",
                        HASH,
                        id))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("refuses the same link digest on two caretakers")
    void givenLinkDigestOfAnotherCaretaker_whenStoringIt_thenRejectIt() {
        // given
        flywayUpTo("latest").migrate();
        UUID first = insertCaretaker("52998224725");
        UUID second = insertCaretaker("11144477735");
        jdbc.update(
                "update " + schema + ".caretaker set recovery_token_hash = ?, recovery_expires_at = now() where id = ?",
                HASH,
                first);

        // when / then
        assertThatThrownBy(() -> jdbc.update(
                        "update " + schema + ".caretaker set recovery_token_hash = ?, recovery_expires_at = now()"
                                + " where id = ?",
                        HASH,
                        second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = {-1, 4})
    @DisplayName("refuses a request count outside 0 to 3")
    void givenRequestCountOutsideTheLimit_whenStoringIt_thenRejectIt(int count) {
        // given
        flywayUpTo("latest").migrate();
        UUID id = insertCaretaker("52998224725");

        // when / then
        assertThatThrownBy(() -> jdbc.update(
                        "update " + schema + ".caretaker set recovery_requests_in_window = ? where id = ?", count, id))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("refuses a negative session generation")
    void givenNegativeSessionGeneration_whenStoringIt_thenRejectIt() {
        // given
        flywayUpTo("latest").migrate();
        UUID id = insertCaretaker("52998224725");

        // when / then
        assertThatThrownBy(() -> jdbc.update(
                        "update " + schema + ".caretaker set session_generation = -1 where id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("creates the recovery attempt table keyed by origin")
    void givenMigratedSchema_whenReadingTheRecoveryAttemptTable_thenFindItsColumns() {
        // given
        flywayUpTo("latest").migrate();

        // when / then
        assertThat(column("recovery_attempt", "origin")).containsExactly("character varying", "60", "NO", "");
        assertThat(column("recovery_attempt", "attempt_count").subList(0, 3)).containsExactly("integer", "", "NO");
        assertThat(column("recovery_attempt", "window_started_at"))
                .containsExactly("timestamp with time zone", "", "NO", "");
        assertThat(column("recovery_attempt", "blocked_until"))
                .containsExactly("timestamp with time zone", "", "YES", "");
        assertThatThrownBy(() -> jdbc.update(
                        "insert into " + schema + ".recovery_attempt (origin, attempt_count, window_started_at)"
                                + " values ('127.0.0.1', -1, now())"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "GRANTED", "INVALID_CREDENTIALS", "INACTIVE_CARETAKER", "THROTTLED", "SIGNED_OUT",
        "RECOVERY_LINK_SENT", "RECOVERY_UNKNOWN_EMAIL", "RECOVERY_INACTIVE", "RECOVERY_LIMITED",
        "RECOVERY_THROTTLED", "RECOVERY_DELIVERY_FAILED", "PASSWORD_RECOVERED", "RECOVERY_LINK_REFUSED"
    })
    @DisplayName("accepts each access outcome, old and new")
    void givenAccessOutcome_whenRecordingIt_thenAcceptIt(String outcome) {
        // given
        flywayUpTo("latest").migrate();

        // when / then
        assertThatCode(() -> insertAccessEvent(outcome)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("still rejects an outcome that does not exist")
    void givenUnknownOutcome_whenRecordingIt_thenRejectIt() {
        // given
        flywayUpTo("latest").migrate();

        // when / then
        assertThatThrownBy(() -> insertAccessEvent("RECOVERY_SOMETHING")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("documents the new columns, saying the link itself is never stored")
    void givenMigratedSchema_whenReadingTheColumnComments_thenFindThemAll() {
        // given
        flywayUpTo("latest").migrate();

        // when
        List<String> comments = jdbc.queryForList(
                "select col_description(c.oid, a.attnum) from pg_class c"
                        + " join pg_namespace n on n.oid = c.relnamespace"
                        + " join pg_attribute a on a.attrelid = c.oid"
                        + " where n.nspname = ? and c.relname = 'caretaker' and a.attname in"
                        + " ('recovery_token_hash', 'recovery_expires_at', 'recovery_window_started_at',"
                        + " 'recovery_requests_in_window', 'session_generation')",
                String.class,
                schema);

        // then
        assertThat(comments).hasSize(5).doesNotContainNull();
        assertThat(String.join(" ", comments)).contains("nunca");
    }
}
