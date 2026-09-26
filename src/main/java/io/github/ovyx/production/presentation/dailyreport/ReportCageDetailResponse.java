package io.github.ovyx.production.presentation.dailyreport;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ovyx.production.application.dailyreport.ReportCageDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Uma gaiola do relatorio, como estava na abertura, com os lancamentos que tiver. */
@Schema(description = "Uma gaiola do relatório, como estava na abertura, com os lançamentos que tiver")
public record ReportCageDetailResponse(
        @Schema(example = "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44") UUID cageId,
        @Schema(example = "B-07") String code,
        @Schema(example = "B") String battery,
        @Schema(example = "7") int number,
        @Schema(description = "Aves da gaiola na abertura do relatório", example = "50") int birdCount,
        @Schema(description = "Ausente enquanto a produção não é lançada") @JsonInclude(JsonInclude.Include.NON_NULL)
        CageProductionResponse production,
        @Schema(description = "Ausente enquanto a mortalidade não é lançada") @JsonInclude(JsonInclude.Include.NON_NULL)
        CageMortalityResponse mortality) {

    public static ReportCageDetailResponse from(ReportCageDetail cage) {
        return new ReportCageDetailResponse(
                cage.cageId(),
                cage.code(),
                cage.battery(),
                cage.number(),
                cage.birdCount(),
                CageProductionResponse.from(cage.production()),
                CageMortalityResponse.from(cage.mortality()));
    }
}
