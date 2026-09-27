package io.github.ovyx.farm.application.formula;

import static io.github.ovyx.farm.domain.model.FeedFormulaTestDataBuilder.aFormula;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.FeedFormula;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.farm.fixtures.InMemoryFeedFormulaRepository;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Cadastro de fórmula pelo administrador (US1 da 004, cenários 1 e 2; FR-001, FR-002, FR-020). */
@DisplayName("RegisterFeedFormulaCommandHandler")
class RegisterFeedFormulaCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-25T13:02:11Z");
    private final InMemoryFeedFormulaRepository repository = new InMemoryFeedFormulaRepository();
    private final RegisterFeedFormulaCommandHandler handler = new RegisterFeedFormulaCommandHandler(repository, clock);

    @Test
    @DisplayName("registers the formula and saves it")
    void givenValidFields_whenRegistering_thenSaveTheFormula() {
        // given
        RegisterFeedFormulaCommand command =
                new RegisterFeedFormulaCommand("Postura Plus", "2,85", "28", "Para codornas em postura");

        // when
        Result<FeedFormulaId> result = handler.handle(command);

        // then
        FeedFormula saved = repository.findById(result.value()).orElseThrow();
        assertThat(saved.name().value()).isEqualTo("Postura Plus");
        assertThat(saved.pricePerKg().value()).isEqualByComparingTo("2.85");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("fails as validation with every invalid field, and saves nothing")
    void givenInvalidFields_whenRegistering_thenFailAsValidationWithEveryField() {
        // given
        RegisterFeedFormulaCommand command = new RegisterFeedFormulaCommand(" ", "0", "300", null);

        // when
        Result<FeedFormulaId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.error().details()).containsOnlyKeys("name", "pricePerKg", "expectedIntake");
        assertThat(repository.saves()).isZero();
    }

    @Test
    @DisplayName("fails as conflict when another formula has the name, and saves nothing")
    void givenNameOfAnotherFormula_whenRegistering_thenFailAsConflict() {
        // given
        repository.save(aFormula().named("Postura Plus").inactive().build());
        RegisterFeedFormulaCommand command = new RegisterFeedFormulaCommand("POSTURA PLUS", "2,85", "28", null);

        // when
        Result<FeedFormulaId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("FEED_FORMULA_NAME_IN_USE");
        assertThat(result.error().message()).isEqualTo("Já existe uma fórmula com este nome.");
        assertThat(result.error().details())
                .containsExactly(
                        Map.entry("name", "Já existe uma fórmula com este nome. Se ela está inativa, reative-a."));
        assertThat(repository.saves()).isEqualTo(1);
    }
}
