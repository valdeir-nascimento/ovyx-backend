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
 * <p>A conferencia da senha atual, a politica minima e a recusa do inativo ficam no agregado; aqui
 * fica a orquestracao: carregar, delegar, gravar. E a traducao da recusa: responsavel indisponivel
 * encerra a sessao (401), e o resto e recusa de preenchimento.
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

        // A sessao pode sobreviver a exclusao do cadastro; sem responsavel, nao ha a quem delegar.
        if (found.isEmpty()) {
            return Result.failure(ApplicationError.of(
                ErrorType.UNAUTHENTICATED,
                IdentityErrorCode.CARETAKER_UNAVAILABLE.code(),
                IdentityErrorCode.CARETAKER_UNAVAILABLE_MESSAGE)
            );
        }

        Caretaker caretaker = found.get();

        try {
            caretaker.changeOwnPassword(command.currentPassword(), command.newPassword(), passwordHasher, clock);
        } catch (DomainException refusal) {
            // O dominio sinaliza por excecao; o Result nasce aqui, no limite da aplicacao.
            return Result.failure(ApplicationError.from(refusal, typeOf(refusal)));
        }

        caretakerRepository.save(caretaker);

        return Result.success(caretaker.id());
    }

    /** O inativo com a sessao aberta tem a sessao encerrada; qualquer outra recusa e de preenchimento. */
    private static ErrorType typeOf(DomainException refusal) {
        return refusal.errorCode() == IdentityErrorCode.CARETAKER_UNAVAILABLE ? ErrorType.UNAUTHENTICATED : ErrorType.VALIDATION;
    }
}
