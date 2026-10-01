package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.shared.application.Query;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetFile;

/**
 * A planilha da granja toda num periodo (US4 da 009).
 *
 * @param period o periodo; sem ele, hoje
 */
public record ExportFarmDashboardQuery(DashboardPeriod period) implements Query<SpreadsheetFile> {}
