package io.github.ovyx.identity.application.caretaker;

import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import java.time.Clock;

/**
 * Cadastra um responsavel (FR-013).
 *
 * <p>As regras — campos, unicidade e senha — sao do agregado. O tratador entrega a ele o
 * repositorio, como o quadro dos demais responsaveis, grava o resultado e traduz a recusa em
 * {@code Failure} (principio IV).
 */
public class RegisterCaretakerCommandHandler implements CommandHandler<RegisterCaretakerCommand, CaretakerId> {

    private final CaretakerRepository caretakerRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public RegisterCaretakerCommandHandler(
            CaretakerRepository caretakerRepository, PasswordHasher passwordHasher, Clock clock) {
        this.caretakerRepository = caretakerRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    public Result<CaretakerId> handle(RegisterCaretakerCommand command) {
        Caretaker caretaker;
        try {
            // A senha definida pelo administrador e provisoria: quem cadastra a conhece, e so a
            // troca pela propria pessoa encerra isso (data-model.md).
            caretaker = Caretaker.register(
                    command.fullName(),
                    command.cpf(),
                    command.email(),
                    command.mobilePhone(),
                    command.password(),
                    command.role(),
                    true,
                    passwordHasher,
                    caretakerRepository,
                    clock);
        } catch (DomainException refusal) {
            return Result.failure(CaretakerRefusals.from(refusal));
        }

        caretakerRepository.save(caretaker);
        return Result.success(caretaker.id());
    }
}
