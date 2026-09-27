package io.github.ovyx.architecture.sample.presentation;

import io.github.ovyx.architecture.sample.application.CatchingHandler;
import io.github.ovyx.shared.application.Result;

/**
 * Entrada correta, para o autoteste da suite de arquitetura.
 *
 * <p>Fala com o caso de uso, que captura a recusa e a devolve como {@code Failure}. Existe para
 * provar que a regra das camadas externas aprova o caminho certo, que passa pelo mesmo dominio que
 * recusa.
 */
public class HandlerCallingEndpoint {

    private final CatchingHandler handler;

    public HandlerCallingEndpoint(CatchingHandler handler) {
        this.handler = handler;
    }

    public Result<String> register(String raw) {
        // O map e o idioma natural de quem monta a resposta; nao pode contar como recusa.
        return handler.handle(new CatchingHandler.SomeCommand(raw)).map(String::trim);
    }
}
