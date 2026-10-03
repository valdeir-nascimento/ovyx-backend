package io.github.ovyx.farm.application.cage;

import io.github.ovyx.shared.domain.WeighingStanding;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;

/**
 * Gaiola na lista (FR-011). Montada direto da consulta, sem passar pelo agregado (principio V).
 *
 * @param code a bateria, um hifen e o numero com pelo menos dois digitos ("B-07")
 *
 * @param lastWeighing a ultima pesagem valida, ou {@code null} sem pesagem (feature 005)
 * @param weighing a situacao na agenda de pesagem do setor (feature 010); {@code null} na gaiola inativa e nas
 *     gaiolas de setor inativo
 */
public record CageSummary(
    CageId id,
    SectorId sectorId,
    String code,
    String battery,
    int number,
    int birdCount,
    Status status,
    CageLastWeighing lastWeighing,
    WeighingStanding weighing
) {

    /** A mesma gaiola com a situacao na agenda (feature 010). */
    public CageSummary withWeighing(WeighingStanding standing) {
        return new CageSummary(id, sectorId, code, battery, number, birdCount, status, lastWeighing, standing);
    }
}
