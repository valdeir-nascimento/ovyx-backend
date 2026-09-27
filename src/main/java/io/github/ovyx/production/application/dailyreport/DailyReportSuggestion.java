package io.github.ovyx.production.application.dailyreport;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Valores sugeridos para o formulario de relatorio novo (FR-004).
 *
 * @param flockAge a idade sugerida, ou {@code null} no primeiro relatorio do setor
 */
public record DailyReportSuggestion(
        LocalDate collectionDate, LocalTime collectionTime, int openingBirdCount, Integer flockAge) {}
