package io.github.ovyx.farm.domain.valueobject;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;

/**
 * Quantidade de aves da gaiola: inteiro de 0 a 1.000 (FR-006). Zero e gaiola vazia, uma situacao
 * valida.
 *
 * @param value quantidade de 0 a 1.000
 */
public record BirdCount(int value) {

    private static final String FIELD = "birdCount";
    private static final int MINIMUM = 0;
    private static final int MAXIMUM = 1000;

    /**
     * Registra no {@link Notification} a violacao do campo, sem lancar: ausente, nao inteiro ou fora da
     * faixa, uma so, a primeira que se aplica.
     */
    public static void validate(String raw, Notification notification) {
        if (!notification.requirePresent(FIELD, raw, FarmErrorCode.BIRD_COUNT_REQUIRED, "Informe a quantidade de aves.")) {
            return;
        }
        if (!WholeNumber.isInteger(raw)) {
            notification.add(
                    FIELD, FarmErrorCode.BIRD_COUNT_NOT_INTEGER, "A quantidade de aves deve ser um número inteiro.");
        } else if (!WholeNumber.within(raw, MINIMUM, MAXIMUM)) {
            notification.add(
                    FIELD, FarmErrorCode.BIRD_COUNT_OUT_OF_RANGE, "A quantidade de aves deve ficar entre 0 e 1.000.");
        }
    }

    /**
     * Cria a quantidade, recusando na hora com a violacao do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando a quantidade falta, nao e inteira ou
     *     sai da faixa
     */
    public static BirdCount of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);
        return new BirdCount(WholeNumber.valueOf(raw).orElseThrow());
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
