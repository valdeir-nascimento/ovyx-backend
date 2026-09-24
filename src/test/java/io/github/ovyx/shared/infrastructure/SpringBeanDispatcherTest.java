package io.github.ovyx.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.shared.application.Command;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Query;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
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
     * Transacao de mentira que roda o trabalho e, nas primeiras vezes, recusa a confirmacao como o
     * banco recusaria uma escrita concorrente.
     */
    private static final class RefusingCommits implements TransactionOperations {
        private int refusalsLeft;
        private int transactions;

        private RefusingCommits(int refusals) {
            this.refusalsLeft = refusals;
        }

        @Override
        public <T> T execute(TransactionCallback<T> action) {
            transactions++;
            T result = action.doInTransaction(null);
            if (refusalsLeft > 0) {
                refusalsLeft--;
                throw new DataIntegrityViolationException("ux_caretaker_email_active");
            }
            return result;
        }
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

    /** Pequeno apoio para manter a chamada sob teste no bloco {@code when}. */
    @Test
    @DisplayName("Runs each command inside one transaction, so what it reads and what it writes go together")
    void givenCommand_whenDispatching_thenRunItInsideOneTransaction() {
        // given
        RefusingCommits transaction = new RefusingCommits(0);
        SpringBeanDispatcher dispatcher = new SpringBeanDispatcher(List.of(new GreetHandler()), List.of(), transaction);

        // when
        dispatcher.dispatch(new Greet("Maria"));

        // then
        assertThat(transaction.transactions).isEqualTo(1);
    }

    @Test
    @DisplayName("Tries a command once more when the database refuses a concurrent write")
    void givenDatabaseRefusingTheFirstCommit_whenDispatching_thenRunTheCommandAgainInANewTransaction() {
        // given
        // Duas escritas simultaneas passam pela mesma verificacao, e o indice unico recusa a segunda.
        // Na nova tentativa, o dominio ja enxerga a linha gravada e recusa com o codigo da regra.
        CountingGreetHandler handler = new CountingGreetHandler();
        SpringBeanDispatcher dispatcher =
                new SpringBeanDispatcher(List.of(handler), List.of(), new RefusingCommits(1));

        // when
        Result<String> result = dispatcher.dispatch(new Greet("Maria"));

        // then
        assertThat(result.value()).isEqualTo("olá, Maria (2)");
    }

    @Test
    @DisplayName("Gives up when the database refuses the second attempt too")
    void givenDatabaseRefusingBothCommits_whenDispatching_thenLetTheRefusalThrough() {
        // given
        SpringBeanDispatcher dispatcher =
                new SpringBeanDispatcher(List.of(new GreetHandler()), List.of(), new RefusingCommits(2));

        // when
        ThrowingAsk dispatching = () -> dispatcher.dispatch(new Greet("Maria"));

        // then
        assertThatThrownBy(dispatching::run).isInstanceOf(DataIntegrityViolationException.class);
    }

    @FunctionalInterface
    private interface ThrowingAsk {
        void run();
    }
}
