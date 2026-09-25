package io.github.ovyx.shared.application;

/**
 * Executa exatamente um caso de uso de escrita.
 *
 * <p>
 * Um handler nunca chama outro handler: reuso de logica sobe para o dominio.
 * </p>
 *
 * <p>
 * O despachante roda cada comando numa transacao, e a confirma tambem quando o {@code Result} e
 * falha: e assim que a auditoria e a contagem da contencao de uma entrada recusada ficam gravadas.
 * Por isso um handler que devolve falha so pode ter gravado esse registro da tentativa — nunca um
 * agregado. Toda recusa acontece antes de qualquer gravacao do caso de uso.
 * </p>
 *
 * @param <C> tipo do comando tratado.
 * @param <R> tipo devolvido em caso de sucesso.
 */
public interface CommandHandler<C extends Command<R>, R> {

    Result<R> handle(C command);
}
