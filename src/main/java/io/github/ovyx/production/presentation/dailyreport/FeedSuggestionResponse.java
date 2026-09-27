package io.github.ovyx.production.presentation.dailyreport;

import io.github.ovyx.production.application.dailyreport.FeedSuggestion;
import io.github.ovyx.production.application.dailyreport.SuggestedCageFeed;
import io.github.ovyx.production.application.dailyreport.SuggestedFormula;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** O que o lancamento pela sugestao gravaria (R-006 da 004). */
@Schema(
        name = "FeedSuggestion",
        description = "O que o lançamento pela sugestão gravaria: a proposta de cada gaiola sem ração e os totais do"
                + " dia com ela")
public record FeedSuggestionResponse(
        SuggestedFormulaResponse formula,
        @Schema(description = "A proposta de cada gaiola ainda sem ração") List<SuggestedCageFeedResponse> cages,
        FeedTotalsResponse totals) {

    public static FeedSuggestionResponse from(FeedSuggestion suggestion) {
        return new FeedSuggestionResponse(
                SuggestedFormulaResponse.from(suggestion.formula()),
                suggestion.cages().stream().map(SuggestedCageFeedResponse::from).toList(),
                FeedTotalsResponse.from(suggestion.totals()));
    }

    /** A formula da proposta, com o preco e o esperado atuais. */
    @Schema(name = "SuggestedFormula", description = "A fórmula da proposta, com o preço e o esperado atuais")
    public record SuggestedFormulaResponse(
            @Schema(example = "4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11") UUID id,
            @Schema(example = "Postura Plus") String name,
            @Schema(example = "2.85") BigDecimal pricePerKg,
            @Schema(example = "28") int expectedIntake) {

        static SuggestedFormulaResponse from(SuggestedFormula formula) {
            return new SuggestedFormulaResponse(
                    formula.id(), formula.name(), formula.pricePerKg(), formula.expectedIntake());
        }
    }

    /** A proposta para uma gaiola sem racao. */
    @Schema(name = "SuggestedCageFeed", description = "Proposta para uma gaiola sem ração")
    public record SuggestedCageFeedResponse(
            @Schema(example = "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44") UUID cageId,
            @Schema(example = "B-07") String code,
            @Schema(example = "50") int birdCount,
            @Schema(description = "Aves × consumo esperado, em gramas", example = "1400") int consumption,
            @Schema(description = "Em reais, com duas casas", example = "3.99") BigDecimal cost) {

        static SuggestedCageFeedResponse from(SuggestedCageFeed cage) {
            return new SuggestedCageFeedResponse(
                    cage.cageId(), cage.code(), cage.birdCount(), cage.consumption(), cage.cost());
        }
    }
}
