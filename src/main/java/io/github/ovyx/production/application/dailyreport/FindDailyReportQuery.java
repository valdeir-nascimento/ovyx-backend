package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.shared.application.Query;

/** O relatorio de um setor, com os identificadores como vieram do endereco. */
public record FindDailyReportQuery(String sectorId, String reportId) implements Query<DailyReportDetail> {}
