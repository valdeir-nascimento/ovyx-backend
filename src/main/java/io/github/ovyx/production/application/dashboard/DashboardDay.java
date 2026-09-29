package io.github.ovyx.production.application.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Um dia da serie dos ultimos 7 dias (R-006 da 006). Sem relatorio, os valores ficam ausentes; com a racao
 * pendente, os custos ficam ausentes.
 */
public record DashboardDay(
        LocalDate date, Integer production, BigDecimal layingRate, BigDecimal feedCost, BigDecimal costPerEgg) {}
