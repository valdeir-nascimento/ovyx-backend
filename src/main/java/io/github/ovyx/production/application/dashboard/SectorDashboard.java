package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.application.dailyreport.DailyReportTotals;
import io.github.ovyx.production.application.dailyreport.PeriodTotals;
import io.github.ovyx.production.application.dailyreport.ReportingSector;
import io.github.ovyx.production.domain.model.ProductionStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * O painel de um setor num periodo (US1 da 006; FR-005 a FR-009, R-005, R-006), calculado a partir dos
 * relatorios somados, sem banco.
 *
 * <p>As contas sao as do relatorio ({@link DailyReportTotals}): a produtividade e a soma dos ovos sobre a soma
 * das aves, e o custo e a soma exata, arredondada so no fim. Num periodo de varios dias, a soma sobre a soma
 * pesa cada dia pelo tamanho dele. O dia sem relatorio fica de fora, e nao conta como zero; o dia com a racao
 * pendente fica fora dos custos, e o indicador diz quantos ficaram.
 *
 * @param todayReport o relatorio de hoje; {@code null} se hoje ainda nao foi aberto
 * @param trend os 7 dias de hoje - 6 ate hoje, qualquer que seja o periodo
 * @param target a meta de produtividade ({@link LayingRateTarget})
 * @param targetStatus o ultimo dia com relatorio da serie diante da meta; {@code null} sem dia com relatorio
 * @param grades a classificacao dos ovos do periodo; {@code null} sem ovo
 * @param alerts os alertas e as pendencias de hoje ({@link DashboardAlerts})
 * @param openAlerts quantos alertas e pendencias estao abertos
 * @param latestReports os relatorios mais recentes do setor, qualquer que seja o periodo
 */
