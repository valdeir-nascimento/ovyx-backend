package io.github.ovyx.farm.presentation.cage;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * Cadastro ou edicao de gaiola, como o cliente envia.
 *
 * <p>O numero e as aves chegam como estao no JSON, e nao convertidos para inteiro na borda: convertido,
 * um "12.5" ou um texto tornava o corpo inteiro ilegivel e escondia as violacoes dos outros campos. O
 * dominio recebe o texto e recusa o que nao e inteiro, junto das demais violacoes (FR-017, R-013).
 */
@Schema(description = "Cadastro ou edição de gaiola")
public record CageRequest(
        @Schema(description = "Bateria, com até 3 letras ou dígitos; gravada em maiúsculas", example = "B")
        String battery,
        @Schema(type = "integer", description = "Número inteiro da gaiola na bateria, de 1 a 999", example = "7")
        JsonNode number,
        @Schema(
                type = "integer",
                description = "Quantidade inteira de aves, de 0 a 1.000; zero é gaiola vazia",
                example = "50")
        JsonNode birdCount) {

    /** O numero como texto, do jeito que veio. */
    public String rawNumber() {
        return raw(number);
    }

    /** As aves como texto, do jeito que vieram. */
    public String rawBirdCount() {
        return raw(birdCount);
    }

    /**
     * O valor como texto: ausente ou nulo vira {@code null}; numero inteiro, os digitos; numero com
     * casas, o decimal; texto, o proprio texto; qualquer outra coisa, o JSON dela, que o dominio recusa.
     */
    private static String raw(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isString()) {
            return node.asString();
        }
        if (node.isIntegralNumber()) {
            return node.bigIntegerValue().toString();
        }
        if (node.isNumber()) {
            return node.decimalValue().toPlainString();
        }
        return node.toString();
    }
}
