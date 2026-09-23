package io.github.ovyx.shared.application;

/**
 * Natureza da falha de um caso de uso.
 *
 * <p>
 * E o unico ponto de traducao para status HTTP: a camada de aplicacao nao conhece
 * codigo de status, e a presentation nao conhece regra de negocio.
 * </p>
 */
public enum ErrorType {

    /** Entrada malformada ou fora do dominio de valores aceitos. */
    VALIDATION,

    /**
     * Credencial invalida ou sessao ausente.
     *
     * <p>Acrescentado ao kernel por este projeto: a entrada responde 401 por contrato, e o cliente
     * distingue "entre novamente" (401) de "voce nao pode" (403).
     */
    UNAUTHENTICATED,

    /** Recurso referenciado nao existe. */
    NOT_FOUND,

    /** Estado atual conflita com a operacao pedida. */
    CONFLICT,

    /** Operacao valida na forma, mas recusada por regra de negocio. */
    BUSINESS_RULE,

    /** Solicitante nao pode operar sobre o recurso. */
    FORBIDDEN
}
