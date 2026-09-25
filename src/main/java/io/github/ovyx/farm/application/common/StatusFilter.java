package io.github.ovyx.farm.application.common;

/**
 * Filtro de situacao das listas de setores e de gaiolas (FR-005, FR-010; R-013).
 *
 * <p>E consulta, e nao dominio: o dominio so conhece ativo e inativo, e "todos" e um jeito de ler.
 * Fica no subpacote transversal da aplicacao, {@code common}, porque setores e gaiolas o usam.
 */
public enum StatusFilter {

    /** So os ativos: o padrao das listas de uso corrente. */
    ACTIVE,

    /** So os inativos, consultaveis com todos os dados que tinham (FR-012). */
    INACTIVE,

    /** Ativos e inativos. */
    ALL
}
