package io.github.ovyx.production.domain.model;

/**
 * Situacao da producao do relatorio (data-model.md, Situacoes derivadas). A regra mora aqui, e o agregado
 * e a leitura a usam, para as duas nao divergirem.
 */
public enum ProductionStatus {

    /** Toda gaiola do relatorio tem a producao lancada. */
    COMPLETE,

    /** Alguma gaiola ainda sem producao. */
    PENDING;

    /** Completa sem gaiola pendente; pendente com qualquer uma. */
    public static ProductionStatus of(int pendingCages) {
        return pendingCages == 0 ? COMPLETE : PENDING;
    }
}
