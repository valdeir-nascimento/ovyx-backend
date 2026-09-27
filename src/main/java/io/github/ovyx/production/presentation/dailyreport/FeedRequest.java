package io.github.ovyx.production.presentation.dailyreport;

import io.github.ovyx.shared.presentation.RawJsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * Lancamento ou correcao da racao de uma gaiola, como o cliente envia. O consumo chega como esta no JSON,
 * numero ou texto, e o dominio recusa o que nao e inteiro, junto da formula (FR-020 da 004).
 */
@Schema(
        name = "FeedRequest",
        description = "Lançamento ou correção da ração de uma gaiola. O consumo pode vir como número ou como texto.")
public record FeedRequest(
        @Schema(description = "Fórmula ativa, ou a que a gaiola já usa", example = "4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11")
        String formulaId,
        @Schema(type = "integer", description = "Consumo da gaiola no dia, em gramas", example = "1250")
        JsonNode consumption) {

    /** O consumo como texto, do jeito que veio. */
    public String rawConsumption() {
        return RawJsonValue.of(consumption);
    }
}
