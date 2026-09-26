package io.github.ovyx.production.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalCodeOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.shared.domain.Violation;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A produção de uma gaiola (US2; FR-007, FR-008): os ovos coletados, obrigatórios, de 0 a 1.000, e a
 * classificação dos que saíram fora do padrão, cada uma de 0 a 1.000, em branco valendo zero, sem somar
 * mais que os ovos. Os valores chegam como texto cru, e todas as violações vêm de uma vez (R-010).
 */
@DisplayName("ProductionEntry")
class ProductionEntryTest {

    private static final EggGrades.Raw NO_GRADES = new EggGrades.Raw(null, null, null, null, null, null);

    /** Cada classificação, com o campo, o nome na mensagem e como montar a classificação só com ela. */
    static Stream<Arguments> grades() {
        return Stream.of(
                grade("small", "pequenos", value -> new EggGrades.Raw(value, null, null, null, null, null)),
                grade("jumbo", "jumbo", value -> new EggGrades.Raw(null, value, null, null, null, null)),
                grade("dirty", "sujos", value -> new EggGrades.Raw(null, null, value, null, null, null)),
                grade("cracked", "trincados", value -> new EggGrades.Raw(null, null, null, value, null, null)),
                grade("bloodSpot", "ovos com sangue", value -> new EggGrades.Raw(null, null, null, null, value, null)),
                grade("abnormal", "anormais", value -> new EggGrades.Raw(null, null, null, null, null, value)));
    }

