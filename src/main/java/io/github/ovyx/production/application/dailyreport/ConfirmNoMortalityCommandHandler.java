package io.github.ovyx.production.application.dailyreport;

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
 * Confirma o dia sem ocorrencia de mortalidade (FR-013). Confirmar de novo nao muda nada e nao grava,
 * como a inativacao repetida da 002.
 */
public class ConfirmNoMortalityCommandHandler implements CommandHandler<ConfirmNoMortalityCommand, DailyReportId> {

    private final DailyReportRepository repository;
    private final FarmStructure farmStructure;
    private final Clock clock;

    public ConfirmNoMortalityCommandHandler(
            DailyReportRepository repository, FarmStructure farmStructure, Clock clock) {
        this.repository = repository;
        this.farmStructure = farmStructure;
        this.clock = clock;
    }

    @Override
    public Result<DailyReportId> handle(ConfirmNoMortalityCommand command) {
        Optional<FarmSector> sector = SectorId.parse(command.sectorId()).flatMap(farmStructure::sectorOf);
        if (sector.isEmpty()) {
            return Result.failure(ProductionRefusals.sectorNotFound());
        }
        Optional<DailyReport> report =
                DailyReportId.parse(command.reportId()).flatMap(id -> repository.findById(sector.get().id(), id));
        if (report.isEmpty()) {
            return Result.failure(ProductionRefusals.dailyReportNotFound());
        }
        boolean changed;
        try {
            changed = report.get().confirmNoMortality(sector.get(), command.actor(), clock.instant());
        } catch (DomainException refusal) {
            return Result.failure(ProductionRefusals.from(refusal));
        }
        if (changed) {
            repository.save(report.get());
        }
        return Result.success(report.get().id());
    }
}
