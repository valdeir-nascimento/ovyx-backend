package io.github.ovyx.production.application.dailyreport;

import java.math.BigDecimal;

/**
 * Um dia de relatorio, no que as contas de um periodo precisam (R-007 da 007).
 *
 * @param openingBirdCount as aves do inicio do dia
 * @param eggs os ovos coletados nas gaiolas com producao
 * @param exactFeedCost a soma de consumo vezes preco guardado das gaiolas com racao, antes de dividir por 1.000
 * @param feedComplete se a racao do dia esta completa
 */
public record PeriodDay(int openingBirdCount, int eggs, BigDecimal exactFeedCost, boolean feedComplete) {}
