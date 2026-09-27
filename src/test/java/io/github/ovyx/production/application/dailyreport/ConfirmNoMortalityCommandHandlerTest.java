package io.github.ovyx.production.application.dailyreport;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.aDailyReport;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.B07;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.aFarmSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.model.DailyReport;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.FarmSector;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.fixtures.InMemoryDailyReportRepository;
import io.github.ovyx.production.fixtures.InMemoryFarmStructure;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Confirmação do dia sem ocorrência (US3; FR-013, FR-020; invariante 6). */
@DisplayName("ConfirmNoMortalityCommandHandler")
class ConfirmNoMortalityCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-24T11:05:12Z");
    private final InMemoryDailyReportRepository repository = new InMemoryDailyReportRepository();
    private final InMemoryFarmStructure farm = new InMemoryFarmStructure();
    private final ConfirmNoMortalityCommandHandler handler = new ConfirmNoMortalityCommandHandler(repository, farm, clock);

    private final FarmSector sector = farm.put(aFarmSector().build());
    private final DailyReport report = stored(aDailyReport().in(sector).build());

    private DailyReport stored(DailyReport opened) {
        repository.save(opened);
        return opened;
    }

    private ConfirmNoMortalityCommand confirmation() {
        return new ConfirmNoMortalityCommand(sector.id().toString(), report.id().toString(), JOAO);
    }

    @Test
    @DisplayName("confirms the day without occurrence, with the actor of the command, and saves it")
    void givenReportWithoutOccurrence_whenConfirming_thenSaveTheConfirmation() {
        // given
        ConfirmNoMortalityCommand command = confirmation();

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.value()).isEqualTo(report.id());
        DailyReport saved = repository.findById(sector.id(), report.id()).orElseThrow();
        assertThat(saved.noMortalityConfirmed()).isTrue();
        assertThat(saved.mortalityStatus()).isEqualTo(MortalityStatus.RECORDED);
        assertThat(saved.lastCorrectedBy()).contains(JOAO);
        assertThat(repository.saves()).isEqualTo(2);
    }

    @Test
    @DisplayName("succeeds without saving again when the day is already confirmed")
    void givenConfirmedReport_whenConfirmingAgain_thenSucceedWithoutSaving() {
        // given
        handler.handle(confirmation());

        // when
        Result<DailyReportId> result = handler.handle(confirmation());

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(repository.saves()).isEqualTo(2);
    }

    @Test
    @DisplayName("fails as conflict when a death or a cull is recorded, and saves nothing")
    void givenOccurrenceRecorded_whenConfirming_thenFailAsMortalityAlreadyRecorded() {
        // given
        report.recordMortality(sector, B07, "1", "0", null, JOAO, clock.instant());

        // when
        Result<DailyReportId> result = handler.handle(confirmation());

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("MORTALITY_ALREADY_RECORDED");
        assertThat(result.error().message()).isEqualTo("O relatório já tem mortes ou descartes lançados.");
        assertThat(repository.saves()).isEqualTo(1);
    }

    @Test
    @DisplayName("fails as conflict in the report of an inactive sector")
    void givenInactiveSector_whenConfirming_thenFailAsSectorInactive() {
        // given
        farm.put(aFarmSector().withId(sector.id()).inactive().build());

        // when
        Result<DailyReportId> result = handler.handle(confirmation());

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_INACTIVE");
        assertThat(repository.saves()).isEqualTo(1);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11", "galpao-9"})
    @DisplayName("fails as sector not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenConfirming_thenFailAsSectorNotFound(String sectorId) {
        // given
        ConfirmNoMortalityCommand command = new ConfirmNoMortalityCommand(sectorId, report.id().toString(), JOAO);

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55", "24-09"})
    @DisplayName("fails as report not found for a report that does not exist or a malformed identifier")
    void givenUnknownOrMalformedReport_whenConfirming_thenFailAsReportNotFound(String reportId) {
        // given
        ConfirmNoMortalityCommand command = new ConfirmNoMortalityCommand(sector.id().toString(), reportId, JOAO);

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("DAILY_REPORT_NOT_FOUND");
    }
}
