package io.github.ovyx.production.application.dashboard;

import java.math.BigDecimal;

/**
 * Um setor ativo na comparacao da granja toda, no periodo (US2 da 009). Sem relatorio no periodo, os numeros ficam
 * ausentes, e nao zero, e a situacao diante da meta tambem.
 *
 * @param target a meta atual do setor, com duas casas
 * @param targetStatus a produtividade do periodo diante da meta, com a igualdade como acima; {@code null} sem relatorio
 * @param todayReport o relatorio de hoje do setor; {@code null} se nao foi aberto
 * @param openAlerts os alertas e as pendencias abertos hoje, os mesmos do painel do setor
 */
public record FarmSectorRow(
        DashboardSector sector,
        Integer production,
        BigDecimal layingRate,
        BigDecimal target,
        TargetStatus targetStatus,
        BigDecimal costPerEgg,
        TodayReport todayReport,
        int openAlerts) {}
