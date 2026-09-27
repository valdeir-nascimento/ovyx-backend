package io.github.ovyx.production.application.dailyreport;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.MARINA;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.aFarmSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.application.common.FarmCalendar;
import io.github.ovyx.production.domain.model.DailyReport;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.FarmSector;
import io.github.ovyx.production.fixtures.InMemoryDailyReportRepository;
import io.github.ovyx.production.fixtures.InMemoryFarmStructure;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Abertura do relatório do dia (US1; FR-001 a FR-005, FR-020). */
@DisplayName("OpenDailyReportCommandHandler")
class OpenDailyReportCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-25T09:44:03Z");
    private final InMemoryDailyReportRepository repository = new InMemoryDailyReportRepository();
    private final InMemoryFarmStructure farm = new InMemoryFarmStructure();
    private final OpenDailyReportCommandHandler handler = new OpenDailyReportCommandHandler(
            repository, farm, new FarmCalendar(clock, ZoneId.of("America/Sao_Paulo")), clock);

    private OpenDailyReportCommand opening(FarmSector sector, String date) {
        return new OpenDailyReportCommand(sector.id().toString(), date, "06:42", "96", "20", "Tarde quente.", MARINA);
    }

    @Test
    @DisplayName("opens the report with the actor of the command and saves it")
    void givenValidReport_whenOpening_thenSaveItWithTheActor() {
        // given
        FarmSector sector = farm.put(aFarmSector().build());

        // when
        Result<DailyReportId> result = handler.handle(opening(sector, "2026-09-25"));

        // then
        DailyReport report = repository.findById(sector.id(), result.value()).orElseThrow();
        assertThat(report.openedBy()).isEqualTo(MARINA);
        assertThat(report.openedAt()).isEqualTo(clock.instant());
        assertThat(report.cages()).hasSize(2);
        assertThat(repository.saves()).isEqualTo(1);
    }

    @Test
    @DisplayName("fails as validation with every invalid field, and saves nothing")
    void givenInvalidFields_whenOpening_thenFailAsValidationAndSaveNothing() {
        // given
        FarmSector sector = farm.put(aFarmSector().build());

        // when
        Result<DailyReportId> result = handler.handle(new OpenDailyReportCommand(
                sector.id().toString(), "2026-09-26", "25:00", "0", "200", null, MARINA));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details())
                .containsOnlyKeys("collectionDate", "collectionTime", "openingBirdCount", "flockAge");
        assertThat(repository.saves()).isZero();
    }

    @Test
    @DisplayName("fails as conflict with the date of another report of the sector")
    void givenReportOnTheDate_whenOpeningAnotherOnTheSameDate_thenFailAsConflict() {
        // given
        FarmSector sector = farm.put(aFarmSector().build());
        handler.handle(opening(sector, "2026-09-24"));

        // when
        Result<DailyReportId> result = handler.handle(opening(sector, "2026-09-24"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("DAILY_REPORT_ALREADY_EXISTS");
        assertThat(repository.saves()).isEqualTo(1);
    }

    @Test
    @DisplayName("fails as conflict in an inactive sector")
    void givenInactiveSector_whenOpening_thenFailAsSectorInactive() {
        // given
        FarmSector sector = farm.put(aFarmSector().inactive().build());

        // when
        Result<DailyReportId> result = handler.handle(opening(sector, "2026-09-25"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_INACTIVE");
    }

    @Test
    @DisplayName("fails as conflict in a sector without active cages")
    void givenSectorWithoutActiveCages_whenOpening_thenFailAsWithoutCages() {
        // given
        FarmSector sector = farm.put(aFarmSector().withoutCages().build());

        // when
        Result<DailyReportId> result = handler.handle(opening(sector, "2026-09-25"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_WITHOUT_ACTIVE_CAGES");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11", "galpao-9"})
    @DisplayName("fails as not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenOpening_thenFailAsSectorNotFound(String sectorId) {
        // given
        OpenDailyReportCommand command =
                new OpenDailyReportCommand(sectorId, "2026-09-25", "06:42", "96", "20", null, MARINA);

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @Test
    @DisplayName("takes today from the farm: at 23:30 of the 24th there, the 25th is still in the future")
    void givenLateEveningAtTheFarm_whenOpeningTheNextDayInUtc_thenRefuseAsFuture() {
        // given
        FixedClock lateEvening = FixedClock.at("2026-09-25T02:30:00Z");
        OpenDailyReportCommandHandler atTheFarm = new OpenDailyReportCommandHandler(
                repository, farm, new FarmCalendar(lateEvening, ZoneId.of("America/Sao_Paulo")), lateEvening);
        FarmSector sector = farm.put(aFarmSector().build());

        // when
        Result<DailyReportId> result = atTheFarm.handle(opening(sector, "2026-09-25"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details()).containsEntry("collectionDate", "A data da coleta não pode ser futura.");
    }
}
