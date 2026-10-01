package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.application.common.ProductionRefusals;
import io.github.ovyx.production.application.dailyreport.ReportingSector;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * O painel de um setor (FR-005 a FR-009 da 006). Le os relatorios de hoje - 13 ate hoje, o bastante para os
 * 7 dias e os 7 anteriores, e a meta do setor (feature 008), e deixa as contas ao {@link SectorDashboard} e ao
 * {@link DashboardAlerts}. O setor inativo continua consultavel.
 */
public class GetSectorDashboardQueryHandler implements QueryHandler<GetSectorDashboardQuery, SectorDashboard> {

    /** Os dias lidos antes de hoje: os 7 dias e os 7 anteriores a eles. */
    private static final int DAYS_READ_BEFORE_TODAY = 13;

    /** Os relatorios mais recentes que o painel lista (FR-019). */
    private static final int LATEST_REPORTS = 4;

    private final DashboardDirectory directory;
    private final FarmCalendar calendar;

    public GetSectorDashboardQueryHandler(DashboardDirectory directory, FarmCalendar calendar) {
        this.directory = directory;
        this.calendar = calendar;
    }

    @Override
    public Result<SectorDashboard> handle(GetSectorDashboardQuery query) {
        Optional<SectorId> sectorId = SectorId.parse(query.sectorId());
        Optional<ReportingSector> sector = sectorId.flatMap(directory::sector);
        if (sector.isEmpty()) {
            return Result.failure(ProductionRefusals.sectorNotFound());
        }
        LayingRateTarget target = directory.layingRateTarget(sectorId.get());
        LocalDate today = calendar.today();
        DashboardPeriod period = query.period() == null ? DashboardPeriod.TODAY : query.period();
        List<ReportDay> days = directory.reportDays(sectorId.get(), today.minusDays(DAYS_READ_BEFORE_TODAY), today);
        ReportDay todayReport =
                days.stream().filter(day -> day.date().equals(today)).findFirst().orElse(null);
        List<DashboardAlert> alerts =
                DashboardAlerts.of(today, todayReport, directory.cageWatch(sectorId.get(), today), target);
        return Result.success(SectorDashboard.of(sector.get(), period, today, days, target)
                .withAlerts(alerts)
                .withLatestReports(directory.latestReports(sectorId.get(), LATEST_REPORTS)));
    }
}
