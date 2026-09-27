package io.github.ovyx.production.domain.valueobject;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.WholeNumber;

/**
 * Idade do lote, em semanas: inteiro de 1 a 150 (FR-001).
 *
 * @param value semanas de 1 a 150
 */
public record FlockAge(int value) {

    private static final String FIELD = "flockAge";
    private static final int MINIMUM = 1;
    /** A idade maxima do lote, em semanas; a sugestao de abertura tambem nao passa dela. */
    public static final int MAXIMUM = 150;

    /** Registra no {@link Notification} a violacao do campo, sem lancar: a primeira que se aplica. */
    public static void validate(String raw, Notification notification) {
        if (!notification.requirePresent(
                FIELD, raw, ProductionErrorCode.FLOCK_AGE_REQUIRED, "Informe a idade do lote, em semanas.")) {
            return;
        }
        if (!WholeNumber.isInteger(raw)) {
            notification.add(
                    FIELD,
                    ProductionErrorCode.FLOCK_AGE_NOT_INTEGER,
                    "A idade do lote deve ser um número inteiro de semanas.");
        } else if (!WholeNumber.within(raw, MINIMUM, MAXIMUM)) {
            notification.add(
                    FIELD,
                    ProductionErrorCode.FLOCK_AGE_OUT_OF_RANGE,
                    "A idade do lote deve ficar entre 1 e 150 semanas.");
        }
    }

    /**
     * Cria a idade, recusando na hora com a violacao do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando a idade falta, nao e inteira ou sai da
     *     faixa
     */
    public static FlockAge of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(ProductionErrorCode.VALIDATION_FAILED);
        return new FlockAge(WholeNumber.valueOf(raw).orElseThrow());
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
