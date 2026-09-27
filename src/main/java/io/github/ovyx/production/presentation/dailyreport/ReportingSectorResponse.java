package io.github.ovyx.production.presentation.dailyreport;

import io.github.ovyx.production.application.dailyreport.ReportingSector;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** O setor do relatorio, como a tela precisa dele. */
@Schema(description = "O setor do relatório")
public record ReportingSectorResponse(
        @Schema(example = "5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33") UUID id,
        @Schema(example = "Codornas — Galpão 4") String name,
        @Schema(description = "Num setor inativo, os relatórios são só de consulta", example = "ACTIVE") String status) {

    public static ReportingSectorResponse from(ReportingSector sector) {
        return new ReportingSectorResponse(sector.id(), sector.name(), sector.status());
    }
}
