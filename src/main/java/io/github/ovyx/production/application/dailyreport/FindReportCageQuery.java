package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.shared.application.Query;

/** Uma gaiola do relatorio, com os identificadores como vieram do endereco. */
public record FindReportCageQuery(String sectorId, String reportId, String cageId) implements Query<ReportCageDetail> {}
