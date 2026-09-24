package io.github.ovyx.shared.infrastructure;

import io.github.ovyx.shared.application.Command;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Query;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.transaction.support.TransactionOperations;

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

    private final TransactionOperations transaction;

    public SpringBeanDispatcher(
        final List<CommandHandler<?, ?>> commandHandlers,
        final List<QueryHandler<?, ?>> queryHandlers,
        final TransactionOperations transaction
    ) {
        this.commandHandlers = index(commandHandlers, CommandHandler.class);
        this.queryHandlers = index(queryHandlers, QueryHandler.class);
        this.transaction = transaction;
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
        try {
            return inTransaction(handler, command);
        } catch (final DataIntegrityViolationException concurrentWrite) {
            // Duas escritas simultaneas passaram pela mesma verificacao, e o indice unico do banco
            // recusou a segunda. Numa transacao nova, o dominio ja enxerga a linha gravada e recusa
            // com o codigo da regra, em vez de a pessoa receber um 500. Uma tentativa so: se o banco
            // recusar de novo, o problema nao e a corrida.
            return inTransaction(handler, command);
        }
    }

    /**
     * Roda o comando numa transacao so, que confirma tambem quando o {@code Result} e falha: a
     * auditoria de uma entrada recusada precisa ficar gravada.
     *
     * <p>E o que da ao caso de uso uma fronteira: o que ele le para decidir e o que ele grava valem
     * juntos. Sem ela, cada metodo do repositorio abria a propria transacao, e duas inativacoes
     * simultaneas dos dois ultimos administradores contavam dois e gravavam as duas (FR-019).
     */
    private <R> Result<R> inTransaction(final CommandHandler<Command<R>, R> handler, final Command<R> command) {
        return transaction.execute(status -> handler.handle(command));
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
