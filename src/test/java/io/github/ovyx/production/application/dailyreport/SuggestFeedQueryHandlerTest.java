package io.github.ovyx.production.application.dailyreport;

import static io.github.ovyx.production.domain.model.CatalogFormulas.POSTURA_PLUS;
import static io.github.ovyx.production.domain.model.CatalogFormulas.RECRIA;
import static io.github.ovyx.production.domain.model.CatalogFormulas.RECRIA_INACTIVE;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.production.domain.model.CatalogFormula;
import io.github.ovyx.production.domain.model.FeedFormulaId;
import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.fixtures.InMemoryFeedCatalog;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A proposta da ração do setor, antes de gravar (US2 da 004; FR-009; R-006): o consumo de cada gaiola sem
 * ração e os totais do dia se a proposta fosse gravada.
 */
@DisplayName("SuggestFeedQueryHandler")
class SuggestFeedQueryHandlerTest {

    private static final UUID A01 = UUID.fromString("2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66");
    private static final UUID B07 = UUID.fromString("9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44");

    private final RecordingDailyReportDirectory directory = new RecordingDailyReportDirectory();
    private final InMemoryFeedCatalog catalog = new InMemoryFeedCatalog();
    private final SuggestFeedQueryHandler handler = new SuggestFeedQueryHandler(directory, catalog);
    private final UUID sectorId = UUID.randomUUID();
    private final UUID reportId = UUID.randomUUID();

    SuggestFeedQueryHandlerTest() {
        catalog.put(POSTURA_PLUS);
        catalog.put(RECRIA_INACTIVE);
    }

    private static ReportCageDetail cage(UUID id, String code, int birds, int eggs, CageFeed feed) {
        return new ReportCageDetail(
                id, code, code.substring(0, 1), Integer.parseInt(code.substring(2)), birds,
                new CageProduction(eggs, 0, 0, 0, 0, 0, 0), null, feed);
    }

    /** O relatório de 24/09 do setor, com as gaiolas dadas e 89 ovos. */
    private void knowReport(String sectorStatus, ReportCageDetail... cages) {
        ReportingSector sector = new ReportingSector(sectorId, "Codornas — Galpão 4", sectorStatus);
        directory.knowSector(sector);
        List<ReportCageDetail> all = List.of(cages);
        directory.knowDetail(new DailyReportDetail(
                reportId,
                sector,
                LocalDate.of(2026, 9, 24),
                LocalTime.of(6, 30),
                98,
                20,
                null,
                false,
                new Actor(UUID.randomUUID(), "Marina Alves"),
                Instant.parse("2026-09-24T09:31:40Z"),
                null,
                null,
                DailyReportTotals.production(98, all),
                new MortalityTotals(MortalityStatus.PENDING, 0, 0, BigDecimal.ZERO, 98),
                DailyReportTotals.feed(all, 89),
                all));
    }

    private SuggestFeedQuery query(String formulaId) {
        return new SuggestFeedQuery(sectorId.toString(), reportId.toString(), formulaId);
    }

    @Test
    @DisplayName("proposes the birds times the expected intake for each cage without feed, with the totals of the day")
    void givenReportWithoutFeed_whenSuggesting_thenProposeEveryCageAndTheTotals() {
        // given
        knowReport("ACTIVE", cage(A01, "A-01", 48, 44, null), cage(B07, "B-07", 50, 45, null));

        // when
        Result<FeedSuggestion> result = handler.handle(query(POSTURA_PLUS.id().toString()));

        // then
        FeedSuggestion suggestion = result.value();
        assertThat(suggestion.formula())
                .isEqualTo(new SuggestedFormula(POSTURA_PLUS.id().value(), "Postura Plus", new BigDecimal("2.85"), 28));
        assertThat(suggestion.cages())
                .containsExactly(
                        new SuggestedCageFeed(A01, "A-01", 48, 1344, new BigDecimal("3.83")),
                        new SuggestedCageFeed(B07, "B-07", 50, 1400, new BigDecimal("3.99")));
        FeedTotals totals = suggestion.totals();
        assertThat(totals.status()).isEqualTo(FeedStatus.COMPLETE);
        assertThat(totals.pendingCages()).isZero();
        assertThat(totals.consumption()).isEqualTo(2744);
        assertThat(totals.cost()).isEqualByComparingTo("7.82");
        assertThat(totals.costPerEgg()).isEqualByComparingTo("0.088");
        assertThat(totals.intakePerBird()).isEqualByComparingTo("28.0");
        assertThat(totals.expectedIntakePerBird()).isEqualByComparingTo("28.0");
    }

