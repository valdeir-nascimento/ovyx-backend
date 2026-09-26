package io.github.ovyx.production.presentation.dailyreport;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ovyx.production.application.dailyreport.DailyReportSuggestion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** Valores sugeridos para o formulario de relatorio novo. */
@Schema(description = "Valores sugeridos para o formulário de relatório novo")
public record DailyReportSuggestionResponse(
        @Schema(description = "Hoje, no fuso da granja", example = "2026-09-25") LocalDate collectionDate,
        @Schema(description = "Agora, no fuso da granja, `HH:mm`", example = "06:42") String collectionTime,
        @Schema(
                description = "Saldo do relatório mais recente do setor ou, no primeiro, a soma das aves das gaiolas ativas",
                example = "96")
        int openingBirdCount,
        @Schema(
                description = "Idade do relatório mais recente mais as semanas completas passadas; ausente no primeiro relatório",
                example = "20")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer flockAge) {

    public static DailyReportSuggestionResponse from(DailyReportSuggestion suggestion) {
        return new DailyReportSuggestionResponse(
                suggestion.collectionDate(),
                Times.format(suggestion.collectionTime()),
                suggestion.openingBirdCount(),
                suggestion.flockAge());
    }
}
