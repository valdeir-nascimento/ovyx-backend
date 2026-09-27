package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.application.common.ProductionRefusals;
import io.github.ovyx.production.domain.model.CatalogFormula;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.FeedFormulaChoice;
import io.github.ovyx.production.domain.model.FeedProposal;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.production.domain.port.FeedCatalog;
import io.github.ovyx.production.domain.valueobject.FeedEntry;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A proposta da racao do setor, antes de gravar (FR-009 e R-006 da 004): o consumo de cada gaiola sem
 * racao e os totais do dia se a proposta fosse gravada.
 *
 * <p>E leitura: monta a proposta sobre o detalhe do relatorio, sem carregar o agregado (principio V), com as
 * mesmas regras de dominio do lancamento — a formula escolhida, existente e ativa, e a proposta de cada
 * gaiola dentro do consumo maximo. Num setor inativo a proposta sai, e e o lancamento que recusa.
 */
public class SuggestFeedQueryHandler implements QueryHandler<SuggestFeedQuery, FeedSuggestion> {

    private final DailyReportDirectory directory;
    private final FeedCatalog feedCatalog;

    public SuggestFeedQueryHandler(DailyReportDirectory directory, FeedCatalog feedCatalog) {
        this.directory = directory;
        this.feedCatalog = feedCatalog;
    }

    @Override
    public Result<FeedSuggestion> handle(SuggestFeedQuery query) {
        Optional<SectorId> sectorId = SectorId.parse(query.sectorId());
        if (sectorId.isEmpty() || directory.sectorOf(sectorId.get()).isEmpty()) {
            return Result.failure(ProductionRefusals.sectorNotFound());
        }
        Optional<DailyReportDetail> detail =
                DailyReportId.parse(query.reportId()).flatMap(reportId -> directory.findDetail(sectorId.get(), reportId));
        if (detail.isEmpty()) {
            return Result.failure(ProductionRefusals.dailyReportNotFound());
        }
        try {
            return Result.success(suggestionOf(detail.get(), FeedFormulaChoice.lookup(query.formulaId(), feedCatalog)));
        } catch (DomainException refusal) {
            return Result.failure(ProductionRefusals.from(refusal));
        }
    }

    /**
     * A proposta sobre o relatorio: as gaiolas lancadas ficam como estao, e as pendentes recebem a proposta
     * da formula, para os totais do dia sairem como sairiam depois de gravar.
     */
    private static FeedSuggestion suggestionOf(DailyReportDetail detail, FeedFormulaChoice choice) {
        FeedProposal<ReportCageDetail> proposal = FeedProposal.of(
                choice,
                detail.cages().stream().filter(cage -> cage.feed() == null).toList(),
                ReportCageDetail::code,
                ReportCageDetail::birdCount);
        CatalogFormula formula = proposal.formula();

        List<SuggestedCageFeed> proposals = new ArrayList<>();
        List<ReportCageDetail> withTheProposal = new ArrayList<>();
        for (ReportCageDetail cage : detail.cages()) {
            FeedEntry entry = proposal.entries().get(cage);
            if (entry == null) {
                withTheProposal.add(cage);
                continue;
            }
            CageFeed feed = CageFeed.of(
                    formula.id().value(),
                    formula.name(),
                    formula.pricePerKg(),
                    formula.expectedIntake(),
                    entry.consumption(),
                    cage.birdCount());
            proposals.add(new SuggestedCageFeed(
                    cage.cageId(), cage.code(), cage.birdCount(), entry.consumption(), feed.cost()));
            withTheProposal.add(new ReportCageDetail(
                    cage.cageId(),
                    cage.code(),
                    cage.battery(),
                    cage.number(),
                    cage.birdCount(),
                    cage.production(),
                    cage.mortality(),
                    feed));
        }

        return new FeedSuggestion(
                new SuggestedFormula(formula.id().value(), formula.name(), formula.pricePerKg(), formula.expectedIntake()),
                proposals,
                DailyReportTotals.feed(withTheProposal, detail.production().collectedEggs()));
    }
}
