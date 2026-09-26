package io.github.ovyx.production.application.dailyreport;

/**
 * A mortalidade lancada numa gaiola do relatorio.
 *
 * @param note a observacao, ou {@code null}
 */
public record CageMortality(int deaths, int culls, String note) {}
