package io.github.ovyx.production.domain.model;

import static io.github.ovyx.production.domain.model.CatalogFormulas.POSTURA_PLUS;
import static io.github.ovyx.production.domain.model.CatalogFormulas.POSTURA_PLUS_ID;
import static io.github.ovyx.production.domain.model.CatalogFormulas.RECRIA;
import static io.github.ovyx.production.domain.model.CatalogFormulas.RECRIA_INACTIVE;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.aDailyReport;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.A01;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.B07;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.aFarmSector;
import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalCodeOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.production.domain.valueobject.FeedEntry;
import io.github.ovyx.shared.domain.FixedClock;
import io.github.ovyx.shared.domain.Violation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A ração de uma gaiola, lançada ou corrigida (US3 da 004; FR-010, FR-011, FR-020; R-005; invariante 10):
 * mantida a fórmula, o preço e o esperado guardados continuam; trocada, valem os atuais da nova, que
 * precisa estar ativa.
 */
@DisplayName("DailyReport feed of a cage")
class DailyReportFeedTest {

    private final FixedClock clock = FixedClock.at("2026-09-24T11:40:18Z");
    private final FarmSector sector = aFarmSector().build();
    private final DailyReport report = aDailyReport().in(sector).build();

    private static FeedFormulaChoice chosen(CatalogFormula formula) {
        return FeedFormulaChoice.of(formula);
    }

    /** A B-07 com a Postura Plus lançada pela sugestão, a R$ 2,85 e 28 g. */
    private void feedTheSectorWithPosturaPlus() {
        report.recordFeedBySuggestion(sector, chosen(POSTURA_PLUS), JOAO, clock.instant());
    }

    @Test
    @DisplayName("records the formula and the consumption of a cage, with the price and the intake of the formula")
    void givenPendingCage_whenRecordingItsFeed_thenKeepTheFormulaPriceAndIntake() {
        // given
        Instant now = clock.instant();

        // when
        report.recordFeed(sector, B07, chosen(POSTURA_PLUS), "1250", JOAO, now);

        // then
        assertThat(report.cage(B07).flatMap(ReportCage::feed))
                .contains(new FeedEntry(POSTURA_PLUS_ID, new BigDecimal("2.85"), 28, 1250));
        assertThat(report.cage(A01).flatMap(ReportCage::feed)).isEmpty();
        assertThat(report.lastCorrectedBy()).contains(JOAO);
        assertThat(report.lastCorrectedAt()).contains(now);
    }

    @Test
    @DisplayName("keeps the price and the intake kept when only the consumption changes, even with the formula now inactive and repriced")
    void givenFormulaRepricedAndInactivated_whenCorrectingOnlyTheConsumption_thenKeepThePriceAndIntakeKept() {
        // given
        feedTheSectorWithPosturaPlus();
        CatalogFormula nowInactiveAndRepriced =
                new CatalogFormula(POSTURA_PLUS_ID, "Postura Plus", false, new BigDecimal("3.10"), 30);

        // when
        report.recordFeed(sector, B07, chosen(nowInactiveAndRepriced), "1250", JOAO, clock.instant());

        // then
        assertThat(report.cage(B07).flatMap(ReportCage::feed))
                .contains(new FeedEntry(POSTURA_PLUS_ID, new BigDecimal("2.85"), 28, 1250));
    }

    @Test
    @DisplayName("takes the current price and intake of a new formula")
    void givenCageFedWithOneFormula_whenChangingToAnother_thenKeepTheCurrentPriceAndIntakeOfTheNewOne() {
        // given
        feedTheSectorWithPosturaPlus();

        // when
        report.recordFeed(sector, B07, chosen(RECRIA), "1200", JOAO, clock.instant());

        // then
        assertThat(report.cage(B07).flatMap(ReportCage::feed))
                .contains(new FeedEntry(RECRIA.id(), new BigDecimal("3.10"), 24, 1200));
    }

    @Test
    @DisplayName("refuses a new formula that is inactive, and keeps the feed as it was")
    void givenCageFedWithOneFormula_whenChangingToAnInactiveOne_thenRefuseItAndKeepTheFeed() {
        // given
        feedTheSectorWithPosturaPlus();
        FeedEntry before = report.cage(B07).flatMap(ReportCage::feed).orElseThrow();

        // when
        Map<String, String> details = detailsOf(
                () -> report.recordFeed(sector, B07, chosen(RECRIA_INACTIVE), "1200", JOAO, clock.instant()));

        // then
        assertThat(details)
                .containsExactly(Map.entry("formulaId", "A fórmula Recria está inativa. Escolha uma fórmula ativa."));
        assertThat(report.cage(B07).flatMap(ReportCage::feed)).contains(before);
    }

    @Test
    @DisplayName("refuses every failure at once, the formula before the consumption")
    void givenInactiveFormulaAndConsumptionThatIsNotAnInteger_whenRecording_thenRefuseBothAtOnce() {
        // given
        String consumption = "12,5";

        // when
        List<Violation> violations = violationsOf(
                () -> report.recordFeed(sector, B07, chosen(RECRIA_INACTIVE), consumption, JOAO, clock.instant()));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(
                        tuple("formulaId", ProductionErrorCode.FORMULA_INACTIVE),
                        tuple("consumption", ProductionErrorCode.CONSUMPTION_NOT_INTEGER));
        assertThat(report.cage(B07).flatMap(ReportCage::feed)).isEmpty();
        assertThat(report.lastCorrectedAt()).isEmpty();
    }

    @Test
    @DisplayName("refuses a missing formula together with a consumption above the maximum")
    void givenNoFormulaAndConsumptionAboveTheMaximum_whenRecording_thenRefuseBoth() {
        // given
        FeedFormulaChoice none = FeedFormulaChoice.absent();

        // when
        Map<String, String> details =
                detailsOf(() -> report.recordFeed(sector, B07, none, "60000", JOAO, clock.instant()));

        // then
        assertThat(details)
                .containsExactly(
                        Map.entry("formulaId", "Escolha a fórmula."),
                        Map.entry("consumption", "O consumo deve ficar entre 0 e 50.000 gramas."));
    }

    @Test
    @DisplayName("refuses a cage that is not in the report")
    void givenCageOutOfTheReport_whenRecordingItsFeed_thenRefuseAsCageNotFound() {
        // given
        CageId outside = CageId.of(UUID.randomUUID());

        // when
        ProductionErrorCode code = (ProductionErrorCode) refusalCodeOf(
                () -> report.recordFeed(sector, outside, chosen(POSTURA_PLUS), "1250", JOAO, clock.instant()));

        // then
        assertThat(code).isEqualTo(ProductionErrorCode.CAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("refuses the feed of a cage in a report of an inactive sector")
    void givenInactiveSector_whenRecordingTheFeedOfACage_thenRefuseAsSectorInactive() {
        // given
        FarmSector inactive = aFarmSector().withId(sector.id()).inactive().build();

        // when
        ProductionErrorCode code = (ProductionErrorCode) refusalCodeOf(
                () -> report.recordFeed(inactive, B07, chosen(POSTURA_PLUS), "1250", JOAO, clock.instant()));

        // then
        assertThat(code).isEqualTo(ProductionErrorCode.SECTOR_INACTIVE);
        assertThat(report.cage(B07).flatMap(ReportCage::feed)).isEmpty();
    }
}
