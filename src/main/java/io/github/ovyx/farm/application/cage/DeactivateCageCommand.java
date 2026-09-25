package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.shared.application.Command;

/**
 * Inativacao de uma gaiola sozinha (FR-012).
 *
 * @param sectorId o identificador do setor como veio no endereco
 * @param cageId o identificador da gaiola como veio no endereco
 */
public record DeactivateCageCommand(String sectorId, String cageId) implements Command<CageId> {}
