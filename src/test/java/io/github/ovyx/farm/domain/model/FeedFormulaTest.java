package io.github.ovyx.farm.domain.model;

import static io.github.ovyx.farm.domain.model.FeedFormulaTestDataBuilder.aFormula;
import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalCodeOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalMessageOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.farm.domain.valueobject.FeedFormulaDescription;
import io.github.ovyx.farm.fixtures.InMemoryFeedFormulaRepository;
import io.github.ovyx.shared.domain.FixedClock;
import io.github.ovyx.shared.domain.Violation;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O agregado {@link FeedFormula}: cadastro, edição, inativação e reativação (US1 da 004; FR-001 a
 * FR-005, FR-020; invariantes 1 e 2 do data-model.md). Sem mock: o quadro das fórmulas é o repositório
 * em memória.
 */
@DisplayName("FeedFormula")
class FeedFormulaTest {

    private static final String NAME_IN_USE = "Já existe uma fórmula com este nome.";
    private static final String NAME_IN_USE_ON_THE_FIELD =
            "Já existe uma fórmula com este nome. Se ela está inativa, reative-a.";

    private final FixedClock clock = FixedClock.at("2026-09-25T13:02:11Z");
    private final InMemoryFeedFormulaRepository roster = new InMemoryFeedFormulaRepository();

    private FeedFormula saved(FeedFormulaTestDataBuilder builder) {
        FeedFormula formula = builder.withRoster(roster).withClock(clock).build();
        roster.save(formula);
        return formula;
    }

