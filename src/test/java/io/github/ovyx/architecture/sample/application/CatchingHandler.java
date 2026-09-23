package io.github.ovyx.architecture.sample.application;

import io.github.ovyx.architecture.violation.domain.RefusingRegistration;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.Command;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;

/**
 * Tratador correto, para o autoteste da suite de arquitetura.
 *
 * <p>Chama o mesmo dominio que {@link io.github.ovyx.architecture.violation.application.EscapingHandler}
 * chama, mas captura a recusa e a traduz em {@code Failure}, como o principio IV manda.
 *
 * <p>Existe para provar que a regra reprova o que deve e <strong>aprova</strong> o que esta certo:
 * uma regra que reprovasse sempre passaria no caso de rejeicao e seria inutil.
 */
public class CatchingHandler implements CommandHandler<CatchingHandler.SomeCommand, String> {

    public record SomeCommand(String value) implements Command<String> {}

    @Override
    public Result<String> handle(SomeCommand command) {
        try {
            return Result.success(RefusingRegistration.of(command.value()));
        } catch (DomainException refusal) {
            return Result.failure(ApplicationError.from(refusal, ErrorType.VALIDATION));
        }
    }
}
