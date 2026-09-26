package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.shared.application.Query;
import java.time.LocalDate;

/**
 * A lista de relatorios do setor.
 *
 * @param collectionDate so o relatorio deste dia; {@code null} nao filtra
 */
public record ListDailyReportsQuery(String sectorId, LocalDate collectionDate, int page, int size)
        implements Query<DailyReportPage> {}
