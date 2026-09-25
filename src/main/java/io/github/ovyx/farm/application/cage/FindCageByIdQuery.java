package io.github.ovyx.farm.application.cage;

import io.github.ovyx.shared.application.Query;

/**
 * Consulta de uma gaiola do setor, ativa ou inativa.
 *
 * @param sectorId o identificador do setor como veio no endereco
 * @param cageId o identificador da gaiola como veio no endereco
 */
public record FindCageByIdQuery(String sectorId, String cageId) implements Query<CageDetail> {}
