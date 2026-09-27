package io.github.ovyx.production.presentation.dailyreport;

import io.github.ovyx.production.application.dailyreport.MortalityTotals;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** Os totais de mortalidade do dia e a situacao do lancamento. */
@Schema(description = "Totais de mortalidade do dia e a situação do lançamento")
public record MortalityTotalsResponse(
        @Schema(example = "RECORDED") MortalityStatus status,
        @Schema(example = "1") int deaths,
        @Schema(example = "1") int culls,
        @Schema(
                description = "Taxa do dia — (mortes + descartes) ÷ aves do início do dia, em porcentagem, com duas casas",
                example = "2.04")
        BigDecimal removalRate,
        @Schema(description = "Saldo de aves — aves do início do dia menos mortes e descartes", example = "96")
        int closingBirdCount) {

    public static MortalityTotalsResponse from(MortalityTotals totals) {
        return new MortalityTotalsResponse(
                totals.status(), totals.deaths(), totals.culls(), totals.removalRate(), totals.closingBirdCount());
    }
}
