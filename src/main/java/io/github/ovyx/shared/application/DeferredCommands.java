package io.github.ovyx.shared.application;

/**
 * Porta para executar um comando depois, fora da requisicao (R-003 da 012).
 *
 * <p>Serve ao que nao pode atrasar nem denunciar a resposta: o pedido de recuperacao da senha responde igual e no
 * mesmo tempo para qualquer conta, e procurar a conta e enviar o e-mail acontecem depois.
 *
 * <p>O comando roda pelo mesmo {@link Dispatcher}, com o mesmo tratador e a mesma transacao de qualquer comando, so
 * que mais tarde. Duas garantias:
 *
 * <ul>
 *   <li>pedido dentro de uma transacao, ele so roda depois que ela confirmar; se ela for desfeita, ou repetida pelo
 *       despachante, o que ela pediu nunca roda. Assim o comando repetido nao envia o mesmo e-mail duas vezes;
 *   <li>a falha do comando, ou a fila cheia, nunca volta para quem pediu: vai para o log, sem os dados do comando.
 * </ul>
 */
public interface DeferredCommands {

    /** Executa o comando depois, e depois da confirmacao da transacao corrente, se houver uma. */
    void submit(Command<?> command);
}
