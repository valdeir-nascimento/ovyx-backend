package io.github.ovyx.identity.domain.port;

/**
 * Porta de saida para a contencao de tentativas repetidas de acesso.
 *
 * <p>Janela deslizante por combinacao de identificador tentado e origem. O identificador recebido e
 * sempre a forma canonica de {@code AccessIdentifier}; a implementacao o usa como veio, sem
 * normalizacao propria.
 *
 * <p>Ponto importante: quando {@link #isBlocked} responde verdadeiro, a resposta ao cliente
 * continua sendo a mesma mensagem generica de credencial invalida. Anunciar o bloqueio confirmaria
 * a quem sonda que o identificador existe, quebrando FR-002 — e ainda ofereceria um jeito barato
 * de descobrir contas validas.
 */
public interface SignInThrottle {

    /** Indica se esta combinacao de identificador e origem esta bloqueada no momento. */
    boolean isBlocked(String attemptedIdentifier, String origin);

    /** Registra mais uma falha, abrindo ou estendendo a janela. */
    void registerFailure(String attemptedIdentifier, String origin);

    /** Zera a contagem apos autenticacao bem-sucedida. */
    void clear(String attemptedIdentifier, String origin);
}
