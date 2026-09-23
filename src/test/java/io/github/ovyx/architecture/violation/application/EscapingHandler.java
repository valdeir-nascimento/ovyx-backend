package io.github.ovyx.architecture.violation.application;

import io.github.ovyx.architecture.violation.domain.RefusingRegistration;
import io.github.ovyx.shared.application.Command;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Tratador que chama um dominio capaz de recusar e nao captura a {@code DomainException}. A
 * excecao escaparia ate o tratador global, que ainda devolveria um 400 plausivel — e por isso o
 * defeito passa despercebido sem uma regra de arquitetura. O {@code Result} deixaria de ser o canal
 * de falha do caso de uso (principio IV).
 *
 * <p>Existe apenas para {@link io.github.ovyx.architecture.ArchitectureRulesSelfCheckTest} comprovar
 * que a suite reprova esta situacao.
 */
public class EscapingHandler implements CommandHandler<EscapingHandler.SomeCommand, String> {

    public record SomeCommand(String value) implements Command<String> {}

    @Override
    public Result<String> handle(SomeCommand command) {
        return Result.success(RefusingRegistration.of(command.value()));
    }
}
