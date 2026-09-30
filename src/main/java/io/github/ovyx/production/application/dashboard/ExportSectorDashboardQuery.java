package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.shared.application.Query;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetFile;

/**
 * A planilha do painel de um setor num periodo (US2 da 007).
 *
 * @param period o periodo; {@code null} vale {@link DashboardPeriod#TODAY}, como na consulta do painel
 */
public record ExportSectorDashboardQuery(String sectorId, DashboardPeriod period) implements Query<SpreadsheetFile> {}
