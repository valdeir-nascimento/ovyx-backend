package io.github.ovyx.shared.application;

/**
 * Executa exatamente um caso de uso de leitura.
 *
 * @param <Q> tipo da query tratada.
 * @param <R> tipo devolvido em caso de sucesso.
 */
public interface QueryHandler<Q extends Query<R>, R> {

    Result<R> handle(Q query);
}
