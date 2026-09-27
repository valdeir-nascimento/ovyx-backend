package io.github.ovyx.architecture.sample.application;

import io.github.ovyx.architecture.sample.domain.Amount;
import io.github.ovyx.architecture.sample.domain.CyclicRegistration;
import io.github.ovyx.architecture.sample.domain.Registration;
import io.github.ovyx.architecture.sample.domain.RefusingFormat;
import io.github.ovyx.architecture.sample.domain.RegistrationPolicy;
import io.github.ovyx.architecture.sample.domain.SampleRefusal;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.Command;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Tratador correto, para o autoteste da suite de arquitetura.
 *
 * <p>Percorre todos os caminhos de
 * {@link io.github.ovyx.architecture.violation.application.EscapingHandlers}, mas dentro de um
 * {@code try} que captura a recusa e a traduz em {@code Failure}, como o principio IV manda.
 *
 * <p>Existe para provar que a regra reprova o que deve e <strong>aprova</strong> o que esta certo:
 * uma regra que reprovasse sempre passaria no caso de rejeicao e seria inutil.
 */
public class CatchingHandler implements CommandHandler<CatchingHandler.SomeCommand, String> {

    public record SomeCommand(String value) implements Command<String> {}

    private final RegistrationPolicy policy;
    private final Registrar registrar = new Registrar();

    public CatchingHandler(RegistrationPolicy policy) {
        this.policy = policy;
    }

    @Override
    public Result<String> handle(SomeCommand command) {
        String raw = command.value();
        Result<String> registered;
        try {
            if (raw == null) {
                throw SampleRefusal.missingValue();
            }
            Supplier<String> registering = new Supplier<>() {
                @Override
                public String get() {
                    return Registration.of(raw);
                }
            };
            Supplier<Function<String, String>> formats = RefusingFormat::new;
            List<String> accepted = List.of(
                    formats.get().apply(raw),
                    Optional.ofNullable(raw).orElseThrow(SampleRefusal::new),
                    registering.get(),
                    CyclicRegistration.confirm(raw, 1),
                    Registration.of(raw),
                    Optional.of(raw).map(Registration::of).orElseThrow(),
                    new Amount(raw).value(),
                    Optional.of(raw).map(Amount::new).orElseThrow().value(),
                    policy.check(raw),
                    String.join(",", Registration.ofAll(List.of(raw))),
                    registrar.register(raw),
                    register(raw));
            registered = Result.success(String.join(",", accepted));
        } catch (DomainException refusal) {
            return Result.failure(ApplicationError.from(refusal, ErrorType.VALIDATION));
        }
        // Fora do try, de proposito: Result.map chama Function.apply, e uma implementacao de Function
        // que recusa, mas que este tratador nem instancia, nao pode contar como recusa aqui.
        return registered.map(String::trim);
    }

    /** Sem {@code catch}, de proposito: e avaliado no ponto em que {@code handle} o chama, dentro do try. */
    private String register(String raw) {
        return Registration.of(raw);
    }
}