    @Test
    @DisplayName("registers an active formula with the price, the expected intake and the description")
    void givenValidFields_whenRegistering_thenCreateAnActiveFormula() {
        // given
        String description = "Milho, farelo de soja, calcário e premix vitamínico; para codornas em postura";

        // when
        FeedFormula formula = FeedFormula.register("Postura Plus", "2,85", "28", description, roster, clock);

        // then
        assertThat(formula.name().value()).isEqualTo("Postura Plus");
        assertThat(formula.pricePerKg().value()).isEqualByComparingTo("2.85");
        assertThat(formula.expectedIntake().value()).isEqualTo(28);
        assertThat(formula.description()).map(FeedFormulaDescription::value).contains(description);
        assertThat(formula.status()).isEqualTo(Status.ACTIVE);
        assertThat(formula.createdAt()).isEqualTo(clock.instant());
        assertThat(formula.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("registers a formula with a blank description as having no description")
    void givenBlankDescription_whenRegistering_thenKeepNoDescription() {
        // given
        String blank = "  ";

        // when
        FeedFormula formula = FeedFormula.register("Recria", "3,10", "24", blank, roster, clock);

        // then
        assertThat(formula.description()).isEmpty();
    }

    @Test
    @DisplayName("refuses every invalid field at once, one violation per field")
    void givenEveryFieldInvalid_whenRegistering_thenRefuseWithAllViolationsAtOnce() {
        // given
        String longDescription = "d".repeat(501);

        // when
        List<Violation> violations =
                violationsOf(() -> FeedFormula.register(" ", "0", "300", longDescription, roster, clock));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(
                        tuple("name", FarmErrorCode.FEED_FORMULA_NAME_REQUIRED),
                        tuple("pricePerKg", FarmErrorCode.PRICE_OUT_OF_RANGE),
                        tuple("expectedIntake", FarmErrorCode.EXPECTED_INTAKE_OUT_OF_RANGE),
                        tuple("description", FarmErrorCode.FEED_FORMULA_DESCRIPTION_TOO_LONG));
        assertThat(refusalCodeOf(() -> FeedFormula.register(" ", "0", "300", longDescription, roster, clock)))
                .isEqualTo(FarmErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("refuses the name of another formula, differing only in case and spaces")
    void givenFormulaWithTheSameName_whenRegisteringWithOtherCaseAndSpaces_thenRefuseAsNameInUse() {
        // given
        saved(aFormula().named("Postura Plus"));

        // when
        Map<String, String> details =
                detailsOf(() -> FeedFormula.register(" postura PLUS ", "2,90", "28", null, roster, clock));

        // then
        assertThat(details).containsExactly(Map.entry("name", NAME_IN_USE_ON_THE_FIELD));
        assertThat(refusalCodeOf(() -> FeedFormula.register(" postura PLUS ", "2,90", "28", null, roster, clock)))
                .isEqualTo(FarmErrorCode.FEED_FORMULA_NAME_IN_USE);
        assertThat(refusalMessageOf(
                        () -> FeedFormula.register(" postura PLUS ", "2,90", "28", null, roster, clock)))
                .isEqualTo(NAME_IN_USE);
    }

    @Test
    @DisplayName("refuses the name of an inactive formula too")
    void givenInactiveFormulaWithTheSameName_whenRegistering_thenRefuseAsNameInUse() {
        // given
        // Diferente do setor: a fórmula dá nome aos custos do passado (R-011 da 004).
        saved(aFormula().named("Recria").inactive());

        // when
        FarmErrorCode code =
                (FarmErrorCode) refusalCodeOf(() -> FeedFormula.register("Recria", "3,10", "24", null, roster, clock));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.FEED_FORMULA_NAME_IN_USE);
    }

    @Test
    @DisplayName("refuses the fields before looking for the name in use")
    void givenNameInUseAndInvalidPrice_whenRegistering_thenRefuseForTheFieldsFirst() {
        // given
        saved(aFormula().named("Postura Plus"));

        // when
        FarmErrorCode code = (FarmErrorCode)
                refusalCodeOf(() -> FeedFormula.register("Postura Plus", "2,855", "28", null, roster, clock));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("updates every field and the instant of the last change")
    void givenValidFields_whenUpdating_thenChangeThemAll() {
        // given
        FeedFormula formula = saved(aFormula());
        clock.advance(Duration.ofHours(2));

        // when
        formula.update("Postura Plus 2", "2,90", "30", "Nova composição", roster, clock);

        // then
        assertThat(formula.name().value()).isEqualTo("Postura Plus 2");
        assertThat(formula.pricePerKg().value()).isEqualByComparingTo("2.90");
        assertThat(formula.expectedIntake().value()).isEqualTo(30);
        assertThat(formula.description()).map(FeedFormulaDescription::value).contains("Nova composição");
        assertThat(formula.updatedAt()).isEqualTo(clock.instant());
        assertThat(formula.createdAt()).isBefore(formula.updatedAt());
    }

    @Test
    @DisplayName("keeps its own name without conflicting with itself")
    void givenOwnNameInOtherCase_whenUpdating_thenAcceptIt() {
        // given
        FeedFormula formula = saved(aFormula().named("Postura Plus"));

        // when
        formula.update("POSTURA PLUS", "2,85", "28", null, roster, clock);

        // then
        assertThat(formula.name().value()).isEqualTo("POSTURA PLUS");
        assertThat(formula.description()).isEmpty();
    }

    @Test
    @DisplayName("updates an inactive formula, which stays inactive")
    void givenInactiveFormula_whenUpdating_thenChangeItAndKeepItInactive() {
        // given
        FeedFormula formula = saved(aFormula().named("Recria").inactive());

        // when
        formula.update("Recria", "3,20", "24", null, roster, clock);

        // then
        assertThat(formula.pricePerKg().value()).isEqualByComparingTo("3.20");
        assertThat(formula.isActive()).isFalse();
    }

    @Test
    @DisplayName("refuses the name of another formula and keeps its data")
    void givenNameOfAnotherFormula_whenUpdating_thenRefuseAsNameInUseAndKeepTheData() {
        // given
        saved(aFormula().named("Recria").inactive());
        FeedFormula formula = saved(aFormula().named("Postura Plus"));

        // when
        FarmErrorCode code =
                (FarmErrorCode) refusalCodeOf(() -> formula.update("recria", "3,10", "24", null, roster, clock));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.FEED_FORMULA_NAME_IN_USE);
        assertThat(formula.name().value()).isEqualTo("Postura Plus");
        assertThat(formula.pricePerKg().value()).isEqualByComparingTo("2.85");
    }

    @Test
    @DisplayName("refuses every invalid field at once on update and keeps its data")
    void givenInvalidFields_whenUpdating_thenRefuseWithAllViolationsAndKeepTheData() {
        // given
        FeedFormula formula = saved(aFormula());

        // when
        Map<String, String> details = detailsOf(() -> formula.update("P", "abc", "28,5", null, roster, clock));

        // then
        assertThat(details)
                .containsExactly(
                        Map.entry("name", "O nome da fórmula deve ter ao menos 2 caracteres."),
                        Map.entry("pricePerKg", "Informe o preço em reais, com até duas casas decimais."),
                        Map.entry("expectedIntake", "O consumo esperado deve ser um número inteiro de gramas."));
        assertThat(formula.name().value()).isEqualTo("Postura Plus");
    }

    @Test
    @DisplayName("deactivates the formula and changes the instant of the last change")
    void givenActiveFormula_whenDeactivating_thenMakeItInactive() {
        // given
        FeedFormula formula = saved(aFormula());
        clock.advance(Duration.ofHours(1));

        // when
        boolean changed = formula.deactivate(clock);

        // then
        assertThat(changed).isTrue();
        assertThat(formula.status()).isEqualTo(Status.INACTIVE);
        assertThat(formula.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("changes nothing when deactivating a formula that is already inactive")
    void givenInactiveFormula_whenDeactivatingAgain_thenChangeNothing() {
        // given
        FeedFormula formula = saved(aFormula().inactive());
        Instant before = formula.updatedAt();
        clock.advance(Duration.ofHours(1));

        // when
        boolean changed = formula.deactivate(clock);

        // then
        assertThat(changed).isFalse();
        assertThat(formula.updatedAt()).isEqualTo(before);
    }

    @Test
    @DisplayName("reactivates the formula and changes the instant of the last change")
    void givenInactiveFormula_whenReactivating_thenMakeItActive() {
        // given
        FeedFormula formula = saved(aFormula().inactive());
        clock.advance(Duration.ofHours(1));

        // when
        boolean changed = formula.reactivate(clock);

        // then
        assertThat(changed).isTrue();
        assertThat(formula.isActive()).isTrue();
        assertThat(formula.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("changes nothing when reactivating a formula that is already active")
    void givenActiveFormula_whenReactivating_thenChangeNothing() {
        // given
        FeedFormula formula = saved(aFormula());
        Instant before = formula.updatedAt();
        clock.advance(Duration.ofHours(1));

        // when
        boolean changed = formula.reactivate(clock);

        // then
        assertThat(changed).isFalse();
        assertThat(formula.updatedAt()).isEqualTo(before);
    }
}
