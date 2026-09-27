package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.application.common.FarmCalendar;
import io.github.ovyx.production.application.common.ProductionRefusals;
import io.github.ovyx.production.domain.model.DailyReport;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.FarmSector;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.production.domain.port.DailyReportRepository;
import io.github.ovyx.production.domain.port.FarmStructure;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import java.time.Clock;
import java.util.Optional;

/**
 * Abre o relatorio do dia de um setor (FR-001 a FR-005).
 *
 * <p>O tratador traz o setor da estrutura da granja e "hoje" do calendario da granja; e o agregado que
 * decide.
 */
public class OpenDailyReportCommandHandler implements CommandHandler<OpenDailyReportCommand, DailyReportId> {

    private final DailyReportRepository repository;
    private final FarmStructure farmStructure;
    private final FarmCalendar calendar;
    private final Clock clock;

    public OpenDailyReportCommandHandler(
            DailyReportRepository repository, FarmStructure farmStructure, FarmCalendar calendar, Clock clock) {
        this.repository = repository;
        this.farmStructure = farmStructure;
        this.calendar = calendar;
        this.clock = clock;
    }

    @Override
    public Result<DailyReportId> handle(OpenDailyReportCommand command) {
        Optional<FarmSector> sector = SectorId.parse(command.sectorId()).flatMap(farmStructure::sectorOf);
        if (sector.isEmpty()) {
            return Result.failure(ProductionRefusals.sectorNotFound());
        }
        DailyReport report;
        try {
            report = DailyReport.open(
                    sector.get(),
                    command.collectionDate(),
                    command.collectionTime(),
                    command.openingBirdCount(),
                    command.flockAge(),
                    command.note(),
                    command.actor(),
                    calendar.today(),
                    repository,
                    clock.instant());
        } catch (DomainException refusal) {
            return Result.failure(ProductionRefusals.from(refusal));
        }
        repository.save(report);
        return Result.success(report.id());
    }
}
