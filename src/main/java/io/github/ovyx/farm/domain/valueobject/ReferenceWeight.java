package io.github.ovyx.farm.domain.valueobject;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.WholeNumber;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * A faixa de peso de referencia das aves de um setor, em gramas (FR-001 e R-006 da 005). Opcional no setor:
 * os dois limites vao juntos, inteiros de 1 a 10.000, com o minimo abaixo do maximo.
 *
 * @param minimum o peso minimo esperado, em gramas
 * @param maximum o peso maximo esperado, em gramas
 */
public record ReferenceWeight(int minimum, int maximum) {

    private static final String MINIMUM_FIELD = "minimumWeight";
    private static final String MAXIMUM_FIELD = "maximumWeight";
    private static final int LOWEST = 1;
    private static final int HIGHEST = 10_000;
    private static final String INVALID = "O peso deve ser um número inteiro de 1 a 10.000 gramas.";

    /**
     * Registra no {@link Notification} as violacoes da faixa, sem lancar: cada limite invalido no proprio
     * campo, o que falta quando so um veio, e a inversao no minimo quando os dois valem.
     */
    public static void validate(String rawMinimum, String rawMaximum, Notification notification) {
        boolean hasMinimum = present(rawMinimum);
        boolean hasMaximum = present(rawMaximum);
        boolean validMinimum = !hasMinimum || validateLimit(MINIMUM_FIELD, rawMinimum, notification);
        boolean validMaximum = !hasMaximum || validateLimit(MAXIMUM_FIELD, rawMaximum, notification);
        if (hasMinimum && !hasMaximum) {
            notification.add(
                    MAXIMUM_FIELD, FarmErrorCode.REFERENCE_WEIGHT_INCOMPLETE, "Informe também o peso máximo.");
        } else if (!hasMinimum && hasMaximum) {
            notification.add(
                    MINIMUM_FIELD, FarmErrorCode.REFERENCE_WEIGHT_INCOMPLETE, "Informe também o peso mínimo.");
        } else if (hasMinimum
                && validMinimum
                && validMaximum
                && gramsOf(rawMinimum) >= gramsOf(rawMaximum)) {
            notification.add(
                    MINIMUM_FIELD,
                    FarmErrorCode.REFERENCE_WEIGHT_INVERTED,
                    "O peso mínimo deve ser menor que o máximo.");
        }
    }

    /**
     * A faixa lida dos limites como vieram, ou nenhuma quando os dois estao vazios. Os limites ja foram
     * validados por {@link #validate}.
     */
    public static Optional<ReferenceWeight> optionalOf(String rawMinimum, String rawMaximum) {
        if (!present(rawMinimum) && !present(rawMaximum)) {
            return Optional.empty();
        }
        return Optional.of(new ReferenceWeight(gramsOf(rawMinimum), gramsOf(rawMaximum)));
    }

    /** Se o peso esta na faixa, com os limites incluidos (FR-009 da 005). */
    public boolean contains(BigDecimal grams) {
        return grams.compareTo(BigDecimal.valueOf(minimum)) >= 0 && grams.compareTo(BigDecimal.valueOf(maximum)) <= 0;
    }

    private static boolean validateLimit(String field, String raw, Notification notification) {
        if (!WholeNumber.isInteger(raw) || !WholeNumber.within(raw, LOWEST, HIGHEST)) {
            notification.add(field, FarmErrorCode.REFERENCE_WEIGHT_INVALID, INVALID);
            return false;
        }
        return true;
    }

    private static boolean present(String raw) {
        return raw != null && !raw.isBlank();
    }

    private static int gramsOf(String raw) {
        return WholeNumber.valueOf(raw).orElseThrow();
    }
}
