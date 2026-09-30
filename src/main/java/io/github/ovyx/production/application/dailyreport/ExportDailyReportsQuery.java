package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.shared.application.Query;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetFile;
import java.time.LocalDate;

public record ExportDailyReportsQuery(String sectorId, LocalDate from, LocalDate to) implements Query<SpreadsheetFile> {}
