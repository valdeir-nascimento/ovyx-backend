package io.github.ovyx.production.presentation.dailyreport;

import io.github.ovyx.production.application.dailyreport.CageProduction;
import io.swagger.v3.oas.annotations.media.Schema;

/** A producao lancada numa gaiola. */
@Schema(description = "Produção lançada numa gaiola")
public record CageProductionResponse(
        @Schema(description = "Ovos coletados", example = "45") int eggs,
        @Schema(description = "Pequenos", example = "1") int small,
        @Schema(description = "Jumbo", example = "1") int jumbo,
        @Schema(description = "Sujos", example = "1") int dirty,
        @Schema(description = "Trincados", example = "2") int cracked,
        @Schema(description = "Com sangue", example = "1") int bloodSpot,
        @Schema(description = "Anormais", example = "1") int abnormal) {

    public static CageProductionResponse from(CageProduction production) {
        return production == null
                ? null
                : new CageProductionResponse(
                        production.eggs(),
                        production.small(),
                        production.jumbo(),
                        production.dirty(),
                        production.cracked(),
                        production.bloodSpot(),
                        production.abnormal());
    }
}
