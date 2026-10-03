package io.github.ovyx.farm.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Violation;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Dia da pesagem do setor: opcional; de segunda a domingo; vazio e "sem dia fixo" (R-003 da feature 010). */
@DisplayName("WeighingDay")
class WeighingDayTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("turns a missing or blank day into no weighing day")
    void givenMissingOrBlankDay_whenCreating_thenFindNoWeighingDay(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Optional<WeighingDay> day = WeighingDay.optionalOf(raw);

        // then
        assertThat(day).isEmpty();
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"FRIDAY", "friday", " Friday "})
    @DisplayName("reads the day of the week regardless of case and surrounding spaces")
    void givenDayOfTheWeek_whenCreating_thenKeepTheDay(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Optional<WeighingDay> day = WeighingDay.optionalOf(raw);

        // then
        assertThat(day).map(WeighingDay::value).contains(DayOfWeek.FRIDAY);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"FUNDAY", "6", "SEXTA", "FRI"})
    @DisplayName("refuses what is not a day of the week, on the field, with the message the contract publishes")
    void givenTextThatIsNotADayOfTheWeek_whenCreating_thenRefuseOnTheField(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        List<Violation> violations = violationsOf(() -> WeighingDay.optionalOf(raw));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code, Violation::message)
                .containsExactly(tuple(
                        "weighingDay",
                        FarmErrorCode.WEIGHING_DAY_INVALID,
                        "Escolha um dia da semana, de segunda a domingo."));
    }
}
