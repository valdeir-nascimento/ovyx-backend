package io.github.ovyx.production.application.dailyreport;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A proposta para uma gaiola sem racao.
 *
 * @param consumption as aves vezes o consumo esperado, em gramas
 * @param cost o custo da proposta, com duas casas
 */
public record SuggestedCageFeed(UUID cageId, String code, int birdCount, int consumption, BigDecimal cost) {}