    @Test
    @DisplayName("proposes only the cages without feed, and keeps the fed ones in the totals")
    void givenOneCageFed_whenSuggesting_thenProposeOnlyThePendingOne() {
        // given
        CageFeed a01 = CageFeed.of(RECRIA.id().value(), "Recria", new BigDecimal("3.10"), 24, 1100, 48);
        knowReport("ACTIVE", cage(A01, "A-01", 48, 44, a01), cage(B07, "B-07", 50, 45, null));

        // when
        Result<FeedSuggestion> result = handler.handle(query(POSTURA_PLUS.id().toString()));

        // then
        assertThat(result.value().cages()).extracting(SuggestedCageFeed::code).containsExactly("B-07");
        assertThat(result.value().totals().consumption()).isEqualTo(2500);
        assertThat(result.value().totals().cost()).isEqualByComparingTo("7.40");
    }

    @Test
    @DisplayName("proposes nothing when every cage is fed")
    void givenEveryCageFed_whenSuggesting_thenProposeNothing() {
        // given
        CageFeed fed = CageFeed.of(POSTURA_PLUS.id().value(), "Postura Plus", new BigDecimal("2.85"), 28, 1400, 50);
        knowReport("ACTIVE", cage(B07, "B-07", 50, 45, fed));

        // when
        Result<FeedSuggestion> result = handler.handle(query(POSTURA_PLUS.id().toString()));

        // then
        assertThat(result.value().cages()).isEmpty();
        assertThat(result.value().totals().status()).isEqualTo(FeedStatus.COMPLETE);
    }

    @Test
    @DisplayName("proposes also in a report of an inactive sector: it is a reading, and the writing is what refuses")
    void givenInactiveSector_whenSuggesting_thenStillPropose() {
        // given
        knowReport("INACTIVE", cage(B07, "B-07", 50, 45, null));

        // when
        Result<FeedSuggestion> result = handler.handle(query(POSTURA_PLUS.id().toString()));

        // then
        assertThat(result.value().cages()).hasSize(1);
    }

    @Test
    @DisplayName("fails as validation, in the formula field, with an inactive formula")
    void givenInactiveFormula_whenSuggesting_thenFailAsValidation() {
        // given
        knowReport("ACTIVE", cage(B07, "B-07", 50, 45, null));

        // when
        Result<FeedSuggestion> result = handler.handle(query(RECRIA_INACTIVE.id().toString()));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details())
                .containsExactly(Map.entry("formulaId", "A fórmula Recria está inativa. Escolha uma fórmula ativa."));
    }

    @Test
    @DisplayName("fails as validation, in the formula field, when the proposal of a cage would pass 50,000 g")
    void givenProposalAboveTheMaximum_whenSuggesting_thenFailAsValidationNamingTheCage() {
        // given
        CatalogFormula heavy = new CatalogFormula(
                FeedFormulaId.of(UUID.fromString("6c8e0a2c-4e6a-4c8e-8a0c-6e8a0c2e4a33")),
                "Engorda",
                true,
                new BigDecimal("2.50"),
                60);
        catalog.put(heavy);
        knowReport("ACTIVE", cage(A01, "A-01", 1000, 45, null), cage(B07, "B-07", 50, 45, null));

        // when
        Result<FeedSuggestion> result = handler.handle(query(heavy.id().toString()));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details())
                .containsExactly(Map.entry(
                        "formulaId", "A proposta da gaiola A-01 passaria de 50.000 g. Lance a ração dela pela própria gaiola."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"", "4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c99", "postura-plus"})
    @DisplayName("fails as validation, in the formula field, without a formula or with one that does not exist")
    void givenMissingOrUnknownFormula_whenSuggesting_thenFailAsValidationInTheFormulaField(String formulaId) {
        // given
        knowReport("ACTIVE", cage(B07, "B-07", 50, 45, null));

        // when
        Result<FeedSuggestion> result = handler.handle(query(formulaId));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details()).containsOnlyKeys("formulaId");
    }

    @Test
    @DisplayName("fails as not found for a sector that does not exist")
    void givenUnknownSector_whenSuggesting_thenFailAsSectorNotFound() {
        // given
        SuggestFeedQuery query =
                new SuggestFeedQuery(UUID.randomUUID().toString(), reportId.toString(), POSTURA_PLUS.id().toString());

        // when
        Result<FeedSuggestion> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d99", "24-09"})
    @DisplayName("fails as not found for a report that does not exist or a malformed one")
    void givenUnknownOrMalformedReport_whenSuggesting_thenFailAsReportNotFound(String report) {
        // given
        knowReport("ACTIVE", cage(B07, "B-07", 50, 45, null));
        SuggestFeedQuery query = new SuggestFeedQuery(sectorId.toString(), report, POSTURA_PLUS.id().toString());

        // when
        Result<FeedSuggestion> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("DAILY_REPORT_NOT_FOUND");
    }
}
