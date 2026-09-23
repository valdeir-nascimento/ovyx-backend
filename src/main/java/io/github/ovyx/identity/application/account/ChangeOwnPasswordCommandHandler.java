package io.github.ovyx.identity.application.account;

import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.domain.DomainException;

import java.time.Clock;

import java.util.Optional;

/**
 * Troca da propria senha.
 *
 * <p>A conferencia da senha atual e a politica minima ficam no agregado; aqui fica a orquestracao:
 * carregar, delegar, gravar.
 */
public class ChangeOwnPasswordCommandHandler implements CommandHandler<ChangeOwnPasswordCommand, CaretakerId> {

    private final CaretakerRepository caretakerRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public ChangeOwnPasswordCommandHandler(CaretakerRepository caretakerRepository, PasswordHasher passwordHasher, Clock clock) {
        this.caretakerRepository = caretakerRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    public Result<CaretakerId> handle(ChangeOwnPasswordCommand command) {
        Optional<Caretaker> found = caretakerRepository.findById(command.caretakerId());

        // A sessao pode sobreviver a inativacao; inativo nao age sobre a conta (invariante 4).
        if (found.isEmpty() || !found.get().isActive()) {
            return Result.failure(ApplicationError.of(
                ErrorType.UNAUTHENTICATED,
                IdentityErrorCode.CARETAKER_UNAVAILABLE.code(),
                "Responsável não encontrado ou inativo.")
            );
        }

        Caretaker caretaker = found.get();

        try {
            caretaker.changeOwnPassword(command.currentPassword(), command.newPassword(), passwordHasher, clock);
        } catch (DomainException violation) {
            // O dominio sinaliza por excecao; o Result nasce aqui, no limite da aplicacao.
            return Result.failure(ApplicationError.from(violation, ErrorType.VALIDATION));
        }

        caretakerRepository.save(caretaker);

        return Result.success(caretaker.id());
    }
}
