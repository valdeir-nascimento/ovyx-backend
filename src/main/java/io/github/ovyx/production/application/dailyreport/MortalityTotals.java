package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.MortalityStatus;
import java.math.BigDecimal;

/**
 * Os totais de mortalidade do dia (FR-014) e a situacao do lancamento (FR-013).
 *
 * @param removalRate mortes mais descartes sobre as aves do inicio do dia, em porcentagem, com duas casas
 * @param closingBirdCount aves do inicio do dia menos mortes e descartes
 */
public record MortalityTotals(
        MortalityStatus status, int deaths, int culls, BigDecimal removalRate, int closingBirdCount) {}
