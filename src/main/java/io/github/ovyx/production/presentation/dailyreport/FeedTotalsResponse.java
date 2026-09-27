package io.github.ovyx.production.presentation.dailyreport;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ovyx.production.application.dailyreport.FeedTotals;
import io.github.ovyx.production.domain.model.FeedStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** Os totais de racao do dia, das gaiolas com racao lancada, e a situacao do lancamento (feature 004). */
@Schema(name = "FeedTotals", description = "Totais de ração do dia, das gaiolas com ração lançada")
public record FeedTotalsResponse(
        @Schema(example = "PENDING") FeedStatus status,
        @Schema(description = "Gaiolas ainda sem ração", example = "1") int pendingCages,
        @Schema(description = "Consumo total, em gramas", example = "2744") int consumption,
        @Schema(description = "Custo da ração, em reais, com duas casas", example = "7.82") BigDecimal cost,
        @Schema(
                description = "Custo da ração ÷ ovos coletados, em reais, com três casas; ausente sem ovo ou sem"
                        + " ração lançada",
                example = "0.088")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal costPerEgg,
        @Schema(
                description = "Consumo total ÷ aves das gaiolas lançadas, em gramas, com uma casa; ausente sem aves",
                example = "28.0")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal intakePerBird,
        @Schema(
                description = "Esperado de cada fórmula pesado pelas aves das gaiolas lançadas, em gramas, com uma"
                        + " casa; ausente sem aves",
                example = "28.0")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal expectedIntakePerBird) {

    public static FeedTotalsResponse from(FeedTotals totals) {
        return new FeedTotalsResponse(
                totals.status(),
                totals.pendingCages(),
                totals.consumption(),
                totals.cost(),
                totals.costPerEgg(),
                totals.intakePerBird(),
                totals.expectedIntakePerBird());
    }
}
