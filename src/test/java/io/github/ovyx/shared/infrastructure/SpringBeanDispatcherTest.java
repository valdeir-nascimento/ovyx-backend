package io.github.ovyx.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.shared.application.Command;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Query;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Testes do indice de tratadores.
 *
 * <p>E ele que liga cada comando ao seu tratador. Um comando sem tratador, ou com dois, e falha de
 * montagem: precisa aparecer na subida, com o nome do tipo, e nao como {@code NullPointerException}
 * no meio de uma requisicao.
 */
@DisplayName("SpringBeanDispatcher")
class SpringBeanDispatcherTest {

    private record Greet(String name) implements Command<String> {}

    private record Ask(String name) implements Query<String> {}

    private static final class GreetHandler implements CommandHandler<Greet, String> {
        @Override
        public Result<String> handle(Greet command) {
            return Result.success("olá, " + command.name());
        }
    }

    private static final class AskHandler implements QueryHandler<Ask, String> {
        @Override
        public Result<String> handle(Ask query) {
            return Result.success("quem é " + query.name() + "?");
        }
    }

    private static final TransactionOperations NO_TRANSACTION = TransactionOperations.withoutTransaction();

    /** Conta as execucoes do tratador de saudacao. */
    private static final class CountingGreetHandler implements CommandHandler<Greet, String> {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public Result<String> handle(Greet command) {
            return Result.success("olá, " + command.name() + " (" + calls.incrementAndGet() + ")");
        }
    }

    /**
     * Transacao de mentira que roda o trabalho e, nas primeiras vezes, recusa a confirmacao com a
     * falha informada, como o banco recusaria.
     */
    private static final class RefusingCommits implements TransactionOperations {
        private final Supplier<RuntimeException> refusal;
        private int refusalsLeft;
        private int transactions;

        private RefusingCommits(int refusals, Supplier<RuntimeException> refusal) {
            this.refusalsLeft = refusals;
            this.refusal = refusal;
        }

        private static RefusingCommits none() {
            return new RefusingCommits(0, IllegalStateException::new);
        }

        @Override
        public <T> T execute(TransactionCallback<T> action) {
            transactions++;
            T result = action.doInTransaction(null);
            if (refusalsLeft > 0) {
                refusalsLeft--;
                throw refusal.get();
            }
            return result;
        }
    }

    /**
     * O indice unico recusando a segunda de duas escritas simultaneas, como chega de verdade: o SQLState
     * do PostgreSQL fica embaixo da excecao do Hibernate, e nao na causa direta.
     */
    private static DataIntegrityViolationException uniqueViolation() {
        return new DataIntegrityViolationException(
                "could not execute statement",
                new IllegalStateException(
                        "could not execute statement", new SQLException("duplicate key value", "23505")));
    }

    /** Uma restricao que recusaria de novo a cada tentativa: a regra de verificacao da coluna. */
    private static DataIntegrityViolationException checkViolation() {
        return new DataIntegrityViolationException(
                "could not execute statement", new SQLException("violates check constraint", "23514"));
    }

    @Test
    @DisplayName("Routes a command to the handler declared for its type")
    void givenRegisteredCommandHandler_whenDispatching_thenReturnWhatTheHandlerProduced() {
        // given
        SpringBeanDispatcher dispatcher = new SpringBeanDispatcher(List.of(new GreetHandler()), List.of(), NO_TRANSACTION);

        // when
        Result<String> result = dispatcher.dispatch(new Greet("Maria"));

        // then
        assertThat(result.value()).isEqualTo("olá, Maria");
    }

    @Test
    @DisplayName("Routes a query to the handler declared for its type")
    void givenRegisteredQueryHandler_whenAsking_thenReturnWhatTheHandlerProduced() {
        // given
        SpringBeanDispatcher dispatcher = new SpringBeanDispatcher(List.of(), List.of(new AskHandler()), NO_TRANSACTION);

        // when
        Result<String> result = dispatcher.ask(new Ask("Maria"));

        // then
        assertThat(result.value()).isEqualTo("quem é Maria?");
    }

