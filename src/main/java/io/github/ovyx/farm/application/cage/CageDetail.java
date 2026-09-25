package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;

import java.time.Instant;

/**
 * Uma gaiola, com os instantes do cadastro e da ultima alteracao.
 */
public record CageDetail(
    CageId id,
    SectorId sectorId,
    String code,
    String battery,
    int number,
    int birdCount,
    Status status,
    Instant createdAt,
    Instant updatedAt
) {
}
