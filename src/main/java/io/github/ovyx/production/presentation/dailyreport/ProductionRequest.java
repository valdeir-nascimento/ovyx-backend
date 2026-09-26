package io.github.ovyx.production.presentation.dailyreport;

import io.github.ovyx.shared.presentation.RawJsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * Lancamento ou correcao da producao de uma gaiola, como o cliente envia. As quantidades chegam como
 * estao no JSON, e o dominio recusa o que nao e inteiro, junto das demais violacoes (R-010); a
 * classificacao ausente ou em branco vale zero.
 */
@Schema(
        description = "Lançamento ou correção da produção de uma gaiola. As quantidades podem vir como número ou como"
                + " texto; classificação em branco vale zero.")
public record ProductionRequest(
        @Schema(type = "integer", description = "Ovos coletados", example = "45") JsonNode eggs,
        @Schema(type = "integer", description = "Pequenos", example = "1") JsonNode small,
        @Schema(type = "integer", description = "Jumbo", example = "1") JsonNode jumbo,
        @Schema(type = "integer", description = "Sujos", example = "1") JsonNode dirty,
        @Schema(type = "integer", description = "Trincados", example = "2") JsonNode cracked,
        @Schema(type = "integer", description = "Com sangue", example = "1") JsonNode bloodSpot,
        @Schema(type = "integer", description = "Anormais", example = "1") JsonNode abnormal) {

    /** Os ovos como texto, do jeito que vieram. */
    public String rawEggs() {
        return RawJsonValue.of(eggs);
    }

    public String rawSmall() {
        return RawJsonValue.of(small);
    }

    public String rawJumbo() {
        return RawJsonValue.of(jumbo);
    }

    public String rawDirty() {
        return RawJsonValue.of(dirty);
    }

    public String rawCracked() {
        return RawJsonValue.of(cracked);
    }

    public String rawBloodSpot() {
        return RawJsonValue.of(bloodSpot);
    }

    public String rawAbnormal() {
        return RawJsonValue.of(abnormal);
    }
}
