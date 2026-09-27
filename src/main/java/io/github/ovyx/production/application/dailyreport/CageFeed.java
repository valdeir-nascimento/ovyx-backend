package io.github.ovyx.production.application.dailyreport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * A racao lancada numa gaiola do relatorio (feature 004), com o preco e o esperado guardados no lancamento e
 * o nome atual da formula.
 *
 * @param formulaName o nome atual da formula: o lancamento guarda o preco, e nao o nome (R-005)
 * @param cost o consumo em quilos vezes o preco guardado, com duas casas
 * @param intakePerBird o consumo sobre as aves da gaiola, com uma casa; {@code null} com 0 aves
 * @param deviation o desvio do consumo por ave em relacao ao esperado, em porcentagem, com uma casa;
 *     {@code null} com 0 aves
 */
public record CageFeed(
        UUID formulaId,
        String formulaName,
        BigDecimal pricePerKg,
        int expectedIntake,
        int consumption,
        BigDecimal cost,
        BigDecimal intakePerBird,
        BigDecimal deviation) {

    private static final BigDecimal GRAMS_PER_KILOGRAM = BigDecimal.valueOf(1000);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * A racao da gaiola com os derivados, cada um arredondado para cima a partir da metade so na saida, a
     * partir da conta exata (R-007 da 004): o desvio vem do consumo por ave exato, e nao do ja arredondado.
     */
    public static CageFeed of(
            UUID formulaId, String formulaName, BigDecimal pricePerKg, int expectedIntake, int consumption, int birdCount) {
        BigDecimal cost = BigDecimal.valueOf(consumption)
                .multiply(pricePerKg)
                .divide(GRAMS_PER_KILOGRAM)
                .setScale(2, RoundingMode.HALF_UP);
        if (birdCount == 0) {
            return new CageFeed(formulaId, formulaName, pricePerKg, expectedIntake, consumption, cost, null, null);
        }
        BigDecimal intakePerBird =
                BigDecimal.valueOf(consumption).divide(BigDecimal.valueOf(birdCount), 1, RoundingMode.HALF_UP);
        // (consumo / aves - esperado) / esperado x 100, numa divisao so: (consumo - aves x esperado) x 100
        // sobre aves x esperado.
        long expectedOfTheCage = (long) birdCount * expectedIntake;
        BigDecimal deviation = BigDecimal.valueOf(consumption - expectedOfTheCage)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(expectedOfTheCage), 1, RoundingMode.HALF_UP);
        return new CageFeed(
                formulaId, formulaName, pricePerKg, expectedIntake, consumption, cost, intakePerBird, deviation);
    }
}
