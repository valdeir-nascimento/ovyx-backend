package io.github.ovyx.production.application.dailyreport;

import static io.github.ovyx.production.domain.model.CatalogFormulas.POSTURA_PLUS;
import static io.github.ovyx.production.domain.model.CatalogFormulas.RECRIA_INACTIVE;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.aDailyReport;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.B07;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.aFarmSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.model.DailyReport;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.FarmSector;
import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.ReportCage;
import io.github.ovyx.production.domain.valueobject.FeedEntry;
import io.github.ovyx.production.fixtures.InMemoryDailyReportRepository;
import io.github.ovyx.production.fixtures.InMemoryFarmStructure;
import io.github.ovyx.production.fixtures.InMemoryFeedCatalog;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Lançamento da ração do setor pela sugestão (US2 da 004; FR-009, FR-020, FR-022). */
@DisplayName("RecordFeedBySuggestionCommandHandler")
class RecordFeedBySuggestionCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-24T11:20:05Z");
    private final InMemoryDailyReportRepository repository = new InMemoryDailyReportRepository();
    private final InMemoryFarmStructure farm = new InMemoryFarmStructure();
    private final InMemoryFeedCatalog catalog = new InMemoryFeedCatalog();
    private final RecordFeedBySuggestionCommandHandler handler =
            new RecordFeedBySuggestionCommandHandler(repository, farm, catalog, clock);

    private final FarmSector sector = farm.put(aFarmSector().build());
    private final DailyReport report = stored(aDailyReport().in(sector).build());

    RecordFeedBySuggestionCommandHandlerTest() {
        catalog.put(POSTURA_PLUS);
        catalog.put(RECRIA_INACTIVE);
    }

    private DailyReport stored(DailyReport opened) {
        repository.save(opened);
        return opened;
    }

    private RecordFeedBySuggestionCommand suggestion(String formulaId) {
        return new RecordFeedBySuggestionCommand(sector.id().toString(), report.id().toString(), formulaId, JOAO);
    }

    @Test
    @DisplayName("feeds every pending cage with the proposal, with the actor and the time of the command, and saves")
    void givenActiveFormula_whenRecordingBySuggestion_thenSaveTheFeedOfEveryPendingCage() {
        // given
        RecordFeedBySuggestionCommand command = suggestion(POSTURA_PLUS.id().toString());

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.value()).isEqualTo(report.id());
        DailyReport saved = repository.findById(sector.id(), report.id()).orElseThrow();
        assertThat(saved.feedStatus()).isEqualTo(FeedStatus.COMPLETE);
        assertThat(saved.cage(B07).flatMap(ReportCage::feed).map(FeedEntry::consumption)).contains(1400);
        assertThat(saved.lastCorrectedBy()).contains(JOAO);
        assertThat(saved.lastCorrectedAt()).contains(clock.instant());
        assertThat(repository.saves()).isEqualTo(2);
    }

    @Test
    @DisplayName("succeeds without saving when no cage is pending")
    void givenEveryCageFed_whenRecordingBySuggestionAgain_thenSucceedWithoutSaving() {
        // given
        handler.handle(suggestion(POSTURA_PLUS.id().toString()));

        // when
        Result<DailyReportId> result = handler.handle(suggestion(POSTURA_PLUS.id().toString()));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(repository.saves()).isEqualTo(2);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("fails as validation, in the formula field, without a formula")
    void givenNoFormula_whenRecordingBySuggestion_thenFailAsValidationInTheFormulaField(String formulaId) {
        // given
        RecordFeedBySuggestionCommand command = suggestion(formulaId);

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.error().details()).containsExactly(Map.entry("formulaId", "Escolha a fórmula."));
        assertThat(repository.saves()).isEqualTo(1);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c99", "postura-plus"})
    @DisplayName("fails as validation, in the formula field, with an unknown or malformed formula")
    void givenUnknownOrMalformedFormula_whenRecordingBySuggestion_thenFailAsFormulaNotFound(String formulaId) {
        // given
        RecordFeedBySuggestionCommand command = suggestion(formulaId);

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details()).containsExactly(Map.entry("formulaId", "Fórmula não encontrada."));
    }

    @Test
    @DisplayName("fails as validation with an inactive formula")
    void givenInactiveFormula_whenRecordingBySuggestion_thenFailAsValidation() {
        // given
        RecordFeedBySuggestionCommand command = suggestion(RECRIA_INACTIVE.id().toString());

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details())
                .containsExactly(Map.entry("formulaId", "A fórmula Recria está inativa. Escolha uma fórmula ativa."));
        assertThat(repository.saves()).isEqualTo(1);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e99", "galpao-4"})
    @DisplayName("fails as not found for an unknown or malformed sector")
    void givenUnknownOrMalformedSector_whenRecordingBySuggestion_thenFailAsSectorNotFound(String sectorId) {
        // given
        RecordFeedBySuggestionCommand command =
                new RecordFeedBySuggestionCommand(sectorId, report.id().toString(), POSTURA_PLUS.id().toString(), JOAO);

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d99", "24-09"})
    @DisplayName("fails as not found for an unknown or malformed report")
    void givenUnknownOrMalformedReport_whenRecordingBySuggestion_thenFailAsReportNotFound(String reportId) {
        // given
        RecordFeedBySuggestionCommand command =
                new RecordFeedBySuggestionCommand(sector.id().toString(), reportId, POSTURA_PLUS.id().toString(), JOAO);

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("DAILY_REPORT_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as not found for the report of another sector")
    void givenReportOfAnotherSector_whenRecordingBySuggestion_thenFailAsReportNotFound() {
        // given
        FarmSector other = farm.put(aFarmSector().named("Codornas — Galpão 5").build());
        RecordFeedBySuggestionCommand command = new RecordFeedBySuggestionCommand(
                other.id().toString(), report.id().toString(), POSTURA_PLUS.id().toString(), JOAO);

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.error().code()).isEqualTo("DAILY_REPORT_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as conflict in a report of an inactive sector")
    void givenInactiveSector_whenRecordingBySuggestion_thenFailAsConflict() {
        // given
        farm.put(aFarmSector().withId(sector.id()).inactive().build());

        // when
        Result<DailyReportId> result = handler.handle(suggestion(POSTURA_PLUS.id().toString()));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_INACTIVE");
        assertThat(repository.saves()).isEqualTo(1);
    }

    @Test
    @DisplayName("does not reach the report for a sector that does not exist")
    void givenSectorOfNobody_whenRecordingBySuggestion_thenNeverAskForTheReport() {
        // given
        RecordFeedBySuggestionCommand command = new RecordFeedBySuggestionCommand(
                UUID.randomUUID().toString(), report.id().toString(), POSTURA_PLUS.id().toString(), JOAO);

        // when
        Result<DailyReportId> result = handler.handle(command);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(repository.saves()).isEqualTo(1);
    }
}
