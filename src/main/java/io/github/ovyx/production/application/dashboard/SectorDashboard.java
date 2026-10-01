package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.application.dailyreport.DailyReportTotals;
import io.github.ovyx.production.application.dailyreport.ReportingSector;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

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
 * @param target a meta de produtividade do setor, com duas casas ({@link LayingRateTarget}, feature 008)
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
     * @param target a meta de produtividade do setor (feature 008)
     */
    public static SectorDashboard of(
            ReportingSector sector,
            DashboardPeriod period,
            LocalDate today,
            List<ReportDay> days,
            LayingRateTarget target) {
        List<ReportDay> current = DashboardFigures.within(days, period.from(today), period.to(today));
        List<ReportDay> previous =
                DashboardFigures.within(days, period.previousFrom(today), period.previousTo(today));
        Indicators indicators = DashboardFigures.indicators(current, previous);
        TodayReport todayReport = DashboardFigures.within(days, today, today).stream()
                .findFirst()
                .map(TodayReport::of)
                .orElse(null);
        List<DashboardDay> trend = DashboardFigures.trend(days, today);
        return new SectorDashboard(
                sector,
                period,
                period.from(today),
                period.to(today),
                todayReport,
                indicators,
                trend,
                target.value(),
                targetStatus(trend, target),
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

    /** O ultimo dia da serie com relatorio diante da meta do setor, com a meta incluida (FR-010 da 006). */
    private static TargetStatus targetStatus(List<DashboardDay> trend, LayingRateTarget target) {
        return trend.reversed().stream()
                .map(DashboardDay::layingRate)
                .filter(Objects::nonNull)
                .findFirst()
                .map(rate -> target.isMetBy(rate) ? TargetStatus.ABOVE : TargetStatus.BELOW)
                .orElse(null);
    }
}
