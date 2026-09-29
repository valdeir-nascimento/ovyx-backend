package io.github.ovyx.farm.presentation.weighing;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ovyx.farm.application.weighing.WeighingDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Uma pesagem valida da gaiola, com quem a registrou e quem a corrigiu por ultimo. */
@Schema(name = "Weighing", description = "Uma pesagem válida da gaiola")
public record WeighingResponse(
        @Schema(example = "7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77") UUID id,
        @Schema(example = "2026-09-24") LocalDate weighedOn,
        @Schema(description = "Peso médio da amostra, em gramas, com uma casa", example = "161.4")
        BigDecimal averageWeight,
        ActorResponse recordedBy,
        @Schema(example = "2026-09-24T10:12:40Z") Instant recordedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) ActorResponse lastCorrectedBy,
        @Schema(example = "2026-09-24T10:15:02Z") @JsonInclude(JsonInclude.Include.NON_NULL) Instant lastCorrectedAt) {

    public static WeighingResponse from(WeighingDetail detail) {
        return new WeighingResponse(
                detail.id().value(),
                detail.weighedOn(),
                detail.averageWeight(),
                ActorResponse.from(detail.recordedBy()),
                detail.recordedAt(),
                ActorResponse.from(detail.lastCorrectedBy()),
                detail.lastCorrectedAt());
    }
}
