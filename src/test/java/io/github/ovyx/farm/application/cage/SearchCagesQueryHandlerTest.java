package io.github.ovyx.farm.application.cage;

import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.ParameterizedTest;
import java.util.List;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.math.BigDecimal;
import io.github.ovyx.shared.domain.WeighingStanding;
import io.github.ovyx.shared.domain.WeighingSituation;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.domain.model.CageId;
import java.time.ZoneId;
import io.github.ovyx.shared.domain.FixedClock;
import io.github.ovyx.shared.application.FarmCalendar;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.PageResponse;
import io.github.ovyx.shared.application.Result;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pesquisa das gaiolas de um setor, com busca pelo código, filtros e páginas (FR-010). */
@DisplayName("SearchCagesQueryHandler")
class SearchCagesQueryHandlerTest {

    private final SectorId sectorId = SectorId.generate();
    private final RecordingCageDirectory directory = new RecordingCageDirectory().withSector(sectorId);
    /** Domingo, 27/09/2026, ao meio-dia na granja. */
    private final FarmCalendar calendar =
            new FarmCalendar(FixedClock.at("2026-09-27T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final SearchCagesQueryHandler handler = new SearchCagesQueryHandler(directory, calendar);

    @Test
    @DisplayName("asks for the active cages when no status is asked, with the battery in capitals")
    void givenNoStatusAndBatteryInLowerCase_whenSearching_thenAskForTheActiveCagesOfTheBatteryInCapitals() {
        // given
        SearchCagesQuery query = new SearchCagesQuery(sectorId.toString(), " b-0 ", " b ", null, 0, 20, null);

        // when
        Result<PageResponse<CageSummary>> result = handler.handle(query);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(directory.searches())
                .containsExactly(new RecordingCageDirectory.Search(sectorId, "b-0", "B", StatusFilter.ACTIVE, 0, 20, null));
    }

    @Test
    @DisplayName("leaves a blank code and a blank battery out of the search")
    void givenBlankCodeAndBattery_whenSearching_thenSearchWithoutThem() {
        // given
        SearchCagesQuery query = new SearchCagesQuery(sectorId.toString(), "  ", "", StatusFilter.ALL, 1, 10, null);

        // when
        handler.handle(query);

        // then
        assertThat(directory.searches())
                .containsExactly(new RecordingCageDirectory.Search(sectorId, null, null, StatusFilter.ALL, 1, 10, null));
    }

    @Test
    @DisplayName("refuses a negative page and a size outside 1 to 100 at once")
    void givenNegativePageAndSizeAbove100_whenSearching_thenFailAsValidationWithBoth() {
        // given
        SearchCagesQuery query = new SearchCagesQuery(sectorId.toString(), null, null, null, -1, 101, null);

        // when
        Result<PageResponse<CageSummary>> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.error().message()).isEqualTo("A requisição contém campos inválidos.");
        assertThat(result.error().details())
                .containsExactly(
                        Map.entry("page", "A página começa em 0."),
                        Map.entry("size", "O tamanho da página deve estar entre 1 e 100."));
        assertThat(directory.searches()).isEmpty();
    }

    @Test
    @DisplayName("fails as not found for a sector that does not exist or a malformed identifier")
    void givenUnknownSector_whenSearching_thenFailAsSectorNotFound() {
        // given
        SearchCagesQuery unknown = new SearchCagesQuery(SectorId.generate().toString(), null, null, null, 0, 20, null);
        SearchCagesQuery malformed = new SearchCagesQuery("galpao-9", null, null, null, 0, 20, null);

        // when
        Result<PageResponse<CageSummary>> unknownResult = handler.handle(unknown);
        Result<PageResponse<CageSummary>> malformedResult = handler.handle(malformed);

        // then
        assertThat(unknownResult.error().code()).isEqualTo("SECTOR_NOT_FOUND");
        assertThat(malformedResult.error().code()).isEqualTo("SECTOR_NOT_FOUND");
        assertThat(directory.searches()).isEmpty();
    }

    // ---------------------------------------------------------------- agenda de pesagem (010)

    private CageSummary cage(String code, Status status, String lastWeighedOn) {
        return new CageSummary(
                CageId.generate(),
                sectorId,
                code,
                code.substring(0, 1),
                Integer.parseInt(code.substring(2)),
                50,
                status,
                lastWeighedOn == null
                        ? null
                        : new CageLastWeighing(LocalDate.parse(lastWeighedOn), new BigDecimal("160.0")),
                null);
    }

    private List<CageSummary> galpaoUm() {
        return List.of(
                cage("A-01", Status.ACTIVE, "2026-09-24"),
                cage("B-07", Status.ACTIVE, "2026-09-18"),
                cage("C-03", Status.ACTIVE, null),
                cage("D-01", Status.INACTIVE, null));
    }

    @Test
    @DisplayName("gives each active cage its standing in the schedule of the sector, and none to the inactive")
    void givenSectorWeighedOnFridays_whenSearchingOnSunday_thenGiveEachActiveCageItsStanding() {
        // given
        directory.withSchedule(sectorId, new SectorSchedule(Status.ACTIVE, DayOfWeek.FRIDAY)).answering(galpaoUm());

        // when
        List<CageSummary> cages = handler.handle(new SearchCagesQuery(sectorId.toString(), null, null, null, 0, 20, null))
                .value()
                .content();

        // then
        assertThat(cages)
                .extracting(CageSummary::weighing)
                .containsExactly(
                        new WeighingStanding(WeighingSituation.UP_TO_DATE, null, LocalDate.of(2026, 10, 2)),
                        new WeighingStanding(WeighingSituation.LATE, LocalDate.of(2026, 9, 25), null),
                        new WeighingStanding(WeighingSituation.NEVER_WEIGHED, null, null),
                        null);
    }

    @Test
    @DisplayName("gives no standing to the cages of an inactive sector")
    void givenInactiveSector_whenSearching_thenGiveNoStanding() {
        // given
        directory.withSchedule(sectorId, new SectorSchedule(Status.INACTIVE, DayOfWeek.FRIDAY)).answering(galpaoUm());

        // when
        List<CageSummary> cages = handler.handle(new SearchCagesQuery(sectorId.toString(), null, null, StatusFilter.ALL, 0, 20, null))
                .value()
                .content();

        // then
        assertThat(cages).extracting(CageSummary::weighing).containsOnlyNulls();
    }

    @ParameterizedTest(name = "weighing day {0}: pending since {1}")
    @CsvSource(value = {"FRIDAY, 2026-09-19", "NULL, 2026-09-20"}, nullValues = "NULL")
    @DisplayName("asks for the cages without a valid weighing since the start of the week, with the pending filter")
    void givenPendingFilter_whenSearching_thenAskForTheCagesWithoutWeighingSinceTheWeekStarted(
            DayOfWeek weighingDay, LocalDate pendingSince) {
        // given
        directory.withSchedule(sectorId, new SectorSchedule(Status.ACTIVE, weighingDay));
        SearchCagesQuery query =
                new SearchCagesQuery(sectorId.toString(), null, " b ", null, 0, 20, WeighingFilter.PENDING);

        // when
        handler.handle(query);

        // then
        assertThat(directory.searches())
                .containsExactly(new RecordingCageDirectory.Search(
                        sectorId, null, "B", StatusFilter.ACTIVE, 0, 20, pendingSince));
    }

    @Test
    @DisplayName("answers an empty page for the pending filter in an inactive sector, without searching")
    void givenInactiveSectorAndPendingFilter_whenSearching_thenAnswerAnEmptyPageWithoutSearching() {
        // given
        directory.withSchedule(sectorId, new SectorSchedule(Status.INACTIVE, null)).answering(galpaoUm());
        SearchCagesQuery query =
                new SearchCagesQuery(sectorId.toString(), null, null, null, 0, 20, WeighingFilter.PENDING);

        // when
        PageResponse<CageSummary> page = handler.handle(query).value();

        // then
        assertThat(page.content()).isEmpty();
        assertThat(page.metadata().totalElements()).isZero();
        assertThat(directory.searches()).isEmpty();
    }
}
