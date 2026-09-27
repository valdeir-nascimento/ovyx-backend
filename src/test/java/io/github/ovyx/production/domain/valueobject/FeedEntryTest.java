package io.github.ovyx.production.domain.valueobject;

import static io.github.ovyx.production.domain.model.CatalogFormulas.POSTURA_PLUS;
import static io.github.ovyx.production.domain.model.CatalogFormulas.POSTURA_PLUS_ID;
import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.production.domain.model.CatalogFormula;
import io.github.ovyx.shared.domain.Violation;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * O lançamento de ração de uma gaiola: a fórmula, com o preço e o consumo esperado dela guardados, e o
 * consumo do dia, inteiro de 0 a 50.000 g (data-model.md da 004; R-005, R-011).
 */
@DisplayName("FeedEntry")
class FeedEntryTest {

    @Test
    @DisplayName("keeps the formula, its price and its expected intake as they were, with the consumption")
    void givenFormulaAndConsumption_whenCreating_thenKeepThePriceAndTheIntakeOfTheFormula() {
        // given
        String consumption = " 1250 ";

        // when
        FeedEntry entry = FeedEntry.of(POSTURA_PLUS, consumption);

        // then
        assertThat(entry).isEqualTo(new FeedEntry(POSTURA_PLUS_ID, new BigDecimal("2.85"), 28, 1250));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("rejects a missing consumption")
    void givenMissingConsumption_whenCreating_thenRejectAsRequired(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> FeedEntry.of(POSTURA_PLUS, raw));

        // then
        assertThat(details).containsExactly(Map.entry("consumption", "Informe o consumo em gramas."));
        assertThat(violationsOf(() -> FeedEntry.of(POSTURA_PLUS, raw)))
                .extracting(Violation::code)
                .containsExactly(ProductionErrorCode.CONSUMPTION_REQUIRED);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"12,5", "1.5", "muito"})
    @DisplayName("rejects a consumption that is not an integer, with the message the contract publishes")
    void givenConsumptionThatIsNotAnInteger_whenCreating_thenRejectAsNotInteger(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> FeedEntry.of(POSTURA_PLUS, raw));

        // then
        assertThat(details)
                .containsExactly(Map.entry("consumption", "O consumo deve ser um número inteiro de gramas."));
        assertThat(violationsOf(() -> FeedEntry.of(POSTURA_PLUS, raw)))
                .extracting(Violation::code)
                .containsExactly(ProductionErrorCode.CONSUMPTION_NOT_INTEGER);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"-1", "50001", "60000", "99999999999"})
    @DisplayName("rejects a consumption outside 0 to 50,000 grams")
    void givenConsumptionOutsideTheRange_whenCreating_thenRejectAsOutOfRange(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> FeedEntry.of(POSTURA_PLUS, raw));

        // then
        assertThat(details)
                .containsExactly(Map.entry("consumption", "O consumo deve ficar entre 0 e 50.000 gramas."));
        assertThat(violationsOf(() -> FeedEntry.of(POSTURA_PLUS, raw)))
                .extracting(Violation::code)
                .containsExactly(ProductionErrorCode.CONSUMPTION_OUT_OF_RANGE);
    }

    @ParameterizedTest(name = "{0} g")
    @ValueSource(strings = {"0", "50000"})
    @DisplayName("accepts a consumption at the limits, zero included")
    void givenConsumptionAtTheLimits_whenCreating_thenAcceptIt(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        FeedEntry entry = FeedEntry.of(POSTURA_PLUS, raw);

        // then
        assertThat(entry.consumption()).isEqualTo(Integer.parseInt(raw));
    }

    @ParameterizedTest(name = "{0} birds")
    @CsvSource({"48, 1344", "50, 1400", "0, 0"})
    @DisplayName("suggests the birds of the cage times the expected intake of the formula")
    void givenBirds_whenSuggesting_thenProposeTheBirdsTimesTheExpectedIntake(int birds, int consumption) {
        // given
        int birdCount = birds;

        // when
        FeedEntry suggested = FeedEntry.suggestedFor(POSTURA_PLUS, birdCount);

        // then
        assertThat(suggested).isEqualTo(new FeedEntry(POSTURA_PLUS_ID, new BigDecimal("2.85"), 28, consumption));
        assertThat(suggested.withinLimits()).isTrue();
    }

    @Test
    @DisplayName("tells a suggestion above 50,000 grams is outside the limits")
    void givenBirdsTimesIntakeAboveTheMaximum_whenSuggesting_thenFindItOutsideTheLimits() {
        // given
        int birds = 1000;

        // when
        FeedEntry suggested = FeedEntry.suggestedFor(POSTURA_PLUS, birds);

        // then
        assertThat(suggested.consumption()).isEqualTo(28_000);
        assertThat(FeedEntry.suggestedFor(POSTURA_PLUS, 1786).withinLimits()).isFalse();
        assertThat(FeedEntry.suggestedFor(POSTURA_PLUS, 1785).withinLimits()).isTrue();
    }

    @Test
    @DisplayName("keeps a proposal of exactly 50,000 grams inside the limits")
    void givenBirdsTimesIntakeOfExactlyTheMaximum_whenSuggesting_thenFindItInsideTheLimits() {
        // given
        CatalogFormula heavy = new CatalogFormula(POSTURA_PLUS_ID, "Engorda", true, new BigDecimal("2.85"), 200);

        // when
        FeedEntry suggested = FeedEntry.suggestedFor(heavy, 250);

        // then
        assertThat(suggested.consumption()).isEqualTo(50_000);
        assertThat(suggested.withinLimits()).isTrue();
    }
}
