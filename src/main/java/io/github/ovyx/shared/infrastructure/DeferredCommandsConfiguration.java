package io.github.ovyx.shared.infrastructure;

import io.github.ovyx.shared.application.DeferredCommands;
import io.github.ovyx.shared.application.Dispatcher;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * O executor dos comandos adiados (R-003 da 012).
 *
 * <p>Duas threads e fila de 100: o que passa por aqui sao pedidos de recuperacao de senha e avisos por e-mail, poucos,
 * e cada thread segura no maximo uma conexao do banco. A aplicacao, ao parar, espera o que ja estava na fila.
 *
 * <p>O executor nao e um bean: um {@code Executor} no contexto tomaria o lugar do executor padrao do Spring Boot, que
 * outras partes da aplicacao podem usar. Esta configuracao o cria, o entrega a porta e o encerra.
 */
@Configuration(proxyBeanMethods = false)
public class DeferredCommandsConfiguration implements DisposableBean {

    static final int THREADS = 2;
    static final int QUEUE_CAPACITY = 100;

    private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    public DeferredCommandsConfiguration() {
        executor.setCorePoolSize(THREADS);
        executor.setMaxPoolSize(THREADS);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("deferred-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
    }

    /**
     * O despachante entra por um proxy preguicoso: ele depende de todos os tratadores, e alguns tratadores dependem
     * desta porta. O proxy so procura o despachante quando o primeiro comando adiado roda.
     */
    @Bean
    DeferredCommands deferredCommands(@Lazy Dispatcher dispatcher) {
        return new ExecutorDeferredCommands(dispatcher, executor);
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }
}
