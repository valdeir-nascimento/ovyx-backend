package io.github.ovyx.identity.domain.port;

/**
 * Porta de saida para a contencao da recuperacao de senha por origem (FR-015 da 012): os pedidos e os links invalidos
 * de uma origem sao contados numa janela, e a origem que passa do limite fica bloqueada por um tempo.
 *
 * <p>Como na contencao da entrada, a resposta a origem bloqueada nao muda: o pedido recebe o mesmo 202, e a
 * conferencia e a redefinicao, a mesma recusa do link invalido. Anunciar o bloqueio diria a quem sonda que o limite
 * existe e quando ele termina.
 */
public interface RecoveryThrottle {

    /** Se a origem esta bloqueada agora. */
    boolean isBlocked(String origin);

    /** Conta mais uma tentativa da origem, abrindo, reabrindo ou estourando a janela. */
    void registerAttempt(String origin);
}
