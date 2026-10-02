package io.github.ovyx.identity.application.account;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import java.util.Optional;

/**
 * Troca do proprio tema (US2 da 011). A validacao do tema e a recusa do inativo ficam no agregado; aqui fica a
 * orquestracao, carregar, delegar e gravar, e a traducao da recusa, como na troca da propria senha: responsavel
 * indisponivel encerra a sessao (401), e o resto e recusa de preenchimento.
 */
public class ChangeOwnThemeCommandHandler implements CommandHandler<ChangeOwnThemeCommand, CaretakerId> {

    private final CaretakerRepository caretakerRepository;

    public ChangeOwnThemeCommandHandler(CaretakerRepository caretakerRepository) {
        this.caretakerRepository = caretakerRepository;
    }

    @Override
    public Result<CaretakerId> handle(ChangeOwnThemeCommand command) {
        Optional<Caretaker> found = caretakerRepository.findById(command.caretakerId());

        // A sessao pode sobreviver a exclusao do cadastro; sem responsavel, nao ha a quem delegar.
        if (found.isEmpty()) {
            return Result.failure(ApplicationError.of(
                    ErrorType.UNAUTHENTICATED,
                    IdentityErrorCode.CARETAKER_UNAVAILABLE.code(),
                    IdentityErrorCode.CARETAKER_UNAVAILABLE_MESSAGE));
        }

        Caretaker caretaker = found.get();
        try {
            caretaker.chooseTheme(command.theme());
        } catch (DomainException refusal) {
            return Result.failure(ApplicationError.from(refusal, typeOf(refusal)));
        }

        caretakerRepository.save(caretaker);
        return Result.success(caretaker.id());
    }

    private static ErrorType typeOf(DomainException refusal) {
        return refusal.errorCode() == IdentityErrorCode.CARETAKER_UNAVAILABLE
                ? ErrorType.UNAUTHENTICATED
                : ErrorType.VALIDATION;
    }
}
