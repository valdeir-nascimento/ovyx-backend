package io.github.ovyx.farm.domain.valueobject;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;

/**
 * Numero da gaiola na bateria: inteiro de 1 a 999 (FR-006). "07" e "7" sao o mesmo numero, e o codigo
 * sai com pelo menos dois digitos ("B-07", "B-120").
 *
 * @param value numero de 1 a 999
 */
public record CageNumber(int value) {

    private static final String FIELD = "number";
    private static final int MINIMUM = 1;
    private static final int MAXIMUM = 999;

    /**
     * Registra no {@link Notification} a violacao do campo, sem lancar: ausente, nao inteiro ou fora da
     * faixa, uma so, a primeira que se aplica.
     */
    public static void validate(String raw, Notification notification) {
        if (!notification.requirePresent(FIELD, raw, FarmErrorCode.CAGE_NUMBER_REQUIRED, "Informe o número da gaiola.")) {
            return;
        }
        if (!WholeNumber.isInteger(raw)) {
            notification.add(
                    FIELD, FarmErrorCode.CAGE_NUMBER_NOT_INTEGER, "O número da gaiola deve ser um número inteiro.");
        } else if (!WholeNumber.within(raw, MINIMUM, MAXIMUM)) {
            notification.add(
                    FIELD, FarmErrorCode.CAGE_NUMBER_OUT_OF_RANGE, "O número da gaiola deve ficar entre 1 e 999.");
        }
    }

    /**
     * Cria o numero, recusando na hora com a violacao do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando o numero falta, nao e inteiro ou sai da
     *     faixa
     */
    public static CageNumber of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);
        return new CageNumber(WholeNumber.valueOf(raw).orElseThrow());
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
