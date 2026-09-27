package io.github.ovyx.farm.application.formula;

import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.farm.domain.model.Status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Formula na lista e no detalhe, com o custo por ave ao dia (FR-005 da 004).
 *
 * <p>Montada direto da consulta, sem passar pelo agregado (principio V). A lista e o detalhe mostram os
 * mesmos campos, e por isso o modelo e um so.
 *
 * @param description    a descricao, ou {@code null} quando a formula nao tem
 * @param pricePerKg     preco por quilo, com duas casas
 * @param costPerBirdDay preco x consumo esperado / 1.000, com tres casas (R-007)
 */
public record FeedFormulaSummary(
    FeedFormulaId id,
    String name,
    String description,
    BigDecimal pricePerKg,
    int expectedIntake,
    BigDecimal costPerBirdDay,
    Status status,
    Instant createdAt,
    Instant updatedAt
) {

    private static final BigDecimal GRAMS_PER_KILOGRAM = BigDecimal.valueOf(1000);
    private static final int COST_PER_BIRD_DAY_SCALE = 3;

    /**
     * A formula, com o custo por ave ao dia derivado do preco e do consumo esperado.
     */
    public static FeedFormulaSummary of(
        FeedFormulaId id,
        String name,
        String description,
        BigDecimal pricePerKg,
        int expectedIntake,
        Status status,
        Instant createdAt,
        Instant updatedAt) {
        return new FeedFormulaSummary(
            id,
            name,
            description,
            pricePerKg,
            expectedIntake,
            costPerBirdDayOf(pricePerKg, expectedIntake),
            status,
            createdAt,
            updatedAt
        );
    }

    /**
     * Preco x consumo esperado / 1.000, arredondado a partir da metade so no fim, com tres casas (R-007):
     * R$ 2,85 x 28 g = R$ 0,0798, que vira R$ 0,080.
     */
    static BigDecimal costPerBirdDayOf(BigDecimal pricePerKg, int expectedIntake) {
        return pricePerKg
            .multiply(BigDecimal.valueOf(expectedIntake))
            .divide(GRAMS_PER_KILOGRAM)
            .setScale(COST_PER_BIRD_DAY_SCALE, RoundingMode.HALF_UP);
    }
}
