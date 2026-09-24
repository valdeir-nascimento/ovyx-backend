package io.github.ovyx.identity.application.caretaker;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.domain.DomainException;

/**
 * Traducao das recusas da administracao de responsaveis em {@code ApplicationError}.
 *
 * <p>O dominio diz qual regra recusou; aqui se decide a natureza da falha. Dado invalido e
 * {@code VALIDATION}; identificador que ja tem dono e o ultimo administrador sao {@code CONFLICT},
 * porque os dados estao certos e e o estado do sistema que impede a operacao.
 */
final class CaretakerRefusals {

    private CaretakerRefusals() {}

    static ApplicationError from(DomainException refusal) {
        ErrorType type = IdentityErrorCode.VALIDATION_FAILED.equals(refusal.errorCode())
                ? ErrorType.VALIDATION
                : ErrorType.CONFLICT;
        return ApplicationError.from(refusal, type);
    }

    static ApplicationError notFound() {
        return ApplicationError.of(
                ErrorType.NOT_FOUND, IdentityErrorCode.CARETAKER_NOT_FOUND.code(), "Responsável não encontrado.");
    }
}
