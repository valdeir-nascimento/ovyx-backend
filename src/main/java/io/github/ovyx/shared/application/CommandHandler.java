package io.github.ovyx.shared.application;

/**
 * Executa exatamente um caso de uso de escrita.
 *
 * <p>
 * Um handler nunca chama outro handler: reuso de logica sobe para o dominio.
 * </p>
 *
 * @param <C> tipo do comando tratado.
 * @param <R> tipo devolvido em caso de sucesso.
 */
public interface CommandHandler<C extends Command<R>, R> {

    Result<R> handle(C command);
}
