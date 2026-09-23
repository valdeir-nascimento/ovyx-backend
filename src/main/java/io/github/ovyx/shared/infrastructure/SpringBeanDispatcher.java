package io.github.ovyx.shared.infrastructure;

import io.github.ovyx.shared.application.Command;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Query;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dispatcher apoiado no container do Spring.
 *
 * <p>
 * O indice de handlers e montado uma unica vez na inicializacao e guardado em mapa
 * imutavel, para nao pagar lookup por tipo generico a cada chamada. Handler
 * ausente ou duplicado falha no startup, nao em producao.
 * </p>
 */
@Component
public class SpringBeanDispatcher implements Dispatcher {

    private final Map<Class<?>, CommandHandler<?, ?>> commandHandlers;

    private final Map<Class<?>, QueryHandler<?, ?>> queryHandlers;

    public SpringBeanDispatcher(
        final List<CommandHandler<?, ?>> commandHandlers,
        final List<QueryHandler<?, ?>> queryHandlers
    ) {
        this.commandHandlers = index(commandHandlers, CommandHandler.class);
        this.queryHandlers = index(queryHandlers, QueryHandler.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Result<R> dispatch(final Command<R> command) {
        final CommandHandler<Command<R>, R> handler =
            (CommandHandler<Command<R>, R>) commandHandlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalStateException(
                "Nenhum CommandHandler registrado para " + command.getClass().getName()
            );
        }
        return handler.handle(command);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Result<R> ask(final Query<R> query) {
        final QueryHandler<Query<R>, R> handler =
            (QueryHandler<Query<R>, R>) queryHandlers.get(query.getClass());
        if (handler == null) {
            throw new IllegalStateException(
                "Nenhum QueryHandler registrado para " + query.getClass().getName()
            );
        }
        return handler.handle(query);
    }

    /**
     * Indexa cada handler pelo tipo de mensagem que ele trata.
     *
     * <p>
     * {@link ClassUtils#getUserClass} desembrulha proxy CGLIB, senao um handler
     * anotado com {@code @Transactional} perderia o generico.
     * </p>
     */
    private static <H> Map<Class<?>, H> index(
        final List<H> handlers,
        final Class<?> handlerInterface
    ) {
        final Map<Class<?>, H> byMessageType = new HashMap<>();
        for (final H handler : handlers) {
            final Class<?> userClass = ClassUtils.getUserClass(handler);
            final Class<?> messageType = ResolvableType.forClass(userClass)
                .as(handlerInterface)
                .getGeneric(0)
                .resolve();

            if (messageType == null) {
                throw new IllegalStateException(
                    "Nao foi possivel resolver o tipo tratado por " + userClass.getName()
                        + "; declare o generico explicitamente"
                );
            }

            final H previous = byMessageType.put(messageType, handler);
            if (previous != null) {
                throw new IllegalStateException(
                    "Mais de um handler registrado para " + messageType.getName() + ": "
                        + ClassUtils.getUserClass(previous).getName() + " e " + userClass.getName()
                );
            }
        }
        return Map.copyOf(byMessageType);
    }
}
