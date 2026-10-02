package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.Command;

/**
 * Edicao do nome, da descricao, da faixa de peso de referencia, da meta de produtividade e do dia da pesagem
 * de um setor (FR-003 da 002; FR-001 da 005; FR-002 da 008; FR-001 da 010). Sem os dois limites, o setor fica
 * sem faixa; sem o dia, sem dia fixo.
 *
 * @param sectorId o identificador como veio no endereco; malformado, o setor nao e encontrado
 */
public record UpdateSectorCommand(
        String sectorId,
        String name,
        String description,
        String minimumWeight,
        String maximumWeight,
        String layingRateTarget,
        String weighingDay)
        implements Command<SectorId> {}
