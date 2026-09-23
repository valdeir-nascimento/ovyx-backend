package io.github.ovyx.shared.application;

/**
 * Porta unica de entrada da camada de aplicacao.
 *
 * <p>
 * A presentation depende deste tipo, nunca dos handlers concretos. E a abstracao
 * de CQRS reutilizavel exigida pela constituicao: existe uma, compartilhada por
 * todas as features.
 * </p>
 */
public interface Dispatcher {

    <R> Result<R> dispatch(Command<R> command);

    <R> Result<R> ask(Query<R> query);
}
