package io.github.ovyx.production.presentation.dailyreport;

import io.github.ovyx.production.application.dailyreport.DailyReportPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Uma pagina dos relatorios do setor, com o setor junto, no formato do contrato. */
@Schema(description = "Uma página dos relatórios do setor, com o setor junto")
public record DailyReportPageResponse(
        ReportingSectorResponse sector,
        List<DailyReportSummaryResponse> content,
        @Schema(example = "0") int page,
        @Schema(example = "20") int size,
        @Schema(description = "Quantos relatórios o setor tem, em todas as páginas", example = "3") long totalElements,
        @Schema(example = "1") int totalPages) {

    public static DailyReportPageResponse from(DailyReportPage page) {
        return new DailyReportPageResponse(
                ReportingSectorResponse.from(page.sector()),
                page.page().content().stream().map(DailyReportSummaryResponse::from).toList(),
                page.page().metadata().page(),
                page.page().metadata().size(),
                page.page().metadata().totalElements(),
                page.page().metadata().totalPages());
    }
}
