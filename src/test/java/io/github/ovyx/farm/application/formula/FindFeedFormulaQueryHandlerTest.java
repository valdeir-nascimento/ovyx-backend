package io.github.ovyx.farm.application.formula;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Consulta de uma fórmula, ativa ou inativa (US1 da 004; FR-005). */
@DisplayName("FindFeedFormulaQueryHandler")
class FindFeedFormulaQueryHandlerTest {

    private final RecordingFeedFormulaDirectory directory = new RecordingFeedFormulaDirectory();
    private final FindFeedFormulaQueryHandler handler = new FindFeedFormulaQueryHandler(directory);

    @Test
    @DisplayName("finds the formula, inactive included")
    void givenExistingInactiveFormula_whenFinding_thenReturnIt() {
        // given
        FeedFormulaSummary recria = new FeedFormulaSummary(
                FeedFormulaId.generate(),
                "Recria",
                "Para codornas em crescimento",
                new BigDecimal("3.10"),
                24,
                new BigDecimal("0.074"),
                Status.INACTIVE,
                Instant.parse("2026-09-20T10:15:00Z"),
                Instant.parse("2026-09-24T17:40:12Z"));
        directory.holding(recria);

        // when
        Result<FeedFormulaSummary> result = handler.handle(new FindFeedFormulaQuery(recria.id().toString()));

        // then
        assertThat(result.value()).isEqualTo(recria);
    }

    @Test
    @DisplayName("fails as not found for an identifier of no formula")
    void givenUnknownIdentifier_whenFinding_thenFailAsNotFound() {
        // given
        FindFeedFormulaQuery query = new FindFeedFormulaQuery("4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c99");

        // when
        Result<FeedFormulaSummary> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("FEED_FORMULA_NOT_FOUND");
        assertThat(result.error().message()).isEqualTo("Fórmula não encontrada.");
    }

    @Test
    @DisplayName("fails as not found for a malformed identifier, without asking the directory")
    void givenMalformedIdentifier_whenFinding_thenFailAsNotFoundWithoutAsking() {
        // given
        FindFeedFormulaQuery query = new FindFeedFormulaQuery("postura-plus");

        // when
        Result<FeedFormulaSummary> result = handler.handle(query);

        // then
        assertThat(result.error().code()).isEqualTo("FEED_FORMULA_NOT_FOUND");
        assertThat(directory.askedIds()).isEmpty();
    }
}
