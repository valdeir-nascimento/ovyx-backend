package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.shared.application.Command;

/**
 * Reativacao de uma gaiola sozinha (FR-015, FR-016).
 *
 * @param sectorId o identificador do setor como veio no endereco
 * @param cageId o identificador da gaiola como veio no endereco
 */
public record ReactivateCageCommand(String sectorId, String cageId) implements Command<CageId> {}
