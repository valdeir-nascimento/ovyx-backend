package io.github.ovyx.architecture.violation.application;

import io.github.ovyx.architecture.sample.application.Registrar;
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
 * VIOLACOES DELIBERADAS — nao imite este codigo.
 *
 * <p>Cada tratador deixa escapar, por um caminho diferente, a recusa do dominio de amostra. A
 * excecao chegaria ao tratador global, que responde 409 "Operacao recusada": a fatia parece
 * funcionar, e o {@code Result} deixa de ser o canal de falha do caso de uso (principio IV).
 *
 * <p>Existem apenas para {@link io.github.ovyx.architecture.ArchitectureRulesSelfCheckTest}
 * comprovar que a regra enxerga cada caminho — um tratador por caminho, para que um caminho cego
 * nao se esconda atras de outro.
 */
public final class EscapingHandlers {

    private EscapingHandlers() {}

    public record SomeCommand(String value) implements Command<String> {}

    /** Chama o dominio diretamente. */
    public static class ByDirectCall implements CommandHandler<SomeCommand, String> {

        @Override
        public Result<String> handle(SomeCommand command) {
            return Result.success(Registration.of(command.value()));
        }
    }

    /** Passa o dominio por referencia de metodo. */
    public static class ByMethodReference implements CommandHandler<SomeCommand, String> {

        @Override
        public Result<String> handle(SomeCommand command) {
            return Result.success(
                    Optional.of(command.value()).map(Registration::of).orElseThrow());
        }
    }

    /** Constroi um objeto de valor que recusa no construtor. */
    public static class ByConstruction implements CommandHandler<SomeCommand, String> {

        @Override
        public Result<String> handle(SomeCommand command) {
            return Result.success(new Amount(command.value()).value());
        }
    }

    /** Passa por referencia o construtor que recusa. */
    public static class ByConstructorReference implements CommandHandler<SomeCommand, String> {

        @Override
        public Result<String> handle(SomeCommand command) {
            return Result.success(Optional.of(command.value())
                    .map(Amount::new)
                    .orElseThrow()
                    .value());
        }
    }

    /** Chama o dominio por uma interface cuja implementacao recusa. */
    public static class ThroughDomainInterface implements CommandHandler<SomeCommand, String> {

        private final RegistrationPolicy policy;

        public ThroughDomainInterface(RegistrationPolicy policy) {
            this.policy = policy;
        }

        @Override
        public Result<String> handle(SomeCommand command) {
            return Result.success(policy.check(command.value()));
        }
    }

    /** Chama um dominio que recusa por referencia de metodo, dentro dele. */
    public static class ThroughDomainMethodReference implements CommandHandler<SomeCommand, String> {

        @Override
        public Result<String> handle(SomeCommand command) {
            return Result.success(String.join(",", Registration.ofAll(List.of(command.value()))));
        }
    }

    /** Chama um colaborador de aplicacao que deixa a recusa subir. */
    public static class ThroughCollaborator implements CommandHandler<SomeCommand, String> {

        private final Registrar registrar = new Registrar();

        @Override
        public Result<String> handle(SomeCommand command) {
            return Result.success(registrar.register(command.value()));
        }
    }

    /** Chama, fora de qualquer {@code try}, um metodo proprio que deixa a recusa subir. */
    public static class ThroughPrivateHelper implements CommandHandler<SomeCommand, String> {

        @Override
        public Result<String> handle(SomeCommand command) {
            return Result.success(register(command.value()));
        }

        private String register(String raw) {
            return Registration.of(raw);
        }
    }

    /** Envolve a chamada num {@code try}, mas captura outro tipo de excecao. */
    public static class WrongCatchType implements CommandHandler<SomeCommand, String> {

        @Override
        public Result<String> handle(SomeCommand command) {
            try {
                return Result.success(Registration.of(command.value()));
            } catch (IllegalStateException unrelated) {
                return Result.success("");
            }
        }
    }

    /** Instancia, por referencia ao construtor, uma {@code Function} do dominio que recusa, e a executa. */
    public static class ThroughAnOutsideInterfaceBuiltByReference implements CommandHandler<SomeCommand, String> {

        @Override
        public Result<String> handle(SomeCommand command) {
            Supplier<Function<String, String>> formats = RefusingFormat::new;
            return Result.success(formats.get().apply(command.value()));
        }
    }

    /** Passa por referencia o construtor da propria recusa. */
    public static class ByRefusalConstructorReference implements CommandHandler<SomeCommand, String> {

        @Override
        public Result<String> handle(SomeCommand command) {
            return Result.success(Optional.ofNullable(command.value()).orElseThrow(SampleRefusal::new));
        }
    }

    /** Recusa por uma fabrica estatica da recusa, que esconde o {@code new} atras de uma chamada. */
    public static class ByRefusalFactory implements CommandHandler<SomeCommand, String> {

        @Override
        public Result<String> handle(SomeCommand command) {
            if (command.value() == null) {
                throw SampleRefusal.missingValue();
            }
            return Result.success(command.value());
        }
    }

    /** Chama, por uma interface de fora do projeto, uma classe anonima que recusa. */
    public static class ThroughAnonymousClass implements CommandHandler<SomeCommand, String> {

        @Override
        public Result<String> handle(SomeCommand command) {
            Supplier<String> registering = new Supplier<>() {
                @Override
                public String get() {
                    return Registration.of(command.value());
                }
            };
            return Result.success(registering.get());
        }
    }

    /** Captura um lado do ciclo, mas chama o outro, que tambem recusa, fora do {@code try}. */
    public static class ThroughCycle implements CommandHandler<SomeCommand, String> {

        @Override
        public Result<String> handle(SomeCommand command) {
            try {
                CyclicRegistration.register(command.value(), 1);
            } catch (DomainException refusal) {
                return Result.failure(ApplicationError.from(refusal, ErrorType.VALIDATION));
            }
            return Result.success(CyclicRegistration.confirm(command.value(), 1));
        }
    }
}
