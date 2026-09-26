package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.application.common.FarmCalendar;
import io.github.ovyx.production.application.common.ProductionRefusals;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.production.domain.valueobject.FlockAge;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Sugere o relatorio novo (FR-004): a data e a hora de agora na granja; as aves pelo saldo do relatorio
 * mais recente do setor ou, no primeiro, pela soma das aves das gaiolas ativas; e a idade pela do mais
 * recente, somadas as semanas completas passadas desde a data dele.
 */
public class SuggestDailyReportQueryHandler implements QueryHandler<SuggestDailyReportQuery, DailyReportSuggestion> {

    private final DailyReportDirectory directory;
    private final FarmCalendar calendar;

    public SuggestDailyReportQueryHandler(DailyReportDirectory directory, FarmCalendar calendar) {
        this.directory = directory;
        this.calendar = calendar;
    }

    @Override
    public Result<DailyReportSuggestion> handle(SuggestDailyReportQuery query) {
        Optional<SectorId> sectorId = SectorId.parse(query.sectorId()).filter(id -> directory.sectorOf(id).isPresent());
        if (sectorId.isEmpty()) {
            return Result.failure(ProductionRefusals.sectorNotFound());
        }
        LocalDate today = calendar.today();
        Optional<LatestDailyReport> latest = directory.latestOf(sectorId.get());
        // O saldo zerado nao abre relatorio (as aves vao de 1 a 1.000.000): as gaiolas ativas dizem as
        // aves que o setor tem agora. A idade nao passa do limite do formulario.
        int birds = latest.map(LatestDailyReport::closingBirdCount)
                .filter(closing -> closing > 0)
                .orElseGet(() -> directory.activeBirdsOf(sectorId.get()));
        Integer age = latest.map(report -> Math.min(
                        FlockAge.MAXIMUM,
                        report.flockAge() + (int) Math.max(0, ChronoUnit.WEEKS.between(report.collectionDate(), today))))
                .orElse(null);
        return Result.success(new DailyReportSuggestion(today, calendar.now(), birds, age));
    }
}
