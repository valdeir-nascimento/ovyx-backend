package io.github.ovyx.production.presentation.dailyreport;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ovyx.production.application.dailyreport.DailyReportDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** O relatorio com as gaiolas, os lancamentos e os totais do dia. */
@Schema(description = "O relatório com as gaiolas, os lançamentos e os totais do dia")
public record DailyReportDetailResponse(
        @Schema(example = "6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55") UUID id,
        ReportingSectorResponse sector,
        @Schema(example = "2026-09-24") LocalDate collectionDate,
        @Schema(example = "06:30") String collectionTime,
        @Schema(description = "Aves no início do dia", example = "98") int openingBirdCount,
        @Schema(description = "Idade do lote, em semanas", example = "20") int flockAge,
        @Schema(description = "Ausente quando não há observação", example = "Bebedouro da bateria B trocado.")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String note,
        @Schema(description = "Verdadeiro depois da confirmação de dia sem ocorrência", example = "true")
        boolean noMortalityConfirmed,
        ActorResponse openedBy,
        @Schema(example = "2026-09-24T09:31:40Z") Instant openedAt,
        @Schema(description = "Ausente enquanto ninguém corrigiu o relatório") @JsonInclude(JsonInclude.Include.NON_NULL)
        ActorResponse lastCorrectedBy,
        @Schema(description = "Ausente enquanto ninguém corrigiu o relatório", example = "2026-09-24T11:05:12Z")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant lastCorrectedAt,
        ProductionTotalsResponse production,
        MortalityTotalsResponse mortality,
        @Schema(description = "As gaiolas do relatório, por bateria e número") List<ReportCageDetailResponse> cages) {

    public static DailyReportDetailResponse from(DailyReportDetail detail) {
        return new DailyReportDetailResponse(
                detail.id(),
                ReportingSectorResponse.from(detail.sector()),
                detail.collectionDate(),
                Times.format(detail.collectionTime()),
                detail.openingBirdCount(),
                detail.flockAge(),
                detail.note(),
                detail.noMortalityConfirmed(),
                ActorResponse.from(detail.openedBy()),
                detail.openedAt(),
                ActorResponse.from(detail.lastCorrectedBy()),
                detail.lastCorrectedAt(),
                ProductionTotalsResponse.from(detail.production()),
                MortalityTotalsResponse.from(detail.mortality()),
                detail.cages().stream().map(ReportCageDetailResponse::from).toList());
    }
}
