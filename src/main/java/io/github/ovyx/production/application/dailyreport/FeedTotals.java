package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.FeedStatus;
import java.math.BigDecimal;

/**
 * Os totais de racao do dia (FR-012 a FR-016 da 004), das gaiolas com racao lancada, e a situacao do
 * lancamento.
 *
 * @param consumption o consumo das gaiolas lancadas, em gramas
 * @param cost o custo da racao do dia, com duas casas
 * @param costPerEgg o custo sobre os ovos coletados, com tres casas; {@code null} sem ovo ou sem racao
 *     lancada
 * @param intakePerBird o consumo sobre as aves das gaiolas lancadas, com uma casa; {@code null} sem aves
 * @param expectedIntakePerBird o esperado de cada formula pesado pelas aves das gaiolas lancadas, com uma
 *     casa; {@code null} sem aves
 */
public record FeedTotals(
        FeedStatus status,
        int pendingCages,
        int consumption,
        BigDecimal cost,
        BigDecimal costPerEgg,
        BigDecimal intakePerBird,
        BigDecimal expectedIntakePerBird) {}
