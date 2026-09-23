package io.github.ovyx.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.shared.application.Command;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Query;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

    @Test
    @DisplayName("Routes a command to the handler declared for its type")
    void givenRegisteredCommandHandler_whenDispatching_thenReturnWhatTheHandlerProduced() {
        // given
        SpringBeanDispatcher dispatcher = new SpringBeanDispatcher(List.of(new GreetHandler()), List.of());

        // when
        Result<String> result = dispatcher.dispatch(new Greet("Maria"));

        // then
        assertThat(result.value()).isEqualTo("olá, Maria");
    }

    @Test
    @DisplayName("Routes a query to the handler declared for its type")
    void givenRegisteredQueryHandler_whenAsking_thenReturnWhatTheHandlerProduced() {
        // given
        SpringBeanDispatcher dispatcher = new SpringBeanDispatcher(List.of(), List.of(new AskHandler()));

        // when
        Result<String> result = dispatcher.ask(new Ask("Maria"));

        // then
        assertThat(result.value()).isEqualTo("quem é Maria?");
    }

    @Test
    @DisplayName("Refuses a command nobody handles, naming the type")
    void givenCommandWithoutHandler_whenDispatching_thenThrowNamingTheCommandType() {
        // given
        SpringBeanDispatcher dispatcher = new SpringBeanDispatcher(List.of(), List.of());

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
        SpringBeanDispatcher dispatcher = new SpringBeanDispatcher(List.of(), List.of());

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
        ThrowingAsk building = () -> new SpringBeanDispatcher(duplicated, List.of());

        // then
        assertThatThrownBy(building::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mais de um handler")
                .hasMessageContaining("GreetHandler");
    }

    /** Pequeno apoio para manter a chamada sob teste no bloco {@code when}. */
    @FunctionalInterface
    private interface ThrowingAsk {
        void run();
    }
}
