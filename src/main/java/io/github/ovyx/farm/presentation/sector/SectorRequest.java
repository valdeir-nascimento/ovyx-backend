package io.github.ovyx.farm.presentation.sector;

import io.github.ovyx.shared.presentation.RawJsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

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
        String description,
        @Schema(
                type = "integer",
                description = "Peso mínimo de referência das aves, em gramas. Vai junto do máximo; sem os dois, o"
                        + " setor fica sem faixa.",
                example = "155")
        JsonNode minimumWeight,
        @Schema(
                type = "integer",
                description = "Peso máximo de referência das aves, em gramas, maior que o mínimo",
                example = "175")
        JsonNode maximumWeight) {

    /** O peso minimo como texto, do jeito que veio; ausente, nulo (feature 005). */
    public String rawMinimumWeight() {
        return RawJsonValue.of(minimumWeight);
    }

    /** O peso maximo como texto, do jeito que veio; ausente, nulo (feature 005). */
    public String rawMaximumWeight() {
        return RawJsonValue.of(maximumWeight);
    }
}
