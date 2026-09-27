package io.github.ovyx.farm.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Violation;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** O dia da pesagem (FR-003 da 005): uma data ISO, não futura no fuso da granja. */
@DisplayName("WeighingDate")
class WeighingDateTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"2026-09-24", "2026-08-27", "2020-01-01"})
    @DisplayName("accepts today and any day before it")
    void givenTodayOrBefore_whenCreating_thenAcceptTheDay(String raw) {
        // given
        String typed = raw;

        // when
        WeighingDate day = WeighingDate.of(typed, TODAY);

        // then
        assertThat(day.value()).isEqualTo(LocalDate.parse(raw));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("asks for the day when it is missing")
    void givenMissingDay_whenValidating_thenAskForIt(String raw) {
        // given
        Notification notification = new Notification();

        // when
        WeighingDate.validate(raw, TODAY, notification);

        // then
        assertThat(notification.violations())
                .extracting(Violation::field, Violation::code, Violation::message)
                .containsExactly(tuple("weighedOn", FarmErrorCode.WEIGHED_ON_REQUIRED, "Informe a data da pesagem."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"24/09/2026", "2026-02-30", "2026-9-24", "ontem"})
    @DisplayName("refuses what is not an ISO date")
    void givenDayOutOfTheFormat_whenValidating_thenRefuseTheFormat(String raw) {
        // given
        Notification notification = new Notification();

        // when
        WeighingDate.validate(raw, TODAY, notification);

        // then
        assertThat(notification.violations())
                .extracting(Violation::field, Violation::code, Violation::message)
                .containsExactly(tuple(
                        "weighedOn",
                        FarmErrorCode.WEIGHED_ON_INVALID,
                        "Informe a data da pesagem no formato AAAA-MM-DD."));
    }

    @Test
    @DisplayName("refuses a day after today at the farm")
    void givenTomorrow_whenValidating_thenRefuseTheFuture() {
        // given
        Notification notification = new Notification();

        // when
        WeighingDate.validate("2026-09-25", TODAY, notification);

        // then
        assertThat(notification.violations())
                .extracting(Violation::field, Violation::code, Violation::message)
                .containsExactly(tuple(
                        "weighedOn", FarmErrorCode.WEIGHED_ON_IN_FUTURE, "A data da pesagem não pode ser futura."));
    }
}
