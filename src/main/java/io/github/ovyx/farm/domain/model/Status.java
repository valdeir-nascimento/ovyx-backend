package io.github.ovyx.farm.domain.model;

/**
 * Situacao de um setor ou de uma gaiola.
 *
 * <p>Nada e apagado (FR-012): o que sai de uso fica inativo, e continua consultavel com todos os
 * dados que tinha.
 */
public enum Status {

    /** Em uso: conta nos totais e aparece nas listas de uso corrente. */
    ACTIVE,

    /** Fora de uso, sem perder o historico. */
    INACTIVE
}
