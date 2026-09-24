package io.github.ovyx.identity.integration;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.randomValidCpf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.application.caretaker.DeactivateCaretakerCommand;
import io.github.ovyx.identity.application.caretaker.RegisterCaretakerCommand;
import io.github.ovyx.identity.application.caretaker.UpdateCaretakerCommand;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Result;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Duas escritas ao mesmo tempo contra PostgreSQL real (FR-016, FR-019, T271).
 *
 * <p>Decidir no agregado nao basta sem fronteira transacional: sem ela, cada metodo do repositorio
 * abria a propria transacao, e a revisao mediu 40 de 40 rodadas terminando sem nenhum administrador
 * ativo. Cada teste se repete porque uma corrida pode passar por sorte numa rodada so.
 */
@DisplayName("Caretaker concurrency")
class CaretakerConcurrencyIT extends IntegrationTestSupport {

    private static final String OTHER_ACTIVE_ADMINISTRATORS = """
        SELECT id
          FROM caretaker
         WHERE role = 'ADMINISTRATOR'
           AND status = 'ACTIVE'
           AND id NOT IN (?, ?)
        """;

    private static final String ACTIVE_ADMINISTRATORS = """
        SELECT count(*)
          FROM caretaker
         WHERE role = 'ADMINISTRATOR'
           AND status = 'ACTIVE'
        """;

    private static final String CARETAKERS_WITH_EMAIL = """
        SELECT count(*)
          FROM caretaker
         WHERE email = ?
        """;

    private static final String SET_STATUS = """
        UPDATE caretaker
           SET status = ?
         WHERE id = ?
        """;

    @Autowired
    private Dispatcher dispatcher;

    @Autowired
    private CaretakerRepository caretakerRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private Clock clock;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private ExecutorService executor;

    /** Administradores inativados para isolar os dois ultimos; voltam a ativa depois de cada rodada. */
    private List<UUID> setAside = List.of();

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        setAside.forEach(id -> jdbc.update(SET_STATUS, "ACTIVE", id));
    }

    private Caretaker savedAdministrator() {
        Caretaker administrator = aUniqueCaretaker()
                .withRole(Role.ADMINISTRATOR)
                .withHasher(passwordHasher)
                .withRoster(caretakerRepository)
                .withClock(clock)
                .build();
        caretakerRepository.save(administrator);
        return administrator;
    }

    /** Deixa os dois como os unicos administradores ativos do banco compartilhado. */
    private void makeTheOnlyActiveAdministrators(Caretaker first, Caretaker second) {
        setAside = jdbc.queryForList(
                OTHER_ACTIVE_ADMINISTRATORS, UUID.class, first.id().value(), second.id().value());
        setAside.forEach(id -> jdbc.update(SET_STATUS, "INACTIVE", id));
    }

    /** Roda as duas acoes ao mesmo tempo: as duas esperam o mesmo sinal para partir. */
    private <T> List<T> simultaneously(Callable<T> first, Callable<T> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Future<T> one = executor.submit(() -> {
            start.await();
            return first.call();
        });
        Future<T> other = executor.submit(() -> {
            start.await();
            return second.call();
        });
        start.countDown();
        return List.of(one.get(60, TimeUnit.SECONDS), other.get(60, TimeUnit.SECONDS));
    }

    private static RegisterCaretakerCommand registration(String email) {
        String mobilePhone = "919" + String.format("%08d", Math.floorMod(UUID.randomUUID().hashCode(), 100_000_000));
        return new RegisterCaretakerCommand(
                "Cadastro Simultaneo", randomValidCpf(), email, mobilePhone, "AviarioSul2026", Role.USER);
    }

    private static UpdateCaretakerCommand demotion(Caretaker administrator) {
        return new UpdateCaretakerCommand(
                administrator.id(),
                administrator.fullName().value(),
                administrator.cpf().value(),
                administrator.email().value(),
                administrator.mobilePhone().value(),
                Role.USER);
    }

    private long count(String sql, Object... args) {
        Long result = jdbc.queryForObject(sql, Long.class, args);
        return result == null ? 0 : result;
    }

    @RepeatedTest(value = 10, name = "rodada {currentRepetition} de {totalRepetitions}")
    @DisplayName("of two simultaneous deactivations of the last two administrators, only one goes through (FR-019)")
    void givenTheLastTwoActiveAdministrators_whenDeactivatingBothAtOnce_thenKeepOneActive() throws Exception {
        // given
        Caretaker first = savedAdministrator();
        Caretaker second = savedAdministrator();
        makeTheOnlyActiveAdministrators(first, second);

        // when
        List<Result<CaretakerId>> results = simultaneously(
                () -> dispatcher.dispatch(new DeactivateCaretakerCommand(first.id())),
                () -> dispatcher.dispatch(new DeactivateCaretakerCommand(second.id())));

        // then
        assertThat(results).filteredOn(Result::isSuccess).hasSize(1);
        assertThat(results)
                .filteredOn(Result::isFailure)
                .extracting(result -> result.error().code())
                .containsExactly("LAST_ADMINISTRATOR");
        assertThat(count(ACTIVE_ADMINISTRATORS)).isEqualTo(1);
    }

    @RepeatedTest(value = 10, name = "rodada {currentRepetition} de {totalRepetitions}")
    @DisplayName("of two simultaneous demotions of the last two administrators, only one goes through (FR-019)")
    void givenTheLastTwoActiveAdministrators_whenDemotingBothAtOnce_thenKeepOneAdministrator() throws Exception {
        // given
        // O rebaixamento pergunta a mesma contagem que a inativacao: sem a trava, os dois viam dois
        // administradores e gravavam os dois como usuario comum.
        Caretaker first = savedAdministrator();
        Caretaker second = savedAdministrator();
        makeTheOnlyActiveAdministrators(first, second);

        // when
        List<Result<CaretakerId>> results = simultaneously(
                () -> dispatcher.dispatch(demotion(first)), () -> dispatcher.dispatch(demotion(second)));

        // then
        assertThat(results).filteredOn(Result::isSuccess).hasSize(1);
        assertThat(results)
                .filteredOn(Result::isFailure)
                .extracting(result -> result.error().code())
                .containsExactly("LAST_ADMINISTRATOR");
        assertThat(count(ACTIVE_ADMINISTRATORS)).isEqualTo(1);
    }

    @RepeatedTest(value = 10, name = "rodada {currentRepetition} de {totalRepetitions}")
    @DisplayName("of two simultaneous registrations with the same email, the second is a conflict, not an error (T271)")
    void givenTwoRegistrationsWithTheSameEmail_whenSubmittedAtOnce_thenRegisterOneAndRefuseTheOther() throws Exception {
        // given
        // As duas passam pela verificacao antes de qualquer uma gravar; o indice unico recusa a
        // segunda, e a nova tentativa do despachante devolve o conflito do proprio dominio.
        String email = "simultaneo." + UUID.randomUUID().toString().substring(0, 8) + "@ovyx.com.br";

        // when
        List<Result<CaretakerId>> results = simultaneously(
                () -> dispatcher.dispatch(registration(email)), () -> dispatcher.dispatch(registration(email)));

        // then
        assertThat(results).filteredOn(Result::isSuccess).hasSize(1);
        assertThat(results)
                .filteredOn(Result::isFailure)
                .extracting(result -> result.error().code())
                .containsExactly("EMAIL_ALREADY_IN_USE");
        assertThat(count(CARETAKERS_WITH_EMAIL, email)).isEqualTo(1);
    }
}
