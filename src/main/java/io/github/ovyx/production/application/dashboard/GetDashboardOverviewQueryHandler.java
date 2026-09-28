package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import java.time.LocalDate;

/** O cabecalho do painel: o dia e a parte do dia no relogio da granja, e os setores (FR-004, FR-023). */
public class GetDashboardOverviewQueryHandler implements QueryHandler<GetDashboardOverviewQuery, DashboardOverview> {

    private final DashboardDirectory directory;
    private final FarmCalendar calendar;

    public GetDashboardOverviewQueryHandler(DashboardDirectory directory, FarmCalendar calendar) {
        this.directory = directory;
        this.calendar = calendar;
    }

    @Override
    public Result<DashboardOverview> handle(GetDashboardOverviewQuery query) {
        LocalDate today = calendar.today();
        DashboardSectors sectors = directory.overview(today);
        return Result.success(new DashboardOverview(
                today,
                PartOfDay.of(calendar.now()),
                sectors.activeSectors(),
                sectors.completeToday(),
                sectors.sectors()));
    }
}
