package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.shared.application.Query;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetFile;

public record ExportCagesQuery(String sectorId, String code, String battery, StatusFilter status)
        implements Query<SpreadsheetFile> {}
