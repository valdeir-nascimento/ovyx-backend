package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.Command;

/**
 * Edicao do nome, da descricao e da faixa de peso de referencia de um setor (FR-003 da 002; FR-001 da
 * 005). Sem os dois limites, o setor fica sem faixa.
 *
 * @param sectorId o identificador como veio no endereco; malformado, o setor nao e encontrado
 */
public record UpdateSectorCommand(
        String sectorId, String name, String description, String minimumWeight, String maximumWeight)
        implements Command<SectorId> {}
