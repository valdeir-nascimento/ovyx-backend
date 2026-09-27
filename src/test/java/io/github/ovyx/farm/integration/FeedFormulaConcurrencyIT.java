package io.github.ovyx.farm.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.farm.application.formula.RegisterFeedFormulaCommand;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Result;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Dois cadastros de fórmula com o mesmo nome ao mesmo tempo, contra PostgreSQL real (spec da 004, casos
 * de borda; R-011).
 *
 * <p>Os dois passam pela checagem do agregado antes de qualquer um gravar; o índice único recusa o
 * segundo, e o despachante repete o comando, que então encontra o conflito e recusa com o código certo.
 * O teste se repete porque uma corrida pode passar por sorte numa rodada só.
 */
@DisplayName("Feed formula concurrency")
class FeedFormulaConcurrencyIT extends IntegrationTestSupport {

    @Autowired
    private Dispatcher dispatcher;

    @Autowired
    private JdbcTemplate jdbc;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    /** Roda as duas ações ao mesmo tempo: as duas esperam o mesmo sinal para partir. */
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

    @RepeatedTest(value = 10, name = "round {currentRepetition} of {totalRepetitions}")
    @DisplayName("of two simultaneous registrations of the same name, one goes through and the other is a conflict")
    void givenTheSameName_whenRegisteringTwoFormulasAtOnce_thenAcceptOneAndRefuseTheOtherAsNameInUse()
            throws Exception {
        // given
        String name = "Corrida " + UUID.randomUUID().toString().substring(0, 8);
        RegisterFeedFormulaCommand first = new RegisterFeedFormulaCommand(name, "2,85", "28", null);
        RegisterFeedFormulaCommand second = new RegisterFeedFormulaCommand(name.toUpperCase(), "3,10", "24", null);

        // when
        List<Result<FeedFormulaId>> results =
                simultaneously(() -> dispatcher.dispatch(first), () -> dispatcher.dispatch(second));

        // then
        assertThat(results).filteredOn(Result::isSuccess).hasSize(1);
        assertThat(results)
                .filteredOn(Result::isFailure)
                .extracting(result -> result.error().code())
                .containsExactly("FEED_FORMULA_NAME_IN_USE");
        assertThat(jdbc.queryForObject(
                        "select count(*) from feed_formula where lower(name) = lower(?)", Integer.class, name))
                .isEqualTo(1);
    }
}
