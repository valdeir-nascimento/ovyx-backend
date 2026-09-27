package io.github.ovyx.production.domain.model;

/**
 * Situacao da racao do relatorio (R-008 da 004), como a da producao: a regra mora aqui, e o agregado e a
 * leitura a usam.
 */
public enum FeedStatus {

    /** Toda gaiola do relatorio tem a racao lancada. */
    COMPLETE,

    /** Alguma gaiola ainda sem racao. */
    PENDING;

    /** Completa sem gaiola pendente; pendente com qualquer uma. */
    public static FeedStatus of(int pendingCages) {
        return pendingCages == 0 ? COMPLETE : PENDING;
    }
}
