package io.github.ovyx.production.application.dailyreport;

import static io.github.ovyx.production.domain.model.CatalogFormulas.POSTURA_PLUS;
import static io.github.ovyx.production.domain.model.CatalogFormulas.RECRIA_INACTIVE;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.aDailyReport;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.B07;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.aFarmSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.production.domain.model.DailyReport;
import io.github.ovyx.production.domain.model.FarmSector;
import io.github.ovyx.production.domain.model.ReportCage;
import io.github.ovyx.production.domain.valueobject.FeedEntry;
import io.github.ovyx.production.fixtures.InMemoryDailyReportRepository;
import io.github.ovyx.production.fixtures.InMemoryFarmStructure;
import io.github.ovyx.production.fixtures.InMemoryFeedCatalog;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Lançamento e correção da ração de uma gaiola do relatório (US3 da 004; FR-010, FR-011, FR-020). */
@DisplayName("RecordFeedCommandHandler")
class RecordFeedCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-24T11:40:18Z");
    private final InMemoryDailyReportRepository repository = new InMemoryDailyReportRepository();
    private final InMemoryFarmStructure farm = new InMemoryFarmStructure();
    private final InMemoryFeedCatalog catalog = new InMemoryFeedCatalog();
    private final RecordFeedCommandHandler handler = new RecordFeedCommandHandler(repository, farm, catalog, clock);

    private final FarmSector sector = farm.put(aFarmSector().build());
    private final DailyReport report = stored(aDailyReport().in(sector).build());

    RecordFeedCommandHandlerTest() {
        catalog.put(POSTURA_PLUS);
        catalog.put(RECRIA_INACTIVE);
    }

    private DailyReport stored(DailyReport opened) {
        repository.save(opened);
        return opened;
    }

    private RecordFeedCommand feedOfB07(String formulaId, String consumption) {
        return new RecordFeedCommand(
                sector.id().toString(), report.id().toString(), B07.toString(), formulaId, consumption, JOAO);
    }

    @Test
    @DisplayName("records the feed of the cage, with the actor and the time of the command, and saves it")
    void givenValidFeed_whenRecording_thenSaveItWithTheActor() {
        // given
        RecordFeedCommand command = feedOfB07(POSTURA_PLUS.id().toString(), "1250");

        // when
        Result<CageId> result = handler.handle(command);

        // then
        assertThat(result.value()).isEqualTo(B07);
        DailyReport saved = repository.findById(sector.id(), report.id()).orElseThrow();
        assertThat(saved.cage(B07).flatMap(ReportCage::feed))
                .contains(new FeedEntry(POSTURA_PLUS.id(), new BigDecimal("2.85"), 28, 1250));
        assertThat(saved.lastCorrectedBy()).contains(JOAO);
        assertThat(saved.lastCorrectedAt()).contains(clock.instant());
        assertThat(repository.saves()).isEqualTo(2);
    }

    @Test
    @DisplayName("fails as validation with the unknown formula and the broken consumption at once, and saves nothing")
    void givenUnknownFormulaAndBrokenConsumption_whenRecording_thenFailAsValidationWithBothFields() {
        // given
        RecordFeedCommand command = feedOfB07(UUID.randomUUID().toString(), "12,5");

        // when
        Result<CageId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.error().details())
                .containsExactly(
                        Map.entry("formulaId", "Fórmula não encontrada."),
                        Map.entry("consumption", "O consumo deve ser um número inteiro de gramas."));
        assertThat(repository.saves()).isEqualTo(1);
    }

    @Test
    @DisplayName("fails as validation with an inactive formula")
    void givenInactiveFormula_whenRecording_thenFailAsValidation() {
        // given
        RecordFeedCommand command = feedOfB07(RECRIA_INACTIVE.id().toString(), "1200");

        // when
        Result<CageId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details()).containsOnlyKeys("formulaId");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a99", "b-07"})
    @DisplayName("fails as not found for a cage that is not in the report, or a malformed one")
    void givenCageOutOfTheReport_whenRecording_thenFailAsCageNotFound(String cageId) {
        // given
        RecordFeedCommand command = new RecordFeedCommand(
                sector.id().toString(), report.id().toString(), cageId, POSTURA_PLUS.id().toString(), "1250", JOAO);

        // when
        Result<CageId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("CAGE_NOT_FOUND");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d99", "24-09"})
    @DisplayName("fails as not found for a report that does not exist, or a malformed one")
    void givenUnknownReport_whenRecording_thenFailAsReportNotFound(String reportId) {
        // given
        RecordFeedCommand command = new RecordFeedCommand(
                sector.id().toString(), reportId, B07.toString(), POSTURA_PLUS.id().toString(), "1250", JOAO);

        // when
        Result<CageId> result = handler.handle(command);

        // then
        assertThat(result.error().code()).isEqualTo("DAILY_REPORT_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as not found for a sector that does not exist")
    void givenUnknownSector_whenRecording_thenFailAsSectorNotFound() {
        // given
        RecordFeedCommand command = new RecordFeedCommand(
                UUID.randomUUID().toString(),
                report.id().toString(),
                B07.toString(),
                POSTURA_PLUS.id().toString(),
                "1250",
                JOAO);

        // when
        Result<CageId> result = handler.handle(command);

        // then
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as conflict in a report of an inactive sector")
    void givenInactiveSector_whenRecording_thenFailAsConflict() {
        // given
        farm.put(aFarmSector().withId(sector.id()).inactive().build());

        // when
        Result<CageId> result = handler.handle(feedOfB07(POSTURA_PLUS.id().toString(), "1250"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_INACTIVE");
        assertThat(repository.saves()).isEqualTo(1);
    }
}