    private static Arguments grade(String field, String name, Function<String, EggGrades.Raw> only) {
        return Arguments.of(field, name, only);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("rejects missing eggs")
    void givenMissingEggs_whenCreating_thenRejectAsRequired(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> ProductionEntry.of(raw, NO_GRADES));

        // then
        assertThat(details).containsExactly(Map.entry("eggs", "Informe os ovos coletados."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"45.5", "45,5", "quarenta"})
    @DisplayName("rejects eggs that are not an integer")
    void givenEggsThatAreNotAnInteger_whenCreating_thenRejectAsNotInteger(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> ProductionEntry.of(raw, NO_GRADES));

        // then
        assertThat(details).containsExactly(Map.entry("eggs", "Os ovos coletados devem ser um número inteiro."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"-1", "1001", "99999999999"})
    @DisplayName("rejects eggs outside 0 to 1,000")
    void givenEggsOutOfRange_whenCreating_thenRejectAsOutOfRange(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> ProductionEntry.of(raw, NO_GRADES));

        // then
        assertThat(details).containsExactly(Map.entry("eggs", "Os ovos coletados devem ficar entre 0 e 1.000."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"0", "1000", "045"})
    @DisplayName("accepts eggs at the limits, zero included")
    void givenEggsWithinTheLimits_whenCreating_thenAcceptThem(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        ProductionEntry entry = ProductionEntry.of(raw, NO_GRADES);

        // then
        assertThat(entry.eggs()).isEqualTo(Integer.parseInt(raw));
    }

    @Test
    @DisplayName("takes a grade that is blank or missing as zero")
    void givenBlankAndMissingGrades_whenCreating_thenTakeThemAsZero() {
        // given
        EggGrades.Raw grades = new EggGrades.Raw("", " ", null, "", null, "");

        // when
        ProductionEntry entry = ProductionEntry.of("45", grades);

        // then
        assertThat(entry.grades()).isEqualTo(new EggGrades(0, 0, 0, 0, 0, 0));
    }

    @Test
    @DisplayName("keeps each grade in its place")
    void givenEveryGrade_whenCreating_thenKeepEachOne() {
        // given
        EggGrades.Raw grades = new EggGrades.Raw("1", "2", "3", "4", "5", "6");

        // when
        ProductionEntry entry = ProductionEntry.of("45", grades);

        // then
        assertThat(entry.grades()).isEqualTo(new EggGrades(1, 2, 3, 4, 5, 6));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("grades")
    @DisplayName("rejects a grade that is not an integer, in its own field and by its name")
    void givenGradeThatIsNotAnInteger_whenCreating_thenRejectInItsField(
            String field, String name, Function<String, EggGrades.Raw> only) {
        // given
        EggGrades.Raw grades = only.apply("2,5");

        // when
        Map<String, String> details = detailsOf(() -> ProductionEntry.of("45", grades));

        // then
        assertThat(details).containsExactly(Map.entry(field, "A quantidade de " + name + " deve ser um número inteiro."));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("grades")
    @DisplayName("rejects a grade outside 0 to 1,000, in its own field and by its name")
    void givenGradeOutOfRange_whenCreating_thenRejectInItsField(
            String field, String name, Function<String, EggGrades.Raw> only) {
        // given
        EggGrades.Raw grades = only.apply("-1");

        // when
        Map<String, String> details = detailsOf(() -> ProductionEntry.of("45", grades));

        // then
        assertThat(details)
                .containsExactly(Map.entry(field, "A quantidade de " + name + " deve ficar entre 0 e 1.000."));
    }

    @Test
    @DisplayName("rejects a grade above 1,000 in its own field, even with the eggs at the limit")
    void givenGradeAboveOneThousand_whenCreating_thenRejectInItsField() {
        // given
        EggGrades.Raw grades = new EggGrades.Raw(null, null, null, "1001", null, null);

        // when
        Map<String, String> details = detailsOf(() -> ProductionEntry.of("1000", grades));

        // then
        assertThat(details)
                .containsExactly(Map.entry("cracked", "A quantidade de trincados deve ficar entre 0 e 1.000."));
    }

    @Test
    @DisplayName("names the grade codes of each rule")
    void givenGradesBreakingEachRule_whenCreating_thenUseTheCodeOfEachRule() {
        // given
        EggGrades.Raw grades = new EggGrades.Raw(null, null, "-1", "2,5", null, null);

        // when
        List<Violation> violations = violationsOf(() -> ProductionEntry.of("45", grades));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactlyInAnyOrder(
                        tuple("dirty", ProductionErrorCode.EGG_GRADE_OUT_OF_RANGE),
                        tuple("cracked", ProductionErrorCode.EGG_GRADE_NOT_INTEGER));
    }

    @Test
    @DisplayName("rejects grades that add up to more than the eggs, in the eggs field, with both numbers")
    void givenGradesAboveTheEggs_whenCreating_thenRejectInTheEggsField() {
        // given
        EggGrades.Raw grades = new EggGrades.Raw("10", "10", "10", "4", null, null);

        // when
        Map<String, String> details = detailsOf(() -> ProductionEntry.of("30", grades));

        // then
        assertThat(details)
                .containsExactly(Map.entry("eggs", "As classificações somam 34, mais que os 30 ovos coletados."));
    }

    @Test
    @DisplayName("refuses the sum with its own code")
    void givenGradesAboveTheEggs_whenCreating_thenUseTheSumCode() {
        // given
        EggGrades.Raw grades = new EggGrades.Raw("10", "10", "10", "4", null, null);

        // when
        List<Violation> violations = violationsOf(() -> ProductionEntry.of("30", grades));

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(ProductionErrorCode.EGG_GRADES_EXCEED_EGGS);
        assertThat(refusalCodeOf(() -> ProductionEntry.of("30", grades))).isEqualTo(ProductionErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("names a single egg in the singular")
    void givenOneEggAndTwoGrades_whenCreating_thenWriteTheEggInTheSingular() {
        // given
        EggGrades.Raw grades = new EggGrades.Raw("1", "1", null, null, null, null);

        // when
        Map<String, String> details = detailsOf(() -> ProductionEntry.of("1", grades));

        // then
        assertThat(details).containsExactly(Map.entry("eggs", "As classificações somam 2, mais que o 1 ovo coletado."));
    }

    @Test
    @DisplayName("accepts grades that add up to exactly the eggs")
    void givenGradesEqualToTheEggs_whenCreating_thenAcceptThem() {
        // given
        EggGrades.Raw grades = new EggGrades.Raw("10", "10", "10", null, null, null);

        // when
        ProductionEntry entry = ProductionEntry.of("30", grades);

        // then
        assertThat(entry.grades().total()).isEqualTo(30);
        assertThat(entry.standardEggs()).isZero();
    }

    @Test
    @DisplayName("rejects every invalid field at once")
    void givenBlankEggsAndTwoInvalidGrades_whenCreating_thenRejectTheThreeTogether() {
        // given
        EggGrades.Raw grades = new EggGrades.Raw(null, null, "-1", "2,5", null, null);

        // when
        Map<String, String> details = detailsOf(() -> ProductionEntry.of("", grades));

        // then
        assertThat(details)
                .containsOnly(
                        Map.entry("eggs", "Informe os ovos coletados."),
                        Map.entry("cracked", "A quantidade de trincados deve ser um número inteiro."),
                        Map.entry("dirty", "A quantidade de sujos deve ficar entre 0 e 1.000."));
    }

    @Test
    @DisplayName("checks the sum only when the eggs and the grades are valid")
    void givenInvalidGradeAndLargeSum_whenCreating_thenLeaveTheSumOut() {
        // given
        EggGrades.Raw grades = new EggGrades.Raw("500", "500", "abc", null, null, null);

        // when
        Map<String, String> details = detailsOf(() -> ProductionEntry.of("30", grades));

        // then
        assertThat(details).containsOnlyKeys("dirty");
    }

    @Test
    @DisplayName("derives the standard eggs and the unsellable ones")
    void givenFortyFiveEggsTwoCrackedAndOneDirty_whenDeriving_thenFindFortyTwoStandardAndTwoUnsellable() {
        // given
        EggGrades.Raw grades = new EggGrades.Raw(null, null, "1", "2", null, null);

        // when
        ProductionEntry entry = ProductionEntry.of("45", grades);

        // then
        assertThat(entry.standardEggs()).isEqualTo(42);
        assertThat(entry.unsellableEggs()).isEqualTo(2);
    }

    @Test
    @DisplayName("counts cracked, blood spot and abnormal eggs as unsellable, and not the small, jumbo or dirty ones")
    void givenEveryGrade_whenDeriving_thenCountOnlyTheUnsellableGrades() {
        // given
        EggGrades.Raw grades = new EggGrades.Raw("1", "2", "3", "4", "5", "6");

        // when
        ProductionEntry entry = ProductionEntry.of("45", grades);

        // then
        assertThat(entry.unsellableEggs()).isEqualTo(15);
        assertThat(entry.standardEggs()).isEqualTo(24);
    }
}
