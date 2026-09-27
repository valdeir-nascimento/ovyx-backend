package io.github.ovyx.production.application.dailyreport;

import java.util.List;

/**
 * O que o lancamento pela sugestao gravaria (R-006 da 004): a proposta de cada gaiola sem racao e os totais
 * do dia com ela.
 */
public record FeedSuggestion(SuggestedFormula formula, List<SuggestedCageFeed> cages, FeedTotals totals) {

    public FeedSuggestion {
        cages = List.copyOf(cages);
    }
}
