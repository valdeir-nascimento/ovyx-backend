package io.github.ovyx.production.application.common;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.shared.domain.FixedClock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * "Hoje" e "agora" no fuso da granja (R-006): o relatório é do dia da granja, e não do instante em UTC.
 */
@DisplayName("FarmCalendar")
class FarmCalendarTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Test
    @DisplayName("keeps the day of the farm when UTC has already turned to the next one")
    void givenLateEveningAtTheFarm_whenAskingForToday_thenAnswerTheDayOfTheFarm() {
        // given
        FarmCalendar calendar = new FarmCalendar(new FixedClock(Instant.parse("2026-09-25T02:30:00Z")), SAO_PAULO);

        // when
        LocalDate today = calendar.today();
        LocalTime now = calendar.now();

        // then
        assertThat(today).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(now).isEqualTo(LocalTime.of(23, 30));
    }

    @Test
    @DisplayName("answers the time of the farm without seconds")
    void givenMorningAtTheFarm_whenAskingForNow_thenAnswerHoursAndMinutesOnly() {
        // given
        FarmCalendar calendar = new FarmCalendar(new FixedClock(Instant.parse("2026-09-25T09:42:10Z")), SAO_PAULO);

        // when
        LocalDate today = calendar.today();
        LocalTime now = calendar.now();

        // then
        assertThat(today).isEqualTo(LocalDate.of(2026, 9, 25));
        assertThat(now).isEqualTo(LocalTime.of(6, 42));
    }
}
