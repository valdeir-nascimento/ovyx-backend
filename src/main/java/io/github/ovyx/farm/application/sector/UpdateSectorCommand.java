package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.Command;

/**
 * Edicao do nome e da descricao de um setor (FR-003).
 *
 * @param sectorId o identificador como veio no endereco; malformado, o setor nao e encontrado
 */
public record UpdateSectorCommand(String sectorId, String name, String description) implements Command<SectorId> {}
