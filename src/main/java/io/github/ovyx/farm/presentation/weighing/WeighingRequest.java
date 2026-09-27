package io.github.ovyx.farm.presentation.weighing;

import io.github.ovyx.shared.presentation.RawJsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * Registro ou correcao de pesagem, como o cliente envia.
 *
 * <p>O peso chega como esta no JSON, numero ou texto: convertido na borda, um "161,4" tornava o corpo
 * ilegivel e escondia a violacao da data. O dominio recebe o texto e recusa no proprio campo, junto das
 * demais falhas (FR-014 e R-007 da 005).
 */
@Schema(description = "Registro ou correção de pesagem. O peso pode vir como número ou como texto, com vírgula ou ponto.")
public record WeighingRequest(
        @Schema(
                type = "string",
                format = "date",
                description = "Dia da pesagem, não futuro no fuso da granja",
                example = "2026-09-24")
        String weighedOn,
        @Schema(
                type = "number",
                description = "Peso médio da amostra, em gramas, com até uma casa decimal",
                example = "161.4")
        JsonNode averageWeight) {

    /** O peso como texto, do jeito que veio. */
    public String rawAverageWeight() {
        return RawJsonValue.of(averageWeight);
    }
}
