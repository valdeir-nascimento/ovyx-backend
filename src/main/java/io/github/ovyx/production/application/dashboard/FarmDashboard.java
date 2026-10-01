package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.application.dailyreport.PeriodTotals;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * O painel da granja toda num periodo (feature 009): os setores ativos somados, com as contas do painel de um setor
 * ({@link DashboardFigures}), para os numeros serem os da soma das abas (FR-019).
 *
 * <p>So os setores ativos entram (FR-004). O setor sem relatorio no periodo nao conta como zero: ele fica fora das
 * somas e da contagem dos setores com relatorio (FR-008). Os pendentes dos indicadores contam relatorios, um por
 * setor e por dia (R-007).
 *
 * @param activeSectors os setores ativos
 * @param reportingSectors os setores ativos com ao menos um relatorio no periodo
 * @param trend os 7 dias de hoje - 6 ate hoje, qualquer que seja o periodo
 * @param target a meta da granja nos 7 dias; {@code null} sem relatorio
 * @param targetStatus o ultimo dia com relatorio diante da meta daquele dia; {@code null} sem dia com relatorio
 * @param grades a classificacao dos ovos do periodo, somada; {@code null} sem ovo
 * @param sectors a comparacao: um por setor ativo, na ordem das abas
 */
public record FarmDashboard(
        DashboardPeriod period,
        LocalDate from,
        LocalDate to,
        int activeSectors,
        int reportingSectors,
        Indicators indicators,
        List<FarmDay> trend,
        BigDecimal target,
        TargetStatus targetStatus,
        EggGrading grades,
        List<FarmSectorRow> sectors) {

    public FarmDashboard {
        trend = List.copyOf(trend);
        sectors = List.copyOf(sectors);
    }

    /**
     * O painel da granja.
     *
     * @param today o dia de hoje da granja
     * @param sectors os setores ativos, na ordem das abas
     * @param days os relatorios de hoje - 13 ate hoje, agrupados pelo setor; os de setor fora da lista sao ignorados
     * @param openAlerts os alertas abertos hoje em cada setor; o setor ausente tem zero
     */
    public static FarmDashboard of(
            DashboardPeriod period,
            LocalDate today,
            List<ActiveSector> sectors,
            Map<UUID, List<ReportDay>> days,
            Map<UUID, Integer> openAlerts) {
        Map<UUID, List<ReportDay>> active = activeOnly(sectors, days);
        List<ReportDay> all = active.values().stream().flatMap(List::stream).toList();
        LocalDate from = period.from(today);
        LocalDate to = period.to(today);
        List<ReportDay> current = DashboardFigures.within(all, from, to);
        List<ReportDay> previous = DashboardFigures.within(all, period.previousFrom(today), period.previousTo(today));
        Map<UUID, LayingRateTarget> targets =
                sectors.stream().collect(Collectors.toMap(ActiveSector::id, ActiveSector::target));
        List<FarmDay> trend = trend(active, all, today, targets);
        LayingRateTarget target = target(active, targets, today.minusDays(DashboardFigures.TREND_DAYS - 1L), today);
        return new FarmDashboard(
                period,
                from,
                to,
                sectors.size(),
                reporting(active, from, to),
                DashboardFigures.indicators(current, previous),
                trend,
                target == null ? null : target.value(),
                targetStatus(trend),
                EggGrading.of(current),
                rows(sectors, active, from, to, today, openAlerts));
    }

    /**
     * A comparacao: uma linha por setor ativo, na ordem dada, com os numeros do periodo dele, a meta e a situacao
     * diante dela, o relatorio de hoje e os alertas (FR-010 a FR-013).
     */
    private static List<FarmSectorRow> rows(
            List<ActiveSector> sectors,
            Map<UUID, List<ReportDay>> active,
            LocalDate from,
            LocalDate to,
            LocalDate today,
            Map<UUID, Integer> openAlerts) {
        List<FarmSectorRow> rows = new ArrayList<>();
        for (ActiveSector sector : sectors) {
            List<ReportDay> reports = active.getOrDefault(sector.id(), List.of());
            PeriodTotals totals = DashboardFigures.totals(DashboardFigures.within(reports, from, to));
            BigDecimal layingRate = totals.layingRate();
            rows.add(new FarmSectorRow(
                    new DashboardSector(sector.id(), sector.name()),
                    totals.days() == 0 ? null : totals.eggs(),
                    layingRate,
                    sector.target().value(),
                    layingRate == null
                            ? null
                            : sector.target().isMetBy(layingRate) ? TargetStatus.ABOVE : TargetStatus.BELOW,
                    totals.costPerEgg(),
                    DashboardFigures.within(reports, today, today).stream()
                            .findFirst()
                            .map(TodayReport::of)
                            .orElse(null),
                    openAlerts.getOrDefault(sector.id(), 0)));
        }
        return rows;
    }

    /** Os relatorios dos setores da lista, e so deles. */
    private static Map<UUID, List<ReportDay>> activeOnly(List<ActiveSector> sectors, Map<UUID, List<ReportDay>> days) {
        Set<UUID> ids = sectors.stream().map(ActiveSector::id).collect(Collectors.toSet());
        return days.entrySet().stream()
                .filter(entry -> ids.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** Quantos setores tem ao menos um relatorio de {@code from} a {@code to}. */
    private static int reporting(Map<UUID, List<ReportDay>> active, LocalDate from, LocalDate to) {
        return (int) active.values().stream()
                .filter(reports -> !DashboardFigures.within(reports, from, to).isEmpty())
                .count();
    }

    /** Os 7 dias somados, com a meta de cada dia e os setores com relatorio nele. */
    private static List<FarmDay> trend(
            Map<UUID, List<ReportDay>> active,
            List<ReportDay> all,
            LocalDate today,
            Map<UUID, LayingRateTarget> targets) {
        List<FarmDay> trend = new ArrayList<>();
        for (DashboardDay day : DashboardFigures.trend(all, today)) {
            LayingRateTarget target = target(active, targets, day.date(), day.date());
            trend.add(new FarmDay(
                    day.date(),
                    day.production(),
                    day.layingRate(),
                    day.feedCost(),
                    day.costPerEgg(),
                    target == null ? null : target.value(),
                    reporting(active, day.date(), day.date())));
        }
        return trend;
    }

    /** A meta da granja dos relatorios de {@code from} a {@code to}, ponderada pelas aves ({@link FarmTarget}). */
    private static LayingRateTarget target(
            Map<UUID, List<ReportDay>> active, Map<UUID, LayingRateTarget> targets, LocalDate from, LocalDate to) {
        List<FarmTarget.WeightedTarget> parts = new ArrayList<>();
        active.forEach((sectorId, reports) -> DashboardFigures.within(reports, from, to)
                .forEach(report -> parts.add(
                        new FarmTarget.WeightedTarget(targets.get(sectorId), report.openingBirdCount()))));
        return FarmTarget.of(parts);
    }

    /**
     * O ultimo dia da serie com relatorio diante da meta daquele dia, com a meta incluida: um dia com um setor so e
     * comparado com a meta dele, e nao com a de dois (R-005).
     */
    private static TargetStatus targetStatus(List<FarmDay> trend) {
        return trend.reversed().stream()
                .filter(day -> day.layingRate() != null && day.target() != null)
                .findFirst()
                .map(day -> new LayingRateTarget(day.target()).isMetBy(day.layingRate())
                        ? TargetStatus.ABOVE
                        : TargetStatus.BELOW)
                .orElse(null);
    }
}
