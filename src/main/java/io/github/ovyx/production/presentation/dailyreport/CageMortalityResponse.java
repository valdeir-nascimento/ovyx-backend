package io.github.ovyx.production.presentation.dailyreport;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ovyx.production.application.dailyreport.CageMortality;
import io.swagger.v3.oas.annotations.media.Schema;

/** A mortalidade lancada numa gaiola. */
@Schema(description = "Mortalidade lançada numa gaiola")
public record CageMortalityResponse(
        @Schema(description = "Aves encontradas mortas", example = "1") int deaths,
        @Schema(description = "Aves descartadas — retiradas do plantel", example = "1") int culls,
        @Schema(example = "Prostração e penas eriçadas; uma ave separada para necropsia.")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String note) {

    public static CageMortalityResponse from(CageMortality mortality) {
        return mortality == null ? null : new CageMortalityResponse(mortality.deaths(), mortality.culls(), mortality.note());
    }
}
