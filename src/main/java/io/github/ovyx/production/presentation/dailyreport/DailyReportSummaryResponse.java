package io.github.ovyx.production.presentation.dailyreport;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ovyx.production.application.dailyreport.DailyReportSummary;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

/** O relatorio na lista do setor. */
@Schema(description = "Relatório na lista do setor")
public record DailyReportSummaryResponse(
        @Schema(example = "6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55") UUID id,
        @Schema(example = "2026-09-24") LocalDate collectionDate,
        @Schema(example = "06:30") String collectionTime,
        @Schema(description = "Quem abriu o relatório", example = "Marina Alves") String openedByName,
        @Schema(description = "Idade do lote, em semanas", example = "20") int flockAge,
        @Schema(description = "Ovos coletados em todas as gaiolas", example = "89") int collectedEggs,
        @Schema(description = "Mortes mais descartes do dia", example = "2") int removedBirds,
        @Schema(description = "Saldo de aves, para o relatório seguinte", example = "96") int closingBirdCount,
        @Schema(description = "Ausente quando não há observação", example = "Bebedouro da bateria B trocado.")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String note,
        @Schema(example = "COMPLETE") ProductionStatus productionStatus,
        @Schema(description = "Gaiolas sem produção lançada", example = "1") int pendingCages,
        @Schema(example = "RECORDED") MortalityStatus mortalityStatus) {

    public static DailyReportSummaryResponse from(DailyReportSummary summary) {
        return new DailyReportSummaryResponse(
                summary.id(),
                summary.collectionDate(),
                Times.format(summary.collectionTime()),
                summary.openedByName(),
                summary.flockAge(),
                summary.collectedEggs(),
                summary.removedBirds(),
                summary.closingBirdCount(),
                summary.note(),
                summary.productionStatus(),
                summary.pendingCages(),
                summary.mortalityStatus());
    }
}
