package io.github.ovyx.production.presentation.dailyreport;

import io.github.ovyx.production.application.dailyreport.ProductionTotals;
import io.github.ovyx.production.domain.model.ProductionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** Os totais de producao do dia e a situacao do lancamento. */
@Schema(description = "Totais de produção do dia e a situação do lançamento")
public record ProductionTotalsResponse(
        @Schema(example = "COMPLETE") ProductionStatus status,
        @Schema(description = "Gaiolas sem produção lançada", example = "1") int pendingCages,
        @Schema(example = "89") int collectedEggs,
        @Schema(description = "Ovos coletados menos os classificados fora do padrão", example = "79") int standardEggs,
        @Schema(description = "Trincados, com sangue e anormais", example = "4") int unsellableEggs,
        @Schema(
                description = "Produtividade — ovos coletados ÷ aves do início do dia, em porcentagem, com duas casas",
                example = "90.82")
        BigDecimal layingRate) {

    public static ProductionTotalsResponse from(ProductionTotals totals) {
        return new ProductionTotalsResponse(
                totals.status(),
                totals.pendingCages(),
                totals.collectedEggs(),
                totals.standardEggs(),
                totals.unsellableEggs(),
                totals.layingRate());
    }
}
