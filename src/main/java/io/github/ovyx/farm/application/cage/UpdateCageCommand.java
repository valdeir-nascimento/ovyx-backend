package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.shared.application.Command;

/**
 * Edicao da bateria, do numero e das aves de uma gaiola (FR-009). O setor da gaiola nao muda.
 *
 * @param sectorId o identificador do setor como veio no endereco
 * @param cageId o identificador da gaiola como veio no endereco
 */
public record UpdateCageCommand(String sectorId, String cageId, String battery, String number, String birdCount)
        implements Command<CageId> {}
