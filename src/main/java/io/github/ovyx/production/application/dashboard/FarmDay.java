package io.github.ovyx.production.application.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Um dia da serie da granja toda (feature 009): os setores ativos com relatorio no dia somados, com as contas do
 * painel de um setor. Sem relatorio, os valores ficam ausentes; com a racao pendente em todos, os custos ficam
 * ausentes.
 *
 * @param target a meta da granja no dia, ponderada pelas aves dos relatorios; {@code null} sem relatorio
 * @param reportingSectors quantos setores ativos tem relatorio no dia
 */
public record FarmDay(
        LocalDate date,
        Integer production,
        BigDecimal layingRate,
        BigDecimal feedCost,
        BigDecimal costPerEgg,
        BigDecimal target,
        int reportingSectors) {}
