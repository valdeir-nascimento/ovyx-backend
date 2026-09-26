package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.application.common.ProductionRefusals;
import io.github.ovyx.production.domain.model.CageId;
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
 * Lanca ou corrige a mortalidade de uma gaiola do relatorio (FR-011 a FR-015).
 *
 * <p>O tratador traz o setor da estrutura da granja e o relatorio do repositorio; e o agregado que
 * decide: a gaiola do relatorio, o setor ativo, os campos e os limites pelas aves.
 */
public class RecordMortalityCommandHandler implements CommandHandler<RecordMortalityCommand, CageId> {

    private final DailyReportRepository repository;
    private final FarmStructure farmStructure;
    private final Clock clock;

    public RecordMortalityCommandHandler(DailyReportRepository repository, FarmStructure farmStructure, Clock clock) {
        this.repository = repository;
        this.farmStructure = farmStructure;
        this.clock = clock;
    }

    @Override
    public Result<CageId> handle(RecordMortalityCommand command) {
        Optional<FarmSector> sector = SectorId.parse(command.sectorId()).flatMap(farmStructure::sectorOf);
        if (sector.isEmpty()) {
            return Result.failure(ProductionRefusals.sectorNotFound());
        }
        Optional<DailyReport> report =
                DailyReportId.parse(command.reportId()).flatMap(id -> repository.findById(sector.get().id(), id));
        if (report.isEmpty()) {
            return Result.failure(ProductionRefusals.dailyReportNotFound());
        }
        Optional<CageId> cageId = CageId.parse(command.cageId());
        if (cageId.isEmpty()) {
            return Result.failure(ProductionRefusals.cageNotFound());
        }
        try {
            report.get()
                    .recordMortality(
                            sector.get(),
                            cageId.get(),
                            command.deaths(),
                            command.culls(),
                            command.note(),
                            command.actor(),
                            clock.instant());
        } catch (DomainException refusal) {
            return Result.failure(ProductionRefusals.from(refusal));
        }
        repository.save(report.get());
        return Result.success(cageId.get());
    }
}
