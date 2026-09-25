package io.github.ovyx.farm.application.common;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.domain.DomainException;

/**
 * Traducao das recusas do contexto farm em {@code ApplicationError}, para setores e gaiolas. Fica no
 * subpacote transversal da aplicacao, {@code common}, porque as duas funcionalidades a usam.
 *
 * <p>O dominio diz qual regra recusou; aqui se decide a natureza da falha. Dado invalido e
 * {@code VALIDATION}; nome ou codigo que ja tem dono, e setor inativo, sao {@code CONFLICT}, porque
 * os dados estao certos e e o estado da granja que impede a operacao.
 */
public final class FarmRefusals {

    private FarmRefusals() {}

    public static ApplicationError from(DomainException refusal) {
        return ApplicationError.from(refusal, typeOf(refusal));
    }

    private static ErrorType typeOf(DomainException refusal) {
        if (FarmErrorCode.VALIDATION_FAILED.equals(refusal.errorCode())) {
            return ErrorType.VALIDATION;
        }
        if (FarmErrorCode.SECTOR_NOT_FOUND.equals(refusal.errorCode())
                || FarmErrorCode.CAGE_NOT_FOUND.equals(refusal.errorCode())) {
            return ErrorType.NOT_FOUND;
        }
        return ErrorType.CONFLICT;
    }

    /** O mesmo para o setor que nao existe e para o identificador malformado. */
    public static ApplicationError sectorNotFound() {
        return ApplicationError.of(
                ErrorType.NOT_FOUND, FarmErrorCode.SECTOR_NOT_FOUND.code(), "Setor não encontrado.");
    }

    /** O mesmo para a gaiola que nao existe, a de outro setor e o identificador malformado. */
    public static ApplicationError cageNotFound() {
        return ApplicationError.of(
                ErrorType.NOT_FOUND, FarmErrorCode.CAGE_NOT_FOUND.code(), "Gaiola não encontrada.");
    }
}
