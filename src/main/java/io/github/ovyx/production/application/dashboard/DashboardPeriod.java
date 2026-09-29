package io.github.ovyx.production.application.dashboard;

import java.time.LocalDate;

/**
 * O periodo do painel (FR-003, R-005 da 006): os dias que os indicadores e a classificacao somam, e o periodo
 * anterior, do mesmo tamanho, com que eles se comparam. "Hoje" e o dia da granja.
 */
public enum DashboardPeriod {
    /** Hoje, comparado com ontem. */
    TODAY(0, 1),
    /** Ontem, comparado com anteontem. */
    YESTERDAY(1, 1),
    /** Os 7 dias ate hoje, comparados com os 7 anteriores. */
    LAST_7_DAYS(0, 7);

    private final int daysBack;
    private final int length;

    DashboardPeriod(int daysBack, int length) {
        this.daysBack = daysBack;
        this.length = length;
    }

    /** O ultimo dia do periodo. */
    public LocalDate to(LocalDate today) {
        return today.minusDays(daysBack);
    }

    /** O primeiro dia do periodo. */
    public LocalDate from(LocalDate today) {
        return to(today).minusDays(length - 1L);
    }

    /** O ultimo dia do periodo anterior. */
    public LocalDate previousTo(LocalDate today) {
        return from(today).minusDays(1);
    }

    /** O primeiro dia do periodo anterior. */
    public LocalDate previousFrom(LocalDate today) {
        return previousTo(today).minusDays(length - 1L);
    }
}
