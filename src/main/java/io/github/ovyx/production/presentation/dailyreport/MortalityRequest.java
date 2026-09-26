package io.github.ovyx.production.presentation.dailyreport;

import io.github.ovyx.shared.presentation.RawJsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * Lancamento ou correcao da mortalidade de uma gaiola, como o cliente envia. Mortes e descartes chegam
 * como estao no JSON, e o dominio recusa o que nao e inteiro, junto das demais violacoes (R-010); em
 * branco vale zero.
 */
@Schema(description = "Lançamento ou correção da mortalidade de uma gaiola. Em branco vale zero.")
public record MortalityRequest(
        @Schema(type = "integer", description = "Aves encontradas mortas", example = "1") JsonNode deaths,
        @Schema(type = "integer", description = "Aves descartadas — retiradas do plantel", example = "1")
        JsonNode culls,
        @Schema(
                description = "Causa provável, sintomas, destino das aves",
                example = "Prostração e penas eriçadas; uma ave separada para necropsia.")
        String note) {

    /** As mortes como texto, do jeito que vieram. */
    public String rawDeaths() {
        return RawJsonValue.of(deaths);
    }

    /** Os descartes como texto, do jeito que vieram. */
    public String rawCulls() {
        return RawJsonValue.of(culls);
    }
}
