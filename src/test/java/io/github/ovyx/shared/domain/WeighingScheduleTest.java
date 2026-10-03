package io.github.ovyx.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * A semana da pesagem (R-002 da feature 010): os 7 dias que terminam no ultimo dia de pesagem que ja chegou,
 * contando a pesagem feita depois dele; sem dia, o prazo de 7 dias desde a ultima.
 */
@DisplayName("WeighingSchedule")
class WeighingScheduleTest {

    private static final WeighingSchedule FRIDAYS = new WeighingSchedule(DayOfWeek.FRIDAY);
    private static final WeighingSchedule EVERY_SEVEN_DAYS = new WeighingSchedule(null);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 9, 27);
    private static final LocalDate FRIDAY = LocalDate.of(2026, 9, 25);

    @ParameterizedTest(name = "on {0} the weighing day reached is {1}, counting from {2}")
    @CsvSource({
        "2026-09-25, 2026-09-25, 2026-09-19, true",
        "2026-09-26, 2026-09-25, 2026-09-19, false",
        "2026-09-27, 2026-09-25, 2026-09-19, false",
        "2026-09-28, 2026-09-25, 2026-09-19, false",
        "2026-09-29, 2026-09-25, 2026-09-19, false",
        "2026-09-30, 2026-09-25, 2026-09-19, false",
        "2026-10-01, 2026-09-25, 2026-09-19, false",
        "2026-10-02, 2026-10-02, 2026-09-26, true"
    })
    @DisplayName("takes the last weighing day reached and the six days before it as the weighing week")
    void givenWeighingOnFridays_whenReadingTheWeekOfEachDay_thenEndItOnTheLastFridayReached(
            LocalDate today, LocalDate dueOn, LocalDate requiredSince, boolean weighingDay) {
        // given — a pesagem as sextas e o dia de hoje vindo do @CsvSource

        // when
        LocalDate due = FRIDAYS.dueOn(today);
        LocalDate since = FRIDAYS.requiredSince(today);
        boolean isWeighingDay = FRIDAYS.isWeighingDay(today);

        // then
        assertThat(due).isEqualTo(dueOn);
        assertThat(since).isEqualTo(requiredSince);
        assertThat(isWeighingDay).isEqualTo(weighingDay);
    }

    @ParameterizedTest(name = "on Sunday, weighed on {0}: {1}")
    @CsvSource({
        "2026-09-19, UP_TO_DATE, , 2026-10-02",
        "2026-09-18, LATE, 2026-09-25, ",
        "2026-09-26, UP_TO_DATE, , 2026-10-02",
        "2026-09-24, UP_TO_DATE, , 2026-10-02",
        "2026-09-27, UP_TO_DATE, , 2026-10-02",
        "2026-08-14, LATE, 2026-09-25, "
    })
    @DisplayName("counts a weighing from the start of the week on, also one made after the weighing day")
    void givenWeighingOnFridays_whenStandingOnSunday_thenCountTheWeighingsSinceTheWeekStarted(
            LocalDate lastWeighedOn, WeighingSituation situation, LocalDate lateSince, LocalDate nextOn) {
        // given — a ultima pesagem vinda do @CsvSource, num domingo

        // when
        WeighingStanding standing = FRIDAYS.standingOf(SUNDAY, lastWeighedOn);

        // then
        assertThat(standing).isEqualTo(new WeighingStanding(situation, lateSince, nextOn));
    }

    @ParameterizedTest(name = "on the weighing day, weighed on {0}: {1}")
    @CsvSource({
        "2026-09-18, DUE_TODAY, ",
        "2026-09-19, UP_TO_DATE, 2026-10-02",
        "2026-09-25, UP_TO_DATE, 2026-10-02"
    })
    @DisplayName("asks to weigh today, on the weighing day, the cage still without the weighing of the week")
    void givenWeighingOnFridays_whenStandingOnFriday_thenAskToWeighTodayTheCageWithoutTheWeighing(
            LocalDate lastWeighedOn, WeighingSituation situation, LocalDate nextOn) {
        // given — a ultima pesagem vinda do @CsvSource, numa sexta

        // when
        WeighingStanding standing = FRIDAYS.standingOf(FRIDAY, lastWeighedOn);

        // then
        assertThat(standing).isEqualTo(new WeighingStanding(situation, null, nextOn));
    }

    @ParameterizedTest(name = "today {0}")
    @CsvSource({"2026-09-25", "2026-09-27"})
    @DisplayName("tells a cage never weighed apart, on the weighing day and after it")
    void givenCageNeverWeighed_whenStanding_thenNeverWeighed(LocalDate today) {
        // given — hoje vindo do @CsvSource e nenhuma pesagem

        // when
        WeighingStanding standing = FRIDAYS.standingOf(today, null);

        // then
        assertThat(standing).isEqualTo(new WeighingStanding(WeighingSituation.NEVER_WEIGHED, null, null));
    }

    @ParameterizedTest(name = "weighed on {0}: {1}")
    @CsvSource({
        "2026-09-20, UP_TO_DATE, , 2026-09-27",
        "2026-09-27, UP_TO_DATE, , 2026-10-04",
        "2026-09-19, LATE, 2026-09-27, ",
        "2026-09-01, LATE, 2026-09-09, "
    })
    @DisplayName("without a weighing day, accepts a last weighing up to 7 days old")
    void givenNoWeighingDay_whenStanding_thenAcceptAWeighingUpToSevenDaysOld(
            LocalDate lastWeighedOn, WeighingSituation situation, LocalDate lateSince, LocalDate nextOn) {
        // given — sem dia definido, e a ultima pesagem vinda do @CsvSource

        // when
        WeighingStanding standing = EVERY_SEVEN_DAYS.standingOf(SUNDAY, lastWeighedOn);

        // then
        assertThat(standing).isEqualTo(new WeighingStanding(situation, lateSince, nextOn));
    }

    @Test
    @DisplayName("without a weighing day, counts from 7 days ago and never has a weighing day")
    void givenNoWeighingDay_whenReadingTheWeek_thenCountFromSevenDaysAgoWithoutWeighingDay() {
        // given
        LocalDate today = SUNDAY;

        // when
        LocalDate since = EVERY_SEVEN_DAYS.requiredSince(today);
        LocalDate due = EVERY_SEVEN_DAYS.dueOn(today);
        boolean weighingDay = EVERY_SEVEN_DAYS.isWeighingDay(today);

        // then
        assertThat(since).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(due).isNull();
        assertThat(weighingDay).isFalse();
    }

    @Test
    @DisplayName("without a weighing day, tells a cage never weighed apart")
    void givenNoWeighingDayAndCageNeverWeighed_whenStanding_thenNeverWeighed() {
        // given
        LocalDate lastWeighedOn = null;

        // when
        WeighingStanding standing = EVERY_SEVEN_DAYS.standingOf(SUNDAY, lastWeighedOn);

        // then
        assertThat(standing.situation()).isEqualTo(WeighingSituation.NEVER_WEIGHED);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(WeighingSituation.class)
    @DisplayName("is pending in every situation but up to date")
    void givenSituation_whenAskingIfPending_thenOnlyUpToDateIsNot(WeighingSituation situation) {
        // given
        WeighingStanding standing = switch (situation) {
            case UP_TO_DATE -> new WeighingStanding(situation, null, FRIDAY);
            case LATE -> new WeighingStanding(situation, FRIDAY, null);
            default -> new WeighingStanding(situation, null, null);
        };

        // when
        boolean pending = standing.isPending();

        // then
        assertThat(pending).isEqualTo(situation != WeighingSituation.UP_TO_DATE);
    }

    @ParameterizedTest(name = "{0}, late since {1}, next on {2}")
    @CsvSource({
        "UP_TO_DATE, , ",
        "UP_TO_DATE, 2026-09-25, 2026-10-02",
        "LATE, , ",
        "LATE, 2026-09-25, 2026-10-02",
        "DUE_TODAY, 2026-09-25, ",
        "DUE_TODAY, , 2026-10-02",
        "NEVER_WEIGHED, 2026-09-25, ",
        "NEVER_WEIGHED, , 2026-10-02"
    })
    @DisplayName("refuses a date that does not belong to the situation, or the lack of the one that does")
    void givenDatesThatDoNotMatchTheSituation_whenCreatingTheStanding_thenRefuse(
            WeighingSituation situation, LocalDate lateSince, LocalDate nextOn) {
        // given — as datas vindas do @CsvSource

        // when / then
        assertThatThrownBy(() -> new WeighingStanding(situation, lateSince, nextOn))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
