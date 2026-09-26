package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.ProductionStatus;
import java.math.BigDecimal;

/**
 * Os totais de producao do dia (FR-009) e a situacao do lancamento (FR-010).
 *
 * @param standardEggs ovos coletados menos os classificados fora do padrao
 * @param unsellableEggs trincados, com sangue e anormais
 * @param layingRate ovos coletados sobre as aves do inicio do dia, em porcentagem, com duas casas
 */
public record ProductionTotals(
        ProductionStatus status,
        int pendingCages,
        int collectedEggs,
        int standardEggs,
        int unsellableEggs,
        BigDecimal layingRate) {}
