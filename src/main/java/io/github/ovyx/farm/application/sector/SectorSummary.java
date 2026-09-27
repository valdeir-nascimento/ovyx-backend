package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;

/**
 * Setor na lista, com os totais das gaiolas ativas (FR-004).
 *
 * <p>Montado direto da consulta, sem passar pelo agregado (principio V). Os totais sao derivados das
 * gaiolas, e nao guardados: calculados na leitura, nao tem como divergir delas.
 *
 * @param description     a descricao, ou {@code null} quando o setor nao tem
 * @param activeCageCount quantidade de gaiolas ativas
 * @param birdCount       soma das aves das gaiolas ativas
 */
public record SectorSummary(
    SectorId id,
    String name,
    String description,
    Status status,
    int activeCageCount,
    int birdCount
) {
}
