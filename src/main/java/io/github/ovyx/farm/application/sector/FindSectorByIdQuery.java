package io.github.ovyx.farm.application.sector;

import io.github.ovyx.shared.application.Query;

/**
 * Consulta de um setor, ativo ou inativo.
 *
 * @param sectorId o identificador como veio no endereco; malformado, o setor nao e encontrado
 */
public record FindSectorByIdQuery(String sectorId) implements Query<SectorDetail> {}