    @Test
    @DisplayName("Refuses a command nobody handles, naming the type")
    void givenCommandWithoutHandler_whenDispatching_thenThrowNamingTheCommandType() {
        // given
        SpringBeanDispatcher dispatcher = new SpringBeanDispatcher(List.of(), List.of(), NO_TRANSACTION);

        // when
        ThrowingAsk dispatching = () -> dispatcher.dispatch(new Greet("Maria"));

        // then
        assertThatThrownBy(dispatching::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Greet");
    }

    @Test
    @DisplayName("Refuses a query nobody handles, naming the type")
    void givenQueryWithoutHandler_whenAsking_thenThrowNamingTheQueryType() {
        // given
        SpringBeanDispatcher dispatcher = new SpringBeanDispatcher(List.of(), List.of(), NO_TRANSACTION);

        // when
        ThrowingAsk asking = () -> dispatcher.ask(new Ask("Maria"));

        // then
        assertThatThrownBy(asking::run).isInstanceOf(IllegalStateException.class).hasMessageContaining("Ask");
    }

    @Test
    @DisplayName("Refuses two handlers for the same command instead of picking one silently")
    void givenTwoHandlersForTheSameCommand_whenBuildingTheIndex_thenThrowNamingBothHandlers() {
        // given
        List<CommandHandler<?, ?>> duplicated = List.of(new GreetHandler(), new GreetHandler());

        // when
        ThrowingAsk building = () -> new SpringBeanDispatcher(duplicated, List.of(), NO_TRANSACTION);

        // then
        assertThatThrownBy(building::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mais de um handler")
                .hasMessageContaining("GreetHandler");
    }

    @Test
    @DisplayName("Runs each command inside one transaction, so what it reads and what it writes go together")
    void givenCommand_whenDispatching_thenRunItInsideOneTransaction() {
        // given
        RefusingCommits transaction = RefusingCommits.none();
        SpringBeanDispatcher dispatcher = new SpringBeanDispatcher(List.of(new GreetHandler()), List.of(), transaction);

        // when
        dispatcher.dispatch(new Greet("Maria"));

        // then
        assertThat(transaction.transactions).isEqualTo(1);
    }

    private static Stream<Arguments> concurrentWrites() {
        return Stream.of(
                Arguments.of(
                        "a unique index refusing the second write",
                        (Supplier<RuntimeException>) SpringBeanDispatcherTest::uniqueViolation),
                Arguments.of(
                        "a duplicate key already translated by Spring",
                        (Supplier<RuntimeException>) () -> new DuplicateKeyException("ux_caretaker_email_active")),
                Arguments.of(
                        "a row changed since it was loaded",
                        (Supplier<RuntimeException>) () -> new OptimisticLockingFailureException("caretaker")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("concurrentWrites")
    @DisplayName("Tries a command once more when the database refuses a concurrent write")
    void givenDatabaseRefusingTheFirstCommitAsAConcurrentWrite_whenDispatching_thenRunTheCommandAgain(
            String situation, Supplier<RuntimeException> refusal) {
        // given
        // Duas escritas simultaneas passam pela mesma verificacao, ou partem da mesma copia do
        // registro. Na nova tentativa, o dominio decide sobre o estado que a outra acabou de gravar.
        CountingGreetHandler handler = new CountingGreetHandler();
        SpringBeanDispatcher dispatcher =
                new SpringBeanDispatcher(List.of(handler), List.of(), new RefusingCommits(1, refusal));

        // when
        Result<String> result = dispatcher.dispatch(new Greet("Maria"));

        // then
        assertThat(result.value()).isEqualTo("olá, Maria (2)");
    }

    @Test
    @DisplayName("Gives up when the database refuses the second attempt too")
    void givenDatabaseRefusingBothCommits_whenDispatching_thenLetTheRefusalThrough() {
        // given
        SpringBeanDispatcher dispatcher = new SpringBeanDispatcher(
                List.of(new GreetHandler()),
                List.of(),
                new RefusingCommits(2, SpringBeanDispatcherTest::uniqueViolation));

        // when
        ThrowingAsk dispatching = () -> dispatcher.dispatch(new Greet("Maria"));

        // then
        assertThatThrownBy(dispatching::run).isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Stream<Arguments> failuresThatAreNotConcurrentWrites() {
        return Stream.of(
                Arguments.of("a check constraint", (Supplier<RuntimeException>) SpringBeanDispatcherTest::checkViolation),
                Arguments.of("an unexpected error", (Supplier<RuntimeException>) IllegalStateException::new));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("failuresThatAreNotConcurrentWrites")
    @DisplayName("Does not try again a failure that would repeat, running the command only once")
    void givenFailureThatIsNotAConcurrentWrite_whenDispatching_thenLetItThroughWithoutRunningTheCommandAgain(
            String situation, Supplier<RuntimeException> refusal) {
        // given
        // Repetir uma falha deterministica so roda o comando inteiro de novo, a toa.
        CountingGreetHandler handler = new CountingGreetHandler();
        RuntimeException expected = refusal.get();
        SpringBeanDispatcher dispatcher =
                new SpringBeanDispatcher(List.of(handler), List.of(), new RefusingCommits(2, () -> expected));

        // when
        ThrowingAsk dispatching = () -> dispatcher.dispatch(new Greet("Maria"));

        // then
        assertThatThrownBy(dispatching::run).isSameAs(expected);
        assertThat(handler.calls).hasValue(1);
    }

    /** Pequeno apoio para manter a chamada sob teste no bloco {@code when}. */
    @FunctionalInterface
    private interface ThrowingAsk {
        void run();
    }
}
