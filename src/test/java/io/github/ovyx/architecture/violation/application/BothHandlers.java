package io.github.ovyx.architecture.violation.application;

import io.github.ovyx.shared.application.Command;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Query;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Um mesmo tratador servindo escrita e leitura. Escrita e leitura tem ciclos de vida, cargas e
 * modelos diferentes; uni-las na mesma classe e o comeco do servico que faz tudo. Existe apenas
 * para o autoteste da suite de arquitetura.
 */
public class BothHandlers
        implements CommandHandler<BothHandlers.SomeCommand, String>, QueryHandler<BothHandlers.SomeQuery, String> {

    public record SomeCommand(String value) implements Command<String> {}

    public record SomeQuery(String value) implements Query<String> {}

    @Override
    public Result<String> handle(SomeCommand command) {
        return Result.success(command.value());
    }

    @Override
    public Result<String> handle(SomeQuery query) {
        return Result.success(query.value());
    }
}
