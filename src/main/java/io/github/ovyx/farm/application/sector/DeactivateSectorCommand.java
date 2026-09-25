package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.Command;

/**
 * Inativacao de um setor, com as gaiolas ativas dele (FR-015).
 *
 * @param sectorId o identificador como veio no endereco
 */
public record DeactivateSectorCommand(String sectorId) implements Command<SectorId> {
}
