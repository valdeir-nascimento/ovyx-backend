package io.github.ovyx.farm.presentation.sector;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cadastro ou edicao de setor, como o cliente envia.
 *
 * <p>Sem anotacoes de validacao, de proposito: todas as violacoes vem do agregado, de uma vez
 * (FR-017).
 */
@Schema(description = "Cadastro ou edição de setor")
public record SectorRequest(
        @Schema(description = "Nome do setor, de 2 a 80 caracteres, único entre os ativos", example = "Codornas — Galpão 4")
        String name,
        @Schema(
                description = "Espécie, linhagem, galpão ou o que ajude a reconhecer o setor; até 500 caracteres",
                example = "Codornas japonesas em postura, baterias A e B")
        String description) {}
