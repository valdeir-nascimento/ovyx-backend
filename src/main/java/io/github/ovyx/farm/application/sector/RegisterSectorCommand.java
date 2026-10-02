package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.Command;

/**
 * Cadastro de um setor pelo administrador (FR-001), com a faixa de peso de referencia opcional (FR-001 da
 * 005) e a meta de produtividade obrigatoria (FR-001 da 008).
 *
 * <p>O dia da pesagem e opcional (FR-001 da 010).
 *
 * <p>Os campos chegam como foram digitados, os limites da faixa, a meta e o dia inclusive: quem valida, e devolve todas as
 * violacoes de uma vez, e o agregado (FR-017).
 */
public record RegisterSectorCommand(
        String name,
        String description,
        String minimumWeight,
        String maximumWeight,
        String layingRateTarget,
        String weighingDay)
        implements Command<SectorId> {}
