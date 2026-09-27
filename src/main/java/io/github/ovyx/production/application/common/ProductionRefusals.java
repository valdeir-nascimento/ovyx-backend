package io.github.ovyx.production.application.common;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.domain.DomainException;

/**
 * Traducao das recusas do contexto production em {@code ApplicationError}, no molde do
 * {@code FarmRefusals}.
 *
 * <p>Dado invalido e {@code VALIDATION}; identificador que nao aponta nada e {@code NOT_FOUND}; o resto
 * — data ja ocupada, setor inativo ou sem gaiola — e {@code CONFLICT}, porque os dados estao certos e e
 * o estado da granja que impede a operacao.
 */
public final class ProductionRefusals {

    private ProductionRefusals() {}

    public static ApplicationError from(DomainException refusal) {
        return ApplicationError.from(refusal, typeOf(refusal));
    }

    private static ErrorType typeOf(DomainException refusal) {
        if (ProductionErrorCode.VALIDATION_FAILED.equals(refusal.errorCode())) {
            return ErrorType.VALIDATION;
        }
        if (refusal.errorCode().code().endsWith("_NOT_FOUND")) {
            return ErrorType.NOT_FOUND;
        }
        return ErrorType.CONFLICT;
    }

    /** O mesmo para o setor que nao existe e para o identificador malformado. */
    public static ApplicationError sectorNotFound() {
        return ApplicationError.of(
                ErrorType.NOT_FOUND, ProductionErrorCode.SECTOR_NOT_FOUND.code(), "Setor não encontrado.");
    }

    /** O mesmo para a gaiola que nao existe, a que nao e do relatorio e o identificador malformado. */
    public static ApplicationError cageNotFound() {
        return ApplicationError.of(
                ErrorType.NOT_FOUND, ProductionErrorCode.CAGE_NOT_FOUND.code(), "Gaiola não encontrada neste relatório.");
    }

    /** O mesmo para o relatorio que nao existe, o de outro setor e o identificador malformado. */
    public static ApplicationError dailyReportNotFound() {
        return ApplicationError.of(
                ErrorType.NOT_FOUND, ProductionErrorCode.DAILY_REPORT_NOT_FOUND.code(), "Relatório não encontrado.");
    }
}
