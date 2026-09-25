package io.github.ovyx.farm.presentation.cage;

import io.github.ovyx.farm.application.cage.CageDetail;
import io.github.ovyx.farm.domain.model.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Uma gaiola, com os instantes do cadastro e da última alteração. */
@Schema(description = "Gaiola")
public record CageDetailResponse(
        @Schema(example = "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44") UUID id,
        @Schema(example = "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11") UUID sectorId,
        @Schema(description = "Bateria, hífen e número com ao menos dois dígitos", example = "B-07") String code,
        @Schema(example = "B") String battery,
        @Schema(example = "7") int number,
        @Schema(example = "50") int birdCount,
        @Schema(example = "ACTIVE") Status status,
        @Schema(example = "2026-09-21T08:30:00Z") Instant createdAt,
        @Schema(example = "2026-09-24T17:42:05Z") Instant updatedAt) {

    public static CageDetailResponse from(CageDetail detail) {
        return new CageDetailResponse(
                detail.id().value(),
                detail.sectorId().value(),
                detail.code(),
                detail.battery(),
                detail.number(),
                detail.birdCount(),
                detail.status(),
                detail.createdAt(),
                detail.updatedAt());
    }
}
