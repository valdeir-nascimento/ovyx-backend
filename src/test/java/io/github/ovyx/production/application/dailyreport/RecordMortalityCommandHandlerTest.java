package io.github.ovyx.production.application.dailyreport;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.aDailyReport;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.B07;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.aFarmSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.production.domain.model.DailyReport;
import io.github.ovyx.production.domain.model.FarmSector;
import io.github.ovyx.production.domain.model.ReportCage;
import io.github.ovyx.production.domain.valueobject.MortalityEntry;
import io.github.ovyx.production.domain.valueobject.MortalityNote;
import io.github.ovyx.production.fixtures.InMemoryDailyReportRepository;
import io.github.ovyx.production.fixtures.InMemoryFarmStructure;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Lançamento da mortalidade de uma gaiola do relatório (US3; FR-011 a FR-015, FR-020). */
@DisplayName("RecordMortalityCommandHandler")
class RecordMortalityCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-24T11:05:12Z");
    private final InMemoryDailyReportRepository repository = new InMemoryDailyReportRepository();
    private final InMemoryFarmStructure farm = new InMemoryFarmStructure();
    private final RecordMortalityCommandHandler handler = new RecordMortalityCommandHandler(repository, farm, clock);

    private final FarmSector sector = farm.put(aFarmSector().build());
    private final DailyReport report = stored(aDailyReport().in(sector).build());

    private DailyReport stored(DailyReport opened) {
        repository.save(opened);
        return opened;
    }

    private RecordMortalityCommand mortality(String sectorId, String reportId, String cageId, String deaths) {
        return new RecordMortalityCommand(sectorId, reportId, cageId, deaths, "1", "Prostração.", JOAO);
    }

    private RecordMortalityCommand mortalityOfB07(String deaths) {
        return mortality(sector.id().toString(), report.id().toString(), B07.toString(), deaths);
    }

    @Test
    @DisplayName("records the mortality in the cage, with the actor and the time of the command, and saves it")
    void givenValidMortality_whenRecording_thenSaveItWithTheActor() {
        // given
        RecordMortalityCommand command = mortalityOfB07("1");

        // when
        Result<CageId> result = handler.handle(command);

        // then
        assertThat(result.value()).isEqualTo(B07);
        DailyReport saved = repository.findById(sector.id(), report.id()).orElseThrow();
        assertThat(saved.cage(B07).flatMap(ReportCage::mortality))
                .contains(new MortalityEntry(1, 1, new MortalityNote("Prostração.")));
        assertThat(saved.lastCorrectedBy()).contains(JOAO);
        assertThat(saved.lastCorrectedAt()).contains(clock.instant());
        assertThat(repository.saves()).isEqualTo(2);
    }

    @Test
    @DisplayName("fails as validation with every invalid field, and saves nothing")
    void givenInvalidFields_whenRecording_thenFailAsValidationAndSaveNothing() {
        // given
        RecordMortalityCommand command = new RecordMortalityCommand(
                sector.id().toString(), report.id().toString(), B07.toString(), "1,5", "-1", "b".repeat(501), JOAO);

        // when
        Result<CageId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details()).containsOnlyKeys("deaths", "culls", "note");
        assertThat(repository.saves()).isEqualTo(1);
    }

    @Test
    @DisplayName("fails as validation when the removals pass the birds of the cage")
    void givenRemovalsAboveTheBirdsOfTheCage_whenRecording_thenFailAsValidation() {
        // given
        RecordMortalityCommand command = mortalityOfB07("50");

        // when
        Result<CageId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details())
                .containsEntry("deaths", "A gaiola tem 50 aves; mortes e descartes somam 51.");
        assertThat(repository.saves()).isEqualTo(1);
    }

    @Test
    @DisplayName("fails as conflict in the report of an inactive sector, and saves nothing")
    void givenInactiveSector_whenRecording_thenFailAsSectorInactive() {
        // given
        farm.put(aFarmSector().withId(sector.id()).inactive().build());

        // when
        Result<CageId> result = handler.handle(mortalityOfB07("1"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_INACTIVE");
        assertThat(repository.saves()).isEqualTo(1);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11", "galpao-9"})
    @DisplayName("fails as sector not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenRecording_thenFailAsSectorNotFound(String sectorId) {
        // given
        RecordMortalityCommand command = mortality(sectorId, report.id().toString(), B07.toString(), "1");

        // when
        Result<CageId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55", "24-09"})
    @DisplayName("fails as report not found for a report that does not exist or a malformed identifier")
    void givenUnknownOrMalformedReport_whenRecording_thenFailAsReportNotFound(String reportId) {
        // given
        RecordMortalityCommand command = mortality(sector.id().toString(), reportId, B07.toString(), "1");

        // when
        Result<CageId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("DAILY_REPORT_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as report not found for a report of another sector")
    void givenReportOfAnotherSector_whenRecording_thenFailAsReportNotFound() {
        // given
        FarmSector other = farm.put(aFarmSector().named("Codornas — Galpão 5").build());

        // when
        Result<CageId> result = handler.handle(mortality(other.id().toString(), report.id().toString(), B07.toString(), "1"));

        // then
        assertThat(result.error().code()).isEqualTo("DAILY_REPORT_NOT_FOUND");
        assertThat(repository.saves()).isEqualTo(1);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"7c9e1a3b-5d7f-4a1c-8e3b-5d7f9a1c3e22", "B-07"})
    @DisplayName("fails as cage not found for a cage out of the report or a malformed identifier")
    void givenUnknownOrMalformedCage_whenRecording_thenFailAsCageNotFound(String cageId) {
        // given
        RecordMortalityCommand command = mortality(sector.id().toString(), report.id().toString(), cageId, "1");

        // when
        Result<CageId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("CAGE_NOT_FOUND");
        assertThat(repository.saves()).isEqualTo(1);
    }
}
