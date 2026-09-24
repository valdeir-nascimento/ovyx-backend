package io.github.ovyx.identity.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.application.authentication.SignInCommand;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Result;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Entradas simultaneas acima do tamanho do pool de conexoes, contra PostgreSQL real (SC-001, FR-002).
 *
 * <p>A T278 abriu uma transacao por comando, e a auditoria e a contencao ainda abriam uma segunda,
 * em transacao propria. Com tantas entradas simultaneas quanto conexoes no pool, cada uma segurava
 * a primeira conexao e esperava a segunda ate o tempo limite: a revisao mediu 10 de 10 entradas
 * falhando depois de 30 s, com 500 no lugar da resposta generica, e a API inteira parada nesse
 * intervalo.
 */
@DisplayName("Concurrent sign-in")
class ConcurrentSignInIT extends IntegrationTestSupport {

    /** SC-001: a entrada responde em ate 10 segundos. */
    private static final Duration SIGN_IN_LIMIT = Duration.ofSeconds(10);

    @Autowired
    private Dispatcher dispatcher;

    @Autowired
    private DataSource dataSource;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private int poolSize() throws Exception {
        return dataSource.unwrap(HikariDataSource.class).getMaximumPoolSize();
    }

    /** Uma entrada com identificador e origem proprios, para a contencao nao bloquear nenhuma. */
    private CompletableFuture<Result<CaretakerId>> signInWhenReleased(CountDownLatch start, int attempt) {
        return CompletableFuture.supplyAsync(
                () -> {
                    awaitQuietly(start);
                    return dispatcher.dispatch(new SignInCommand(
                            "ninguem." + UUID.randomUUID().toString().substring(0, 8) + "@ovyx.com.br",
                            "SenhaErrada2026",
                            "10.0.1." + attempt));
                },
                executor);
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    @Test
    @DisplayName("refuses each of twice as many simultaneous failed sign-ins as the pool has connections, in time")
    void givenTwiceThePoolSizeOfSimultaneousFailedSignIns_whenSubmitted_thenRefuseEachAsInvalidCredentialsInTime()
            throws Exception {
        // given
        int attempts = 2 * poolSize();
        CountDownLatch start = new CountDownLatch(1);
        List<CompletableFuture<Result<CaretakerId>>> inFlight = IntStream.range(0, attempts)
                .mapToObj(attempt -> signInWhenReleased(start, attempt))
                .toList();

        // when
        long startedAt = System.nanoTime();
        start.countDown();
        CompletableFuture.allOf(inFlight.toArray(CompletableFuture[]::new)).get(60, TimeUnit.SECONDS);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        // then
        assertThat(inFlight)
                .extracting(CompletableFuture::join)
                .extracting(result -> result.error().code())
                .containsOnly("INVALID_CREDENTIALS")
                .hasSize(attempts);
        assertThat(elapsed).isLessThan(SIGN_IN_LIMIT);
    }
}
