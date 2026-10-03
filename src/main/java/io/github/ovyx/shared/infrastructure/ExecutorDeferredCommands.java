package io.github.ovyx.shared.infrastructure;

import io.github.ovyx.shared.application.Command;
import io.github.ovyx.shared.application.DeferredCommands;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Result;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Os comandos adiados sobre um executor limitado (R-003 da 012).
 *
 * <p>Dentro de uma transacao, o envio espera a confirmacao dela ({@code afterCommit}); fora, vai direto ao executor.
 * O log diz so o tipo do comando e o codigo da falha: os comandos levam dados que nao podem aparecer em log, como o
 * codigo de um link de recuperacao.
 *
 * <p>O executor e da configuracao ({@code DeferredCommandsConfiguration}): poucas threads e fila curta, porque o que
 * passa por aqui e pouco e nao pode tomar as conexoes do banco das requisicoes.
 */
public class ExecutorDeferredCommands implements DeferredCommands {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorDeferredCommands.class);

    private final Dispatcher dispatcher;
    private final Executor executor;

    public ExecutorDeferredCommands(Dispatcher dispatcher, Executor executor) {
        this.dispatcher = dispatcher;
        this.executor = executor;
    }

    @Override
    public void submit(Command<?> command) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enqueue(command);
                }
            });
            return;
        }
        enqueue(command);
    }

    private void enqueue(Command<?> command) {
        String name = command.getClass().getSimpleName();
        try {
            executor.execute(() -> run(command, name));
        } catch (RejectedExecutionException full) {
            LOG.error("Comando adiado descartado, fila cheia: {}", name);
        }
    }

    private void run(Command<?> command, String name) {
        try {
            Result<?> result = dispatcher.dispatch(command);
            if (result.isFailure()) {
                LOG.warn("Comando adiado recusado: {} ({})", name, result.error().code());
            }
        } catch (RuntimeException failure) {
            // So a classe da excecao: a mensagem pode trazer dados do comando.
            LOG.error("Comando adiado falhou: {} ({})", name, failure.getClass().getSimpleName());
        }
    }
}
