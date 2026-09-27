package io.github.ovyx.production.application.dailyreport;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.application.common.FarmCalendar;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A sugestão de abertura do relatório (US1 e US3; FR-004), com "agora" = 25/09/2026 06:42 na granja. */
@DisplayName("SuggestDailyReportQueryHandler")
class SuggestDailyReportQueryHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-25T09:42:10Z");
    private final RecordingDailyReportDirectory directory = new RecordingDailyReportDirectory();
    private final SuggestDailyReportQueryHandler handler =
            new SuggestDailyReportQueryHandler(directory, new FarmCalendar(clock, ZoneId.of("America/Sao_Paulo")));
    private final UUID sectorId = UUID.randomUUID();

    private Result<DailyReportSuggestion> suggestion() {
        directory.knowSector(new ReportingSector(sectorId, "Codornas — Galpão 4", "ACTIVE"));
        return handler.handle(new SuggestDailyReportQuery(sectorId.toString()));
    }

    @Test
    @DisplayName("suggests the closing birds and the age of yesterday's report")
    void givenReportOfYesterday_whenSuggesting_thenTakeItsClosingBirdsAndAge() {
        // given
        directory.knowLatest(SectorId.of(sectorId), new LatestDailyReport(LocalDate.of(2026, 9, 24), 20, 96));

        // when
        Result<DailyReportSuggestion> result = suggestion();

        // then
        assertThat(result.value().openingBirdCount()).isEqualTo(96);
        assertThat(result.value().flockAge()).isEqualTo(20);
    }

    @Test
    @DisplayName("suggests no age above 150 weeks, the limit of the form")
    void givenLatestReportOfAnOldFlock_whenSuggesting_thenKeepTheAgeAtTheLimit() {
        // given
        directory.knowLatest(SectorId.of(sectorId), new LatestDailyReport(LocalDate.of(2026, 8, 28), 149, 96));

        // when
        Result<DailyReportSuggestion> result = suggestion();

        // then
        assertThat(result.value().flockAge()).isEqualTo(150);
    }

    @Test
    @DisplayName("suggests the birds of the active cages when the latest report removed every bird")
    void givenLatestReportWithNoBirdLeft_whenSuggesting_thenTakeTheBirdsOfTheCages() {
        // given
        directory.knowLatest(SectorId.of(sectorId), new LatestDailyReport(LocalDate.of(2026, 9, 24), 20, 0));
        directory.knowActiveBirds(SectorId.of(sectorId), 98);

        // when
        Result<DailyReportSuggestion> result = suggestion();

        // then
        assertThat(result.value().openingBirdCount()).isEqualTo(98);
        assertThat(result.value().flockAge()).isEqualTo(20);
    }

    @Test
    @DisplayName("adds the complete weeks since the latest report to its age")
    void givenReportOfTwoWeeksAgo_whenSuggesting_thenAddTheTwoWeeks() {
        // given
        directory.knowLatest(SectorId.of(sectorId), new LatestDailyReport(LocalDate.of(2026, 9, 11), 18, 96));

        // when
        Result<DailyReportSuggestion> result = suggestion();

        // then
        assertThat(result.value().flockAge()).isEqualTo(20);
    }

    @Test
    @DisplayName("suggests the birds of the active cages, and no age, for the first report")
    void givenNoReportYet_whenSuggesting_thenTakeTheBirdsOfTheCagesAndNoAge() {
        // given
        directory.knowActiveBirds(SectorId.of(sectorId), 98);

        // when
        Result<DailyReportSuggestion> result = suggestion();

        // then
        assertThat(result.value().openingBirdCount()).isEqualTo(98);
        assertThat(result.value().flockAge()).isNull();
    }

    @Test
    @DisplayName("suggests the date and the time of now at the farm, without seconds")
    void givenMorningAtTheFarm_whenSuggesting_thenSuggestTodayAndNow() {
        // given
        directory.knowActiveBirds(SectorId.of(sectorId), 98);

        // when
        Result<DailyReportSuggestion> result = suggestion();

        // then
        assertThat(result.value().collectionDate()).isEqualTo(LocalDate.of(2026, 9, 25));
        assertThat(result.value().collectionTime()).isEqualTo(LocalTime.of(6, 42));
    }

    @Test
    @DisplayName("fails as not found for a sector that does not exist")
    void givenUnknownSector_whenSuggesting_thenFailAsSectorNotFound() {
        // given
        SuggestDailyReportQuery query = new SuggestDailyReportQuery(UUID.randomUUID().toString());

        // when
        Result<DailyReportSuggestion> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }
}