public record SectorDashboard(
        ReportingSector sector,
        DashboardPeriod period,
        LocalDate from,
        LocalDate to,
        TodayReport todayReport,
        Indicators indicators,
        List<DashboardDay> trend,
        BigDecimal target,
        TargetStatus targetStatus,
        EggGrading grades,
        List<DashboardAlert> alerts,
        int openAlerts,
        List<LatestReport> latestReports) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int TREND_DAYS = 7;

    public SectorDashboard {
        trend = List.copyOf(trend);
        alerts = List.copyOf(alerts);
        latestReports = List.copyOf(latestReports);
    }

    /**
     * O painel do setor.
     *
     * @param today o dia de hoje da granja
     * @param days os relatorios do setor de hoje - 13 ate hoje, em qualquer ordem
     */
    public static SectorDashboard of(ReportingSector sector, DashboardPeriod period, LocalDate today, List<ReportDay> days) {
        List<ReportDay> current = within(days, period.from(today), period.to(today));
        List<ReportDay> previous = within(days, period.previousFrom(today), period.previousTo(today));
        int productionPending = pending(current, day -> day.productionStatus() == ProductionStatus.PENDING);
        PeriodTotals now = totals(current);
        PeriodTotals before = totals(previous);
        Indicators indicators = new Indicators(
                inPercent(now.production(), before.production(), GoodDirection.UP, productionPending),
                inPoints(now.layingRate(), before.layingRate(), productionPending),
                inPercent(now.feedCost(), before.feedCost(), GoodDirection.DOWN, now.incompleteDays()),
                inPercent(now.costPerEgg(), before.costPerEgg(), GoodDirection.DOWN, now.incompleteDays()));
        TodayReport todayReport = dayOf(days, today).map(TodayReport::of).orElse(null);
        List<DashboardDay> trend = trend(days, today);
        return new SectorDashboard(
                sector,
                period,
                period.from(today),
                period.to(today),
                todayReport,
                indicators,
                trend,
                LayingRateTarget.VALUE,
                targetStatus(trend),
                EggGrading.of(current),
                List.of(),
                0,
                List.of());
    }

    /** O mesmo painel, com os alertas de hoje. */
    public SectorDashboard withAlerts(List<DashboardAlert> alertsOfToday) {
        return new SectorDashboard(
                sector,
                period,
                from,
                to,
                todayReport,
                indicators,
                trend,
                target,
                targetStatus,
                grades,
                alertsOfToday,
                alertsOfToday.size(),
                latestReports);
    }

    /** O mesmo painel, com os relatorios mais recentes do setor. */
    public SectorDashboard withLatestReports(List<LatestReport> latest) {
        return new SectorDashboard(
                sector,
                period,
                from,
                to,
                todayReport,
                indicators,
                trend,
                target,
                targetStatus,
                grades,
                alerts,
                openAlerts,
                latest);
    }

    /** O ultimo dia da serie com relatorio diante da meta, com a meta incluida (FR-010). */
    private static TargetStatus targetStatus(List<DashboardDay> trend) {
        return trend.reversed().stream()
                .map(DashboardDay::layingRate)
                .filter(Objects::nonNull)
                .findFirst()
                .map(rate -> rate.compareTo(LayingRateTarget.VALUE) >= 0 ? TargetStatus.ABOVE : TargetStatus.BELOW)
                .orElse(null);
    }

    // ---------------------------------------------------------------- contas de um conjunto de dias

    /** As contas de um conjunto de dias, as mesmas da linha de totais da planilha (R-007 da 007). */
    private static PeriodTotals totals(List<ReportDay> days) {
        return PeriodTotals.of(days.stream().map(ReportDay::periodDay).toList());
    }

    private static int pending(List<ReportDay> days, Predicate<ReportDay> isPending) {
        return (int) days.stream().filter(isPending).count();
    }

    // ---------------------------------------------------------------- variacoes

    /** A variacao em porcentagem com uma casa, sobre os valores ja arredondados (os que a tela mostra). */
    private static Indicator inPercent(BigDecimal value, BigDecimal previous, GoodDirection good, int incompleteDays) {
        BigDecimal change = comparable(value, previous)
                ? value.subtract(previous).multiply(HUNDRED).divide(previous, 1, RoundingMode.HALF_UP)
                : null;
        return new Indicator(value, previous, change, good, incompleteDays);
    }

    /** A variacao da produtividade, em pontos percentuais com duas casas. */
    private static Indicator inPoints(BigDecimal value, BigDecimal previous, int incompleteDays) {
        BigDecimal change =
                comparable(value, previous) ? value.subtract(previous).setScale(2, RoundingMode.HALF_UP) : null;
        return new Indicator(value, previous, change, GoodDirection.UP, incompleteDays);
    }

    private static boolean comparable(BigDecimal value, BigDecimal previous) {
        return value != null && previous != null && previous.signum() != 0;
    }

    // ---------------------------------------------------------------- serie

    /** Os 7 dias ate hoje: sem relatorio, sem valores; com a racao pendente, sem os custos. */
    private static List<DashboardDay> trend(List<ReportDay> days, LocalDate today) {
        List<DashboardDay> trend = new ArrayList<>();
        for (LocalDate date = today.minusDays(TREND_DAYS - 1L); !date.isAfter(today); date = date.plusDays(1)) {
            LocalDate current = date;
            trend.add(dayOf(days, date)
                    .map(day -> dashboardDay(List.of(day), current))
                    .orElseGet(() -> new DashboardDay(current, null, null, null, null)));
        }
        return trend;
    }

    private static DashboardDay dashboardDay(List<ReportDay> day, LocalDate date) {
        PeriodTotals totals = totals(day);
        return new DashboardDay(date, totals.eggs(), totals.layingRate(), totals.feedCost(), totals.costPerEgg());
    }

    private static Optional<ReportDay> dayOf(List<ReportDay> days, LocalDate date) {
        return days.stream().filter(day -> day.date().equals(date)).findFirst();
    }

    private static List<ReportDay> within(List<ReportDay> days, LocalDate from, LocalDate to) {
        return days.stream()
                .filter(day -> !day.date().isBefore(from) && !day.date().isAfter(to))
                .toList();
    }
}
