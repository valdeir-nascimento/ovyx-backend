package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.application.dailyreport.PeriodTotals;
import io.github.ovyx.production.domain.model.ProductionStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * As contas do painel, as mesmas para um setor e para a granja toda (R-003 da 009): os quatro indicadores do periodo
 * contra o anterior e a serie dos ultimos 7 dias.
 *
 * <p>Cada conta recebe os relatorios somados, de um setor ou de varios, e soma todos os de cada data: a
 * produtividade e a soma dos ovos sobre a soma das aves, e o custo conta so os relatorios com a racao completa
 * ({@link PeriodTotals}). Num setor ha no maximo um relatorio por data, e o resultado e o do painel do setor; na
 * granja, os setores se somam com as mesmas contas, e o total nao tem como divergir da soma das abas (FR-019).
 *
 * <p>Os pendentes dos indicadores contam relatorios: num setor, um por dia; na granja, um por setor e por dia.
 */
final class DashboardFigures {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    /** Os dias da serie, de hoje - 6 ate hoje: os mesmos para o grafico e para a meta da granja. */
    static final int TREND_DAYS = 7;

    private DashboardFigures() {}

    /** Os quatro indicadores dos relatorios do periodo, comparados com os do periodo anterior. */
    static Indicators indicators(List<ReportDay> current, List<ReportDay> previous) {
        int productionPending = (int) current.stream()
                .filter(day -> day.productionStatus() == ProductionStatus.PENDING)
                .count();
        PeriodTotals now = totals(current);
        PeriodTotals before = totals(previous);
        return new Indicators(
                inPercent(now.production(), before.production(), GoodDirection.UP, productionPending),
                inPoints(now.layingRate(), before.layingRate(), productionPending),
                inPercent(now.feedCost(), before.feedCost(), GoodDirection.DOWN, now.incompleteDays()),
                inPercent(now.costPerEgg(), before.costPerEgg(), GoodDirection.DOWN, now.incompleteDays()));
    }

    /**
     * Os 7 dias de hoje - 6 ate hoje, cada um somando todos os relatorios da data: sem relatorio, sem valores; com a
     * racao pendente em todos, sem os custos.
     */
    static List<DashboardDay> trend(List<ReportDay> days, LocalDate today) {
        List<DashboardDay> trend = new ArrayList<>();
        for (LocalDate date = today.minusDays(TREND_DAYS - 1L); !date.isAfter(today); date = date.plusDays(1)) {
            trend.add(dayOf(within(days, date, date), date));
        }
        return trend;
    }

    /** Os relatorios de {@code from} a {@code to}, inclusive. */
    static List<ReportDay> within(List<ReportDay> days, LocalDate from, LocalDate to) {
        return days.stream()
                .filter(day -> !day.date().isBefore(from) && !day.date().isAfter(to))
                .toList();
    }

    /** As contas de um conjunto de relatorios, as mesmas da linha de totais da planilha (R-007 da 007). */
    static PeriodTotals totals(List<ReportDay> days) {
        return PeriodTotals.of(days.stream().map(ReportDay::periodDay).toList());
    }

    private static DashboardDay dayOf(List<ReportDay> reports, LocalDate date) {
        if (reports.isEmpty()) {
            return new DashboardDay(date, null, null, null, null);
        }
        PeriodTotals totals = totals(reports);
        return new DashboardDay(date, totals.eggs(), totals.layingRate(), totals.feedCost(), totals.costPerEgg());
    }

    /** A variacao em porcentagem com uma casa, sobre os valores ja arredondados (os que a tela mostra). */
    private static Indicator inPercent(BigDecimal value, BigDecimal previous, GoodDirection good, int incomplete) {
        BigDecimal change = comparable(value, previous)
                ? value.subtract(previous).multiply(HUNDRED).divide(previous, 1, RoundingMode.HALF_UP)
                : null;
        return new Indicator(value, previous, change, good, incomplete);
    }

    /** A variacao da produtividade, em pontos percentuais com duas casas. */
    private static Indicator inPoints(BigDecimal value, BigDecimal previous, int incomplete) {
        BigDecimal change =
                comparable(value, previous) ? value.subtract(previous).setScale(2, RoundingMode.HALF_UP) : null;
        return new Indicator(value, previous, change, GoodDirection.UP, incomplete);
    }

    private static boolean comparable(BigDecimal value, BigDecimal previous) {
        return value != null && previous != null && previous.signum() != 0;
    }
}
