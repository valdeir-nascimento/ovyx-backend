package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.shared.application.PageResponse;

/** Uma pagina dos relatorios do setor, com o setor junto: a tela precisa dos dois numa consulta so. */
public record DailyReportPage(ReportingSector sector, PageResponse<DailyReportSummary> page) {}
