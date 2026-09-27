package io.github.ovyx.farm.presentation.formula;

import io.github.ovyx.shared.presentation.RawJsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * Cadastro ou edicao de formula de racao, como o cliente envia.
 *
 * <p>O preco e o consumo esperado chegam como estao no JSON, numero ou texto: convertidos na borda, um
 * "2,85" ou um "28.5" tornavam o corpo ilegivel e escondiam as violacoes dos outros campos. O dominio
 * recebe o texto e recusa no proprio campo, junto das demais falhas (FR-020 e R-011 da 004).
 */
@Schema(description = "Cadastro ou edição de fórmula. O preço e o consumo podem vir como número ou como texto.")
public record FeedFormulaRequest(
        @Schema(description = "Nome da fórmula, único entre todas, ativas e inativas", example = "Postura Plus")
        String name,
        @Schema(
                type = "number",
                description = "Preço por quilo, em reais, com até duas casas decimais",
                example = "2.85")
        JsonNode pricePerKg,
        @Schema(type = "integer", description = "Consumo esperado por ave ao dia, em gramas", example = "28")
        JsonNode expectedIntake,
        @Schema(
                description = "Ingredientes e indicação",
                example = "Milho, farelo de soja, calcário e premix vitamínico; para codornas em postura")
        String description) {

    /** O preco como texto, do jeito que veio. */
    public String rawPricePerKg() {
        return RawJsonValue.of(pricePerKg);
    }

    /** O consumo esperado como texto, do jeito que veio. */
    public String rawExpectedIntake() {
        return RawJsonValue.of(expectedIntake);
    }
}
