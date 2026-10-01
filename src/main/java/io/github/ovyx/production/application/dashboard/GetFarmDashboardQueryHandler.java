package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O painel da granja toda (feature 009). Le os setores ativos e os relatorios deles de hoje - 13 ate hoje, o bastante
 * para os 7 dias e os 7 anteriores, e deixa as contas ao {@link FarmDashboard}. Sem setor ativo, o painel sai com os
 * numeros ausentes, e nao falha: a regra de quando a aba aparece e da tela (R-008).
 *
 * <p>Os alertas de cada setor saem do mesmo calculo do painel do setor ({@link DashboardAlerts}), com as mesmas
 * leituras e a meta do setor: o numero da comparacao e sempre o da aba dele (FR-012, R-004).
 */
public class GetFarmDashboardQueryHandler implements QueryHandler<GetFarmDashboardQuery, FarmDashboard> {

    /** Os dias lidos antes de hoje: os 7 dias e os 7 anteriores a eles. */
    private static final int DAYS_READ_BEFORE_TODAY = 13;

    private final DashboardDirectory directory;
    private final FarmCalendar calendar;

    public GetFarmDashboardQueryHandler(DashboardDirectory directory, FarmCalendar calendar) {
        this.directory = directory;
        this.calendar = calendar;
    }

    @Override
    public Result<FarmDashboard> handle(GetFarmDashboardQuery query) {
        LocalDate today = calendar.today();
        DashboardPeriod period = query.period() == null ? DashboardPeriod.TODAY : query.period();
        List<ActiveSector> sectors = directory.activeSectors();
        Map<UUID, List<ReportDay>> days = directory.activeReportDays(today.minusDays(DAYS_READ_BEFORE_TODAY), today);
        Map<UUID, Integer> openAlerts = new HashMap<>();
        for (ActiveSector sector : sectors) {
            ReportDay todayReport = DashboardFigures.within(days.getOrDefault(sector.id(), List.of()), today, today)
                    .stream()
                    .findFirst()
                    .orElse(null);
            CageWatchReading reading = directory.cageWatch(SectorId.of(sector.id()), today);
            openAlerts.put(sector.id(), DashboardAlerts.of(today, todayReport, reading, sector.target()).size());
        }
        return Result.success(FarmDashboard.of(period, today, sectors, days, openAlerts));
    }
}
