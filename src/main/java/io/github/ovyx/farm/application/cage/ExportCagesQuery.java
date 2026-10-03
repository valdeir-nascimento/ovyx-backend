package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.shared.application.Query;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetFile;

/**
 * A exportacao das gaiolas de um setor (feature 007), com os filtros da pesquisa.
 *
 * @param weighing o filtro de pesagem; ausente, nao filtra (feature 010)
 */
public record ExportCagesQuery(
        String sectorId, String code, String battery, StatusFilter status, WeighingFilter weighing)
        implements Query<SpreadsheetFile> {}
