package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.shared.application.Query;

/** A sugestao para o formulario de relatorio novo do setor (FR-004). */
public record SuggestDailyReportQuery(String sectorId) implements Query<DailyReportSuggestion> {}
