package io.github.ovyx.production.application.dailyreport;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.MARINA;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.aDailyReport;
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

/** Correção dos dados gerais do relatório (US4; FR-003, FR-018, FR-020). */
@DisplayName("CorrectDailyReportCommandHandler")
class CorrectDailyReportCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-25T11:05:12Z");
    private final InMemoryDailyReportRepository repository = new InMemoryDailyReportRepository();
    private final InMemoryFarmStructure farm = new InMemoryFarmStructure();
    private final CorrectDailyReportCommandHandler handler = new CorrectDailyReportCommandHandler(
            repository, farm, new FarmCalendar(clock, ZoneId.of("America/Sao_Paulo")), clock);

    private final FarmSector sector = farm.put(aFarmSector().build());
    private final DailyReport report = stored(aDailyReport().in(sector).withRoster(repository).build());

    private DailyReport stored(DailyReport opened) {
        repository.save(opened);
        return opened;
    }

    private CorrectDailyReportCommand correction(String sectorId, String reportId, String date, String age) {
        return new CorrectDailyReportCommand(sectorId, reportId, date, "06:45", "96", age, "Corrigido.", JOAO);
    }

    private CorrectDailyReportCommand correctionOfTheReport(String date, String age) {
        return correction(sector.id().toString(), report.id().toString(), date, age);
    }

    @Test
    @DisplayName("corrects the report with the actor and the time of the command, and saves it")
    void givenValidCorrection_whenCorrecting_thenSaveItWithTheActor() {
        // given
        CorrectDailyReportCommand command = correctionOfTheReport("2026-09-24", "21");

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.value()).isEqualTo(report.id());
        DailyReport saved = repository.findById(sector.id(), report.id()).orElseThrow();
        assertThat(saved.flockAge().value()).isEqualTo(21);
        assertThat(saved.openedBy()).isEqualTo(MARINA);
        assertThat(saved.lastCorrectedBy()).contains(JOAO);
        assertThat(saved.lastCorrectedAt()).contains(clock.instant());
        assertThat(repository.saves()).isEqualTo(2);
    }

    @Test
    @DisplayName("takes today from the farm: at 23:30 of the 24th there, the 25th is still in the future")
    void givenLateEveningAtTheFarm_whenCorrectingToTheNextDayInUtc_thenRefuseAsFuture() {
        // given
        FixedClock lateEvening = FixedClock.at("2026-09-25T02:30:00Z");
        CorrectDailyReportCommandHandler atTheFarm = new CorrectDailyReportCommandHandler(
                repository, farm, new FarmCalendar(lateEvening, ZoneId.of("America/Sao_Paulo")), lateEvening);

        // when
        Result<DailyReportId> result = atTheFarm.handle(correctionOfTheReport("2026-09-25", "20"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details()).containsOnlyKeys("collectionDate");
        assertThat(repository.saves()).isEqualTo(1);
    }

    @Test
    @DisplayName("fails as validation with every invalid field, and saves nothing")
    void givenInvalidFields_whenCorrecting_thenFailAsValidationAndSaveNothing() {
        // given
        CorrectDailyReportCommand command = new CorrectDailyReportCommand(
                sector.id().toString(), report.id().toString(), "2026-09-26", "25:00", "0", "200", null, JOAO);

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details())
                .containsOnlyKeys("collectionDate", "collectionTime", "openingBirdCount", "flockAge");
        assertThat(repository.saves()).isEqualTo(1);
    }

    @Test
    @DisplayName("fails as conflict with the date of another report of the sector")
    void givenDateOfAnotherReport_whenCorrecting_thenFailAsConflict() {
        // given
        stored(aDailyReport().in(sector).on("2026-09-23").withRoster(repository).build());

        // when
        Result<DailyReportId> result = handler.handle(correctionOfTheReport("2026-09-23", "20"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("DAILY_REPORT_ALREADY_EXISTS");
        assertThat(repository.saves()).isEqualTo(2);
    }

    @Test
    @DisplayName("fails as conflict in the report of an inactive sector")
    void givenInactiveSector_whenCorrecting_thenFailAsSectorInactive() {
        // given
        farm.put(aFarmSector().withId(sector.id()).inactive().build());

        // when
        Result<DailyReportId> result = handler.handle(correctionOfTheReport("2026-09-24", "21"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_INACTIVE");
        assertThat(repository.saves()).isEqualTo(1);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11", "galpao-9"})
    @DisplayName("fails as sector not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenCorrecting_thenFailAsSectorNotFound(String sectorId) {
        // given
        CorrectDailyReportCommand command = correction(sectorId, report.id().toString(), "2026-09-24", "21");

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55", "24-09"})
    @DisplayName("fails as report not found for a report that does not exist or a malformed identifier")
    void givenUnknownOrMalformedReport_whenCorrecting_thenFailAsReportNotFound(String reportId) {
        // given
        CorrectDailyReportCommand command = correction(sector.id().toString(), reportId, "2026-09-24", "21");

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("DAILY_REPORT_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as report not found for a report of another sector")
    void givenReportOfAnotherSector_whenCorrecting_thenFailAsReportNotFound() {
        // given
        FarmSector other = farm.put(aFarmSector().named("Codornas — Galpão 5").build());

        // when
        Result<DailyReportId> result =
                handler.handle(correction(other.id().toString(), report.id().toString(), "2026-09-24", "21"));

        // then
        assertThat(result.error().code()).isEqualTo("DAILY_REPORT_NOT_FOUND");
        assertThat(repository.saves()).isEqualTo(1);
    }
}
