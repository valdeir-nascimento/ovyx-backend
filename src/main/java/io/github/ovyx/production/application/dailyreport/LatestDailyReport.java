package io.github.ovyx.production.application.dailyreport;

import java.time.LocalDate;

/** O relatorio mais recente do setor, pela data da coleta: a base da sugestao do seguinte (FR-004). */
public record LatestDailyReport(LocalDate collectionDate, int flockAge, int closingBirdCount) {}
