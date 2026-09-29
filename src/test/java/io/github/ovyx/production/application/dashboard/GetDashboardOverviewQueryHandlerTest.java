package io.github.ovyx.production.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.fixtures.InMemoryDashboardDirectory;
import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** O cabeçalho do painel (US1 da 006; FR-002, FR-004, FR-023, R-003): o dia da granja, a parte do dia e as abas. */
@DisplayName("GetDashboardOverviewQueryHandler")
class GetDashboardOverviewQueryHandlerTest {

    private final InMemoryDashboardDirectory directory = new InMemoryDashboardDirectory();

    private GetDashboardOverviewQueryHandler handlerAt(String instant) {
        FixedClock clock = FixedClock.at(instant);
        return new GetDashboardOverviewQueryHandler(directory, new FarmCalendar(clock, ZoneId.of("America/Sao_Paulo")));
    }

    @Test
    @DisplayName("tells the day of the farm, the active sectors, the complete ones and the tabs")
    void givenSectors_whenReadingTheOverview_thenTellTheDayTheCountsAndTheTabs() {
        // given
        DashboardSector codornas =
                new DashboardSector(UUID.fromString("3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11"), "Codornas — Galpão 1");
        directory.answerOverview(new DashboardSectors(3, 2, List.of(codornas)));

        // when
        Result<DashboardOverview> result = handlerAt("2026-09-24T10:12:40Z").handle(new GetDashboardOverviewQuery());

        // then
        DashboardOverview overview = result.value();
        assertThat(overview.today()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(overview.activeSectors()).isEqualTo(3);
        assertThat(overview.completeToday()).isEqualTo(2);
        assertThat(overview.sectors()).containsExactly(codornas);
    }

    @Test
    @DisplayName("tells the day of the farm, and not the one in UTC, late in the evening")
    void givenLateEveningAtTheFarm_whenReadingTheOverview_thenKeepTheDayOfTheFarm() {
        // given
        String lateEvening = "2026-09-25T02:30:00Z";

        // when
        DashboardOverview overview =
                handlerAt(lateEvening).handle(new GetDashboardOverviewQuery()).value();

        // then
        assertThat(overview.today()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(overview.partOfDay()).isEqualTo(PartOfDay.EVENING);
    }

    @ParameterizedTest(name = "{0} is {1}")
    @CsvSource({
        "2026-09-24T08:00:00Z, MORNING",
        "2026-09-24T14:59:00Z, MORNING",
        "2026-09-24T15:00:00Z, AFTERNOON",
        "2026-09-24T20:59:00Z, AFTERNOON",
        "2026-09-24T21:00:00Z, EVENING",
        "2026-09-24T07:59:00Z, EVENING"
    })
    @DisplayName("tells the part of the day on the clock of the farm, for the greeting")
    void givenHourAtTheFarm_whenReadingTheOverview_thenTellThePartOfTheDay(String instant, PartOfDay expected) {
        // given
        GetDashboardOverviewQueryHandler handler = handlerAt(instant);

        // when
        DashboardOverview overview = handler.handle(new GetDashboardOverviewQuery()).value();

        // then
        assertThat(overview.partOfDay()).isEqualTo(expected);
    }
}
