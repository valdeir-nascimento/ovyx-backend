package io.github.ovyx.farm.domain.valueobject;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.WholeNumber;

/**
 * Consumo esperado da formula: gramas de racao por ave ao dia, inteiro de 1 a 200 (R-011 da 004).
 *
 * @param value gramas por ave ao dia, de 1 a 200
 */
public record ExpectedIntake(int value) {

    private static final String FIELD = "expectedIntake";
    private static final int MINIMUM = 1;
    private static final int MAXIMUM = 200;

    /**
     * Registra no {@link Notification} a violacao do campo, sem lancar: ausente, nao inteiro ou fora da
     * faixa, uma so, a primeira que se aplica.
     */
    public static void validate(String raw, Notification notification) {
        if (!notification.requirePresent(
                FIELD, raw, FarmErrorCode.EXPECTED_INTAKE_REQUIRED, "Informe o consumo esperado.")) {
            return;
        }
        if (!WholeNumber.isInteger(raw)) {
            notification.add(
                    FIELD,
                    FarmErrorCode.EXPECTED_INTAKE_NOT_INTEGER,
                    "O consumo esperado deve ser um número inteiro de gramas.");
        } else if (!WholeNumber.within(raw, MINIMUM, MAXIMUM)) {
            notification.add(
                    FIELD,
                    FarmErrorCode.EXPECTED_INTAKE_OUT_OF_RANGE,
                    "O consumo esperado deve ficar entre 1 e 200 gramas por ave ao dia.");
        }
    }

    /**
     * Cria o consumo esperado, recusando na hora com a violacao do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando o consumo falta, nao e inteiro ou sai
     *     da faixa
     */
    public static ExpectedIntake of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);
        return new ExpectedIntake(WholeNumber.valueOf(raw).orElseThrow());
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
