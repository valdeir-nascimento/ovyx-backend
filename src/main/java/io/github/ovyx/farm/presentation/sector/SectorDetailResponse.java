package io.github.ovyx.farm.presentation.sector;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ovyx.farm.application.sector.SectorDetail;
import io.github.ovyx.farm.domain.model.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Um setor, com os totais e os instantes do cadastro e da última alteração. */
@Schema(description = "Setor, com os totais das gaiolas ativas")
public record SectorDetailResponse(
        @Schema(example = "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11") UUID id,
        @Schema(example = "Codornas — Galpão 1") String name,
        @Schema(description = "Ausente quando o setor não tem descrição", example = "Codornas japonesas em postura, baterias A a D")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String description,
        @Schema(example = "ACTIVE") Status status,
        @Schema(description = "Quantidade de gaiolas ativas", example = "48") int activeCageCount,
        @Schema(description = "Soma das aves das gaiolas ativas", example = "2400") int birdCount,
        @Schema(
                description = "Baterias que as gaiolas do setor usam, ativas ou inativas, em ordem; é o filtro de"
                        + " bateria da lista de gaiolas",
                example = "[\"A\", \"B\", \"C\", \"D\"]")
        List<String> batteries,
        @Schema(example = "2026-09-20T10:15:00Z") Instant createdAt,
        @Schema(example = "2026-09-24T17:40:12Z") Instant updatedAt) {

    public static SectorDetailResponse from(SectorDetail detail) {
        return new SectorDetailResponse(
                detail.id().value(),
                detail.name(),
                detail.description(),
                detail.status(),
                detail.activeCageCount(),
                detail.birdCount(),
                detail.batteries(),
                detail.createdAt(),
                detail.updatedAt());
    }
}
