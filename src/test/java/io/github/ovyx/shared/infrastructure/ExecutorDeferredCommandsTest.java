package io.github.ovyx.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.Command;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Query;
import io.github.ovyx.shared.application.Result;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Os comandos adiados (R-003 da 012): rodam fora da requisição, só depois que a transação de quem os pediu confirmar,
 * e nunca levam a falha de volta a quem os pediu.
 */
@DisplayName("ExecutorDeferredCommands")
@ExtendWith(OutputCaptureExtension.class)
class ExecutorDeferredCommandsTest {

    /** Comando com um dado que não pode aparecer no log. */
    private record Deliver(String secret) implements Command<Void> {}

    private final ExecutorService pool = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "deferred-test"));

    /** O despachante falso: anota a thread e o comando, e responde o que o teste mandar. */
    private static final class RecordingDispatcher implements Dispatcher {
        private final List<String> threads = new CopyOnWriteArrayList<>();
        private final List<Command<?>> dispatched = new CopyOnWriteArrayList<>();
        private final CountDownLatch done = new CountDownLatch(1);
        private final Function<Command<?>, Result<?>> answer;

        RecordingDispatcher(Function<Command<?>, Result<?>> answer) {
            this.answer = answer;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> Result<R> dispatch(Command<R> command) {
            threads.add(Thread.currentThread().getName());
            dispatched.add(command);
            try {
                return (Result<R>) answer.apply(command);
            } finally {
                done.countDown();
            }
        }

        @Override
        public <R> Result<R> ask(Query<R> query) {
            throw new UnsupportedOperationException();
        }

        boolean awaitDone() throws InterruptedException {
            return done.await(5, TimeUnit.SECONDS);
        }
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        pool.shutdownNow();
    }

    @Test
    @DisplayName("dispatches the command on another thread when there is no transaction")
    void givenNoTransaction_whenSubmitting_thenDispatchOnAnotherThread() throws InterruptedException {
        // given
        RecordingDispatcher dispatcher = new RecordingDispatcher(command -> Result.success(null));
        ExecutorDeferredCommands deferred = new ExecutorDeferredCommands(dispatcher, pool);
        Deliver command = new Deliver("3q2-7wq9");

        // when
        deferred.submit(command);

        // then
        assertThat(dispatcher.awaitDone()).isTrue();
        assertThat(dispatcher.dispatched).containsExactly(command);
        assertThat(dispatcher.threads).containsExactly("deferred-test");
    }

    @Test
    @DisplayName("waits for the transaction of whoever submitted to commit before dispatching")
    void givenActiveTransaction_whenSubmitting_thenDispatchOnlyAfterTheCommit() throws InterruptedException {
        // given
        RecordingDispatcher dispatcher = new RecordingDispatcher(command -> Result.success(null));
        ExecutorDeferredCommands deferred = new ExecutorDeferredCommands(dispatcher, pool);
        TransactionSynchronizationManager.initSynchronization();

        // when
        deferred.submit(new Deliver("3q2-7wq9"));

        // then
        assertThat(dispatcher.dispatched).isEmpty();
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        assertThat(dispatcher.awaitDone()).isTrue();
        assertThat(dispatcher.dispatched).hasSize(1);
    }

    @Test
    @DisplayName("never dispatches what a transaction that rolled back submitted")
    void givenTransactionThatRollsBack_whenSubmitting_thenNeverDispatch() throws InterruptedException {
        // given
        RecordingDispatcher dispatcher = new RecordingDispatcher(command -> Result.success(null));
        ExecutorDeferredCommands deferred = new ExecutorDeferredCommands(dispatcher, pool);
        TransactionSynchronizationManager.initSynchronization();

        // when
        deferred.submit(new Deliver("3q2-7wq9"));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        // then
        pool.shutdown();
        assertThat(pool.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
        assertThat(dispatcher.dispatched).isEmpty();
    }

    @Test
    @DisplayName("logs a failed result without the data of the command")
    void givenHandlerThatFails_whenDispatching_thenLogTheCodeWithoutTheCommandData(CapturedOutput output)
            throws InterruptedException {
        // given
        RecordingDispatcher dispatcher = new RecordingDispatcher(command ->
                Result.failure(ApplicationError.of(ErrorType.BUSINESS_RULE, "SOME_RULE", "Recusado.")));
        ExecutorDeferredCommands deferred = new ExecutorDeferredCommands(dispatcher, pool);

        // when
        deferred.submit(new Deliver("segredo-do-link"));

        // then
        assertThat(dispatcher.awaitDone()).isTrue();
        pool.shutdown();
        assertThat(pool.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
        assertThat(output.getAll()).contains("Deliver").contains("SOME_RULE").doesNotContain("segredo-do-link");
    }

    @Test
    @DisplayName("logs a handler that throws, without the data of the command, and keeps working")
    void givenHandlerThatThrows_whenDispatching_thenLogItAndKeepWorking(CapturedOutput output)
            throws InterruptedException {
        // given
        RecordingDispatcher dispatcher = new RecordingDispatcher(command -> {
            throw new IllegalStateException("falhou");
        });
        ExecutorDeferredCommands deferred = new ExecutorDeferredCommands(dispatcher, pool);

        // when
        deferred.submit(new Deliver("segredo-do-link"));

        // then
        assertThat(dispatcher.awaitDone()).isTrue();
        pool.shutdown();
        assertThat(pool.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
        assertThat(output.getAll()).contains("Deliver").doesNotContain("segredo-do-link");
    }

    @Test
    @DisplayName("logs a full queue instead of handing the failure to whoever submitted")
    void givenFullQueue_whenSubmitting_thenLogAndReturnNormally(CapturedOutput output) {
        // given
        Executor full = runnable -> {
            throw new RejectedExecutionException("cheia");
        };
        ExecutorDeferredCommands deferred =
                new ExecutorDeferredCommands(new RecordingDispatcher(command -> Result.success(null)), full);

        // when / then
        assertThatCode(() -> deferred.submit(new Deliver("segredo-do-link"))).doesNotThrowAnyException();
        assertThat(output.getAll()).contains("Deliver").doesNotContain("segredo-do-link");
    }
}
