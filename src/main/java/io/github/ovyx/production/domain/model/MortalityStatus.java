package io.github.ovyx.production.domain.model;

/**
 * Situacao da mortalidade do relatorio (data-model.md, Situacoes derivadas). A regra mora aqui, e o
 * agregado e a leitura a usam, para as duas nao divergirem.
 */
public enum MortalityStatus {

    /** Ha morte ou descarte lancado, ou a confirmacao de dia sem ocorrencia. */
    RECORDED,

    /** Nenhuma ocorrencia e nenhuma confirmacao. */
    PENDING;

    /** Lancada com ocorrencia ou com a confirmacao de dia sem ocorrencia; pendente sem nenhuma das duas. */
    public static MortalityStatus of(boolean noMortalityConfirmed, int removedBirds) {
        return noMortalityConfirmed || removedBirds > 0 ? RECORDED : PENDING;
    }
}
