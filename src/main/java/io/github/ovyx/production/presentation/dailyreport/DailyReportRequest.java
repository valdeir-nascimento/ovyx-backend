package io.github.ovyx.production.presentation.dailyreport;

import io.github.ovyx.shared.presentation.RawJsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * Abertura ou correcao dos dados gerais do relatorio, como o cliente envia. As quantidades chegam como
 * estao no JSON, e o dominio recusa o que nao e inteiro, junto das demais violacoes (R-010).
 */
@Schema(description = "Abertura ou correção dos dados gerais do relatório")
public record DailyReportRequest(
        @Schema(description = "Data da coleta, `AAAA-MM-DD`; não futura no fuso da granja; única no setor", example = "2026-09-25")
        String collectionDate,
        @Schema(description = "Hora da coleta, `HH:mm`", example = "06:42") String collectionTime,
        @Schema(type = "integer", description = "Aves no início do dia, de 1 a 1.000.000", example = "96")
        JsonNode openingBirdCount,
        @Schema(type = "integer", description = "Idade do lote, em semanas, de 1 a 150", example = "20")
        JsonNode flockAge,
        @Schema(description = "Algo fora do comum na coleta, até 500 caracteres", example = "Temperatura alta no fim da tarde de ontem.")
        String note) {

    /** As aves como texto, do jeito que vieram. */
    public String rawOpeningBirdCount() {
        return RawJsonValue.of(openingBirdCount);
    }

    /** A idade como texto, do jeito que veio. */
    public String rawFlockAge() {
        return RawJsonValue.of(flockAge);
    }
}
