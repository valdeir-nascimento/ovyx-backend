package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.application.common.ProductionRefusals;
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
 * Lanca a racao do setor pela sugestao (FR-009, FR-022 da 004).
 *
 * <p>O tratador traz o setor da estrutura da granja, o relatorio do repositorio e a formula do catalogo; e o
 * agregado que decide: o setor ativo, a formula escolhida, existente e ativa, e as gaiolas ainda sem racao.
 * Sem gaiola pendente nao ha o que gravar, e o comando termina bem sem gravar.
 */
public class RecordFeedBySuggestionCommandHandler
        implements CommandHandler<RecordFeedBySuggestionCommand, DailyReportId> {

    private final DailyReportRepository repository;
    private final FarmStructure farmStructure;
    private final FeedCatalog feedCatalog;
    private final Clock clock;

    public RecordFeedBySuggestionCommandHandler(
            DailyReportRepository repository, FarmStructure farmStructure, FeedCatalog feedCatalog, Clock clock) {
        this.repository = repository;
        this.farmStructure = farmStructure;
        this.feedCatalog = feedCatalog;
        this.clock = clock;
    }

    @Override
    public Result<DailyReportId> handle(RecordFeedBySuggestionCommand command) {
        Optional<FarmSector> sector = SectorId.parse(command.sectorId()).flatMap(farmStructure::sectorOf);
        if (sector.isEmpty()) {
            return Result.failure(ProductionRefusals.sectorNotFound());
        }
        Optional<DailyReport> report =
                DailyReportId.parse(command.reportId()).flatMap(id -> repository.findById(sector.get().id(), id));
        if (report.isEmpty()) {
            return Result.failure(ProductionRefusals.dailyReportNotFound());
        }
        int recorded;
        try {
            recorded = report.get()
                    .recordFeedBySuggestion(
                            sector.get(),
                            FeedFormulaChoice.lookup(command.formulaId(), feedCatalog),
                            command.actor(),
                            clock.instant());
        } catch (DomainException refusal) {
            return Result.failure(ProductionRefusals.from(refusal));
        }
        // Com todas as gaiolas ja lancadas, nada mudou: gravar so subiria a versao.
        if (recorded > 0) {
            repository.save(report.get());
        }
        return Result.success(report.get().id());
    }
}
