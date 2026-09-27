package io.github.ovyx.farm.application.formula;

import static io.github.ovyx.farm.domain.model.FeedFormulaTestDataBuilder.aFormula;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.FeedFormula;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.farm.fixtures.InMemoryFeedFormulaRepository;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Edição de fórmula pelo administrador (US1 da 004, cenário 3; FR-003). */
@DisplayName("UpdateFeedFormulaCommandHandler")
class UpdateFeedFormulaCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-25T13:02:11Z");
    private final InMemoryFeedFormulaRepository repository = new InMemoryFeedFormulaRepository();
    private final UpdateFeedFormulaCommandHandler handler = new UpdateFeedFormulaCommandHandler(repository, clock);

    private FeedFormula saved(String name) {
        FeedFormula formula = aFormula().named(name).withRoster(repository).withClock(clock).build();
        repository.save(formula);
        return formula;
    }

    @Test
    @DisplayName("updates the fields and saves them")
    void givenValidFields_whenUpdating_thenSaveTheNewData() {
        // given
        FeedFormula formula = saved("Postura Plus");
        clock.advance(Duration.ofHours(1));
        UpdateFeedFormulaCommand command =
                new UpdateFeedFormulaCommand(formula.id().toString(), "Postura Plus", "2,90", "28", null);

        // when
        Result<FeedFormulaId> result = handler.handle(command);

        // then
        FeedFormula updated = repository.findById(result.value()).orElseThrow();
        assertThat(updated.pricePerKg().value()).isEqualByComparingTo("2.90");
        assertThat(updated.updatedAt()).isEqualTo(clock.instant());
        assertThat(repository.saves()).isEqualTo(2);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c99", "postura-plus"})
    @DisplayName("fails as not found for an unknown or malformed identifier")
    void givenUnknownOrMalformedIdentifier_whenUpdating_thenFailAsNotFound(String formulaId) {
        // given
        UpdateFeedFormulaCommand command = new UpdateFeedFormulaCommand(formulaId, "Postura Plus", "2,85", "28", null);

        // when
        Result<FeedFormulaId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("FEED_FORMULA_NOT_FOUND");
        assertThat(result.error().message()).isEqualTo("Fórmula não encontrada.");
    }

    @Test
    @DisplayName("fails as validation with every invalid field, and saves nothing")
    void givenInvalidFields_whenUpdating_thenFailAsValidationAndSaveNothing() {
        // given
        FeedFormula formula = saved("Postura Plus");
        UpdateFeedFormulaCommand command =
                new UpdateFeedFormulaCommand(formula.id().toString(), "P", "2,855", "", "d".repeat(501));

        // when
        Result<FeedFormulaId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details()).containsOnlyKeys("name", "pricePerKg", "expectedIntake", "description");
        assertThat(repository.saves()).isEqualTo(1);
    }

    @Test
    @DisplayName("fails as conflict with the name of another formula")
    void givenNameOfAnotherFormula_whenUpdating_thenFailAsConflict() {
        // given
        saved("Recria");
        FeedFormula formula = saved("Postura Plus");
        UpdateFeedFormulaCommand command =
                new UpdateFeedFormulaCommand(formula.id().toString(), "RECRIA", "2,85", "28", null);

        // when
        Result<FeedFormulaId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("FEED_FORMULA_NAME_IN_USE");
        assertThat(repository.saves()).isEqualTo(2);
    }
}
