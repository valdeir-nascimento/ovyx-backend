package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.Command;

/**
 * Reativacao de um setor, com as gaiolas que a inativacao dele levou (FR-015, FR-016).
 *
 * @param sectorId o identificador como veio no endereco
 */
public record ReactivateSectorCommand(String sectorId) implements Command<SectorId> {}
