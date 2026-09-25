package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;

/**
 * Gaiola na lista (FR-011). Montada direto da consulta, sem passar pelo agregado (principio V).
 *
 * @param code a bateria, um hifen e o numero com pelo menos dois digitos ("B-07")
 */
public record CageSummary(
    CageId id,
    SectorId sectorId,
    String code,
    String battery,
    int number,
    int birdCount,
    Status status
) {
}
