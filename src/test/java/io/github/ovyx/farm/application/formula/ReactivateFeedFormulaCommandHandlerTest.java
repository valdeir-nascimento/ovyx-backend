package io.github.ovyx.farm.application.formula;

import static io.github.ovyx.farm.domain.model.FeedFormulaTestDataBuilder.aFormula;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.FeedFormula;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.farm.fixtures.InMemoryFeedFormulaRepository;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Reativação de fórmula (US1 da 004, cenário 4; FR-004). */
@DisplayName("ReactivateFeedFormulaCommandHandler")
class ReactivateFeedFormulaCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-25T14:03:51Z");
    private final InMemoryFeedFormulaRepository repository = new InMemoryFeedFormulaRepository();
    private final ReactivateFeedFormulaCommandHandler handler =
            new ReactivateFeedFormulaCommandHandler(repository, clock);

    @Test
    @DisplayName("reactivates the formula and saves it")
    void givenInactiveFormula_whenReactivating_thenSaveItActive() {
        // given
        FeedFormula formula = aFormula().inactive().withClock(clock).build();
        repository.save(formula);

        // when
        Result<FeedFormulaId> result = handler.handle(new ReactivateFeedFormulaCommand(formula.id().toString()));

        // then
        assertThat(repository.findById(result.value()).orElseThrow().isActive()).isTrue();
        assertThat(repository.saves()).isEqualTo(2);
    }

    @Test
    @DisplayName("saves nothing when the formula is already active")
    void givenActiveFormula_whenReactivating_thenSaveNothing() {
        // given
        FeedFormula formula = aFormula().withClock(clock).build();
        repository.save(formula);

        // when
        Result<FeedFormulaId> result = handler.handle(new ReactivateFeedFormulaCommand(formula.id().toString()));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(repository.saves()).isEqualTo(1);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c99", "postura-plus"})
    @DisplayName("fails as not found for an unknown or malformed identifier")
    void givenUnknownOrMalformedIdentifier_whenReactivating_thenFailAsNotFound(String formulaId) {
        // given
        ReactivateFeedFormulaCommand command = new ReactivateFeedFormulaCommand(formulaId);

        // when
        Result<FeedFormulaId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("FEED_FORMULA_NOT_FOUND");
    }
}
