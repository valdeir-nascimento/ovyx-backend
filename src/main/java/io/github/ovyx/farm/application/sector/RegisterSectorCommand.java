package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.Command;

/**
 * Cadastro de um setor pelo administrador (FR-001).
 *
 * <p>Os campos chegam como foram digitados: quem valida, e devolve todas as violacoes de uma vez, e
 * o agregado (FR-017).
 */
public record RegisterSectorCommand(String name, String description) implements Command<SectorId> {}
