package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.application.common.ProductionRefusals;
import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.production.domain.model.DailyReport;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.FarmSector;
import io.github.ovyx.production.domain.model.FeedFormulaChoice;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.production.domain.port.DailyReportRepository;
import io.github.ovyx.production.domain.port.FarmStructure;
import io.github.ovyx.production.domain.port.FeedCatalog;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import java.time.Clock;
import java.util.Optional;

/**
 * Lanca ou corrige a racao de uma gaiola do relatorio (FR-010, FR-011 da 004).
 *
 * <p>O tratador traz o setor da estrutura da granja, o relatorio do repositorio e a formula do catalogo; e o
 * agregado que decide: a gaiola do relatorio, o setor ativo, a formula e o consumo, todas as falhas de uma
 * vez.
 */
public class RecordFeedCommandHandler implements CommandHandler<RecordFeedCommand, CageId> {

    private final DailyReportRepository repository;
    private final FarmStructure farmStructure;
    private final FeedCatalog feedCatalog;
    private final Clock clock;

    public RecordFeedCommandHandler(
            DailyReportRepository repository, FarmStructure farmStructure, FeedCatalog feedCatalog, Clock clock) {
        this.repository = repository;
        this.farmStructure = farmStructure;
        this.feedCatalog = feedCatalog;
        this.clock = clock;
    }

    @Override
    public Result<CageId> handle(RecordFeedCommand command) {
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
                    .recordFeed(
                            sector.get(),
                            cageId.get(),
                            FeedFormulaChoice.lookup(command.formulaId(), feedCatalog),
                            command.consumption(),
                            command.actor(),
                            clock.instant());
        } catch (DomainException refusal) {
            return Result.failure(ProductionRefusals.from(refusal));
        }
        repository.save(report.get());
        return Result.success(cageId.get());
    }
}
