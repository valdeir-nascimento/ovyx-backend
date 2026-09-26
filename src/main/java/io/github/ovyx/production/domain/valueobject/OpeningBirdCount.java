package io.github.ovyx.production.domain.valueobject;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.WholeNumber;

/**
 * Aves no inicio do dia: inteiro de 1 a 1.000.000 (FR-001). E a base da produtividade e da taxa do dia,
 * e o limite da mortalidade do relatorio.
 *
 * @param value aves de 1 a 1.000.000
 */
public record OpeningBirdCount(int value) {

    private static final String FIELD = "openingBirdCount";
    private static final int MINIMUM = 1;
    private static final int MAXIMUM = 1_000_000;

    /** Registra no {@link Notification} a violacao do campo, sem lancar: a primeira que se aplica. */
    public static void validate(String raw, Notification notification) {
        if (!notification.requirePresent(
                FIELD, raw, ProductionErrorCode.OPENING_BIRD_COUNT_REQUIRED, "Informe as aves no início do dia.")) {
            return;
        }
        if (!WholeNumber.isInteger(raw)) {
            notification.add(
                    FIELD,
                    ProductionErrorCode.OPENING_BIRD_COUNT_NOT_INTEGER,
                    "As aves do início do dia devem ser um número inteiro.");
        } else if (!WholeNumber.within(raw, MINIMUM, MAXIMUM)) {
            notification.add(
                    FIELD,
                    ProductionErrorCode.OPENING_BIRD_COUNT_OUT_OF_RANGE,
                    "As aves do início do dia devem ficar entre 1 e 1.000.000.");
        }
    }

    /**
     * Cria a quantidade, recusando na hora com a violacao do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando a quantidade falta, nao e inteira ou sai
     *     da faixa
     */
    public static OpeningBirdCount of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(ProductionErrorCode.VALIDATION_FAILED);
        return new OpeningBirdCount(WholeNumber.valueOf(raw).orElseThrow());
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
