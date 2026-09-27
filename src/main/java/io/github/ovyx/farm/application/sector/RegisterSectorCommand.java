package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.Command;

/**
 * Cadastro de um setor pelo administrador (FR-001), com a faixa de peso de referencia opcional (FR-001 da
 * 005).
 *
 * <p>Os campos chegam como foram digitados, os limites da faixa inclusive: quem valida, e devolve todas as
 * violacoes de uma vez, e o agregado (FR-017).
 */
public record RegisterSectorCommand(String name, String description, String minimumWeight, String maximumWeight)
        implements Command<SectorId> {}
