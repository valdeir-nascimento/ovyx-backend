package io.github.ovyx.farm.presentation.sector;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ovyx.farm.application.sector.SectorSummary;
import io.github.ovyx.farm.domain.model.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Um setor na lista, com os totais das gaiolas ativas (FR-004). */
@Schema(description = "Setor na lista, com os totais das gaiolas ativas")
public record SectorSummaryResponse(
        @Schema(example = "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11") UUID id,
        @Schema(example = "Codornas — Galpão 1") String name,
        @Schema(description = "Ausente quando o setor não tem descrição", example = "Codornas japonesas em postura, baterias A a D")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String description,
        @Schema(example = "ACTIVE") Status status,
        @Schema(description = "Quantidade de gaiolas ativas", example = "48") int activeCageCount,
        @Schema(description = "Soma das aves das gaiolas ativas", example = "2400") int birdCount) {

    public static SectorSummaryResponse from(SectorSummary summary) {
        return new SectorSummaryResponse(
                summary.id().value(),
                summary.name(),
                summary.description(),
                summary.status(),
                summary.activeCageCount(),
                summary.birdCount());
    }
}
