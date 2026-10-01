package io.github.ovyx.production.fixtures;

import io.github.ovyx.production.application.dailyreport.ReportingSector;
import io.github.ovyx.production.application.dashboard.CageWatchReading;
import io.github.ovyx.production.application.dashboard.DashboardDirectory;
import io.github.ovyx.production.application.dashboard.DashboardSectors;
import io.github.ovyx.production.application.dashboard.LayingRateTarget;
import io.github.ovyx.production.application.dashboard.LatestReport;
import io.github.ovyx.production.application.dashboard.MortalityBaseline;
import io.github.ovyx.production.application.dashboard.ReportDay;
import io.github.ovyx.production.domain.model.SectorId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Dublê da consulta do painel: os setores e os relatórios somados que o teste monta, e os pedidos feitos. */
public final class InMemoryDashboardDirectory implements DashboardDirectory {

    private final Map<SectorId, ReportingSector> sectors = new LinkedHashMap<>();
    private final Map<SectorId, LayingRateTarget> targets = new LinkedHashMap<>();
    private int askedTargets;
    private final List<ReportDay> days = new ArrayList<>();
    private DashboardSectors overview = new DashboardSectors(0, 0, List.of());
    private CageWatchReading watch = new CageWatchReading(List.of(), null, new MortalityBaseline(0, 0));
    private List<LatestReport> latest = List.of();
    private int askedLatest;
    private LocalDate askedFrom;
    private LocalDate askedTo;

    public ReportingSector put(ReportingSector sector) {
        sectors.put(SectorId.of(sector.id()), sector);
        return sector;
    }

    /** A meta do setor; sem ela, os 85% dos setores anteriores à feature 008. */
    public void answerLayingRateTarget(SectorId sectorId, String target) {
        targets.put(sectorId, new LayingRateTarget(new BigDecimal(target)));
    }

    /** Quantas vezes o handler leu a meta de um setor. */
    public int askedTargets() {
        return askedTargets;
    }

    public void add(ReportDay day) {
        days.add(day);
    }

    public void answerOverview(DashboardSectors sectorsToday) {
        this.overview = sectorsToday;
    }

    public void answerCageWatch(CageWatchReading reading) {
        this.watch = reading;
    }

    public void answerLatestReports(List<LatestReport> reports) {
        this.latest = List.copyOf(reports);
    }

    /** Quantos relatórios recentes o handler pediu. */
    public int askedLatest() {
        return askedLatest;
    }

    /** O primeiro dia que o handler pediu na última leitura dos relatórios. */
    public LocalDate askedFrom() {
        return askedFrom;
    }

    /** O último dia que o handler pediu na última leitura dos relatórios. */
    public LocalDate askedTo() {
        return askedTo;
    }

    @Override
    public DashboardSectors overview(LocalDate today) {
        return overview;
    }

    @Override
    public Optional<ReportingSector> sector(SectorId sectorId) {
        return Optional.ofNullable(sectors.get(sectorId));
    }

    @Override
    public LayingRateTarget layingRateTarget(SectorId sectorId) {
        askedTargets++;
        return targets.getOrDefault(sectorId, new LayingRateTarget(new BigDecimal("85")));
    }

    @Override
    public List<ReportDay> reportDays(SectorId sectorId, LocalDate from, LocalDate to) {
        this.askedFrom = from;
        this.askedTo = to;
        return days.stream()
                .filter(day -> !day.date().isBefore(from) && !day.date().isAfter(to))
                .toList();
    }

    @Override
    public CageWatchReading cageWatch(SectorId sectorId, LocalDate today) {
        return watch;
    }

    @Override
    public List<LatestReport> latestReports(SectorId sectorId, int limit) {
        this.askedLatest = limit;
        return latest.stream().limit(limit).toList();
    }
}
