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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A ração do setor pela sugestão (US2 da 004; FR-007, FR-009, FR-022; invariantes 3, 9 e 10 do
 * data-model.md): cada gaiola sem ração recebe as aves dela vezes o consumo esperado da fórmula.
 */
@DisplayName("DailyReport feed by suggestion")
class DailyReportFeedSuggestionTest {

    private final FixedClock clock = FixedClock.at("2026-09-24T11:20:05Z");
    private final FarmSector sector = aFarmSector().build();
    private final DailyReport report = aDailyReport().in(sector).build();

    private static FeedFormulaChoice chosen(CatalogFormula formula) {
        return FeedFormulaChoice.of(formula);
    }

    @Test
    @DisplayName("gives each cage without feed the formula and the birds times the expected intake")
    void givenReportWithoutFeed_whenRecordingBySuggestion_thenFeedEveryCageWithTheProposal() {
        // given
        Instant now = clock.instant();

        // when
        int recorded = report.recordFeedBySuggestion(sector, chosen(POSTURA_PLUS), JOAO, now);

        // then
        assertThat(recorded).isEqualTo(2);
        assertThat(report.cage(A01).flatMap(ReportCage::feed))
                .contains(new FeedEntry(POSTURA_PLUS_ID, new BigDecimal("2.85"), 28, 1344));
        assertThat(report.cage(B07).flatMap(ReportCage::feed))
                .contains(new FeedEntry(POSTURA_PLUS_ID, new BigDecimal("2.85"), 28, 1400));
        assertThat(report.lastCorrectedBy()).contains(JOAO);
        assertThat(report.lastCorrectedAt()).contains(now);
    }

    /** O relatório com a A-01 já lançada com a Recria, como se viesse do banco. */
    private DailyReport withA01FedWithRecria(FeedEntry a01) {
        return DailyReport.restore(
                report.id(),
                report.sectorId(),
                report.collectionDate(),
                report.collectionTime(),
                report.openingBirdCount(),
                report.flockAge(),
                null,
                false,
                List.of(
                        ReportCage.restore(A01, "A", 1, 48, null, null, a01),
                        ReportCage.restore(B07, "B", 7, 50, null, null, null)),
                report.openedBy(),
                report.openedAt(),
                null,
                null);
    }

    @Test
    @DisplayName("leaves the cages already fed as they were")
    void givenOneCageAlreadyFed_whenRecordingBySuggestion_thenFeedOnlyThePendingOne() {
        // given
        FeedEntry a01 = new FeedEntry(RECRIA.id(), new BigDecimal("3.10"), 24, 1100);
        DailyReport partlyFed = withA01FedWithRecria(a01);

        // when
        int recorded = partlyFed.recordFeedBySuggestion(sector, chosen(POSTURA_PLUS), JOAO, clock.instant());

        // then
        assertThat(recorded).isEqualTo(1);
        assertThat(partlyFed.cage(A01).flatMap(ReportCage::feed)).contains(a01);
        assertThat(partlyFed.cage(B07).flatMap(ReportCage::feed).map(FeedEntry::consumption)).contains(1400);
        assertThat(partlyFed.pendingFeedCages()).isZero();
    }

    @Test
    @DisplayName("changes nothing, and marks no correction, when every cage is already fed")
    void givenEveryCageFed_whenRecordingBySuggestionAgain_thenChangeNothing() {
        // given
        report.recordFeedBySuggestion(sector, chosen(POSTURA_PLUS), JOAO, clock.instant());
        Instant firstCorrection = report.lastCorrectedAt().orElseThrow();
        clock.advance(Duration.ofMinutes(5));

        // when
        int recorded = report.recordFeedBySuggestion(sector, chosen(RECRIA), JOAO, clock.instant());

        // then
        assertThat(recorded).isZero();
        assertThat(report.lastCorrectedAt()).contains(firstCorrection);
        assertThat(report.cage(B07).flatMap(ReportCage::feed).map(FeedEntry::formulaId)).contains(POSTURA_PLUS_ID);
    }

    @Test
    @DisplayName("feeds a cage without birds with 0 g")
    void givenCageWithoutBirds_whenRecordingBySuggestion_thenFeedItWithZero() {
        // given
        FarmSector withEmptyCage =
                aFarmSector().withCages(new FarmCage(A01, "A", 1, 0), new FarmCage(B07, "B", 7, 50)).build();
        DailyReport withEmpty = aDailyReport().in(withEmptyCage).build();

        // when
        withEmpty.recordFeedBySuggestion(withEmptyCage, chosen(POSTURA_PLUS), JOAO, clock.instant());

        // then
        assertThat(withEmpty.cage(A01).flatMap(ReportCage::feed).map(FeedEntry::consumption)).contains(0);
    }

    @Test
    @DisplayName("refuses an inactive formula in the field of the formula, and feeds no cage")
    void givenInactiveFormula_whenRecordingBySuggestion_thenRefuseItInTheFormulaField() {
        // given
        FeedFormulaChoice recria = chosen(RECRIA_INACTIVE);

        // when
        Map<String, String> details =
                detailsOf(() -> report.recordFeedBySuggestion(sector, recria, JOAO, clock.instant()));

        // then
        assertThat(details)
                .containsExactly(Map.entry("formulaId", "A fórmula Recria está inativa. Escolha uma fórmula ativa."));
        assertThat(violationsOf(() -> report.recordFeedBySuggestion(sector, recria, JOAO, clock.instant())))
                .extracting(Violation::code)
                .containsExactly(ProductionErrorCode.FORMULA_INACTIVE);
        assertThat(report.cages()).noneMatch(ReportCage::hasFeed);
        assertThat(report.lastCorrectedAt()).isEmpty();
    }

    @Test
    @DisplayName("refuses a formula that was not chosen, and one that does not exist, in the field of the formula")
    void givenNoFormulaAndUnknownFormula_whenRecordingBySuggestion_thenRefuseEachInTheFormulaField() {
        // given
        FeedFormulaChoice none = FeedFormulaChoice.absent();
        FeedFormulaChoice unknown = FeedFormulaChoice.unknown();

        // when
        Map<String, String> withoutFormula =
                detailsOf(() -> report.recordFeedBySuggestion(sector, none, JOAO, clock.instant()));
        Map<String, String> withUnknown =
                detailsOf(() -> report.recordFeedBySuggestion(sector, unknown, JOAO, clock.instant()));

        // then
        assertThat(withoutFormula).containsExactly(Map.entry("formulaId", "Escolha a fórmula."));
        assertThat(withUnknown).containsExactly(Map.entry("formulaId", "Fórmula não encontrada."));
        assertThat(violationsOf(() -> report.recordFeedBySuggestion(sector, unknown, JOAO, clock.instant())))
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("formulaId", ProductionErrorCode.FORMULA_NOT_FOUND));
    }

    @Test
    @DisplayName("refuses the suggestion when the proposal of a cage would pass 50,000 grams, naming the cage")
    void givenProposalAboveTheMaximum_whenRecordingBySuggestion_thenRefuseItAndFeedNoCage() {
        // given
        // 1.000 aves × 200 g = 200.000 g: as faixas das aves e do esperado permitem, e o consumo não
        // (decisão da implementação da 004, research.md).
        CatalogFormula heavy = new CatalogFormula(POSTURA_PLUS_ID, "Engorda", true, new BigDecimal("2.85"), 200);
        FarmSector large = aFarmSector().withCages(new FarmCage(A01, "A", 1, 1000)).build();
        DailyReport largeReport = aDailyReport().in(large).withOpeningBirds(1000).build();

        // when
        Map<String, String> details =
                detailsOf(() -> largeReport.recordFeedBySuggestion(large, chosen(heavy), JOAO, clock.instant()));

        // then
        assertThat(details)
                .containsExactly(Map.entry(
                        "formulaId",
                        "A proposta da gaiola A-01 passaria de 50.000 g. Lance a ração dela pela própria gaiola."));
        assertThat(violationsOf(() -> largeReport.recordFeedBySuggestion(large, chosen(heavy), JOAO, clock.instant())))
                .extracting(Violation::code)
                .containsExactly(ProductionErrorCode.CONSUMPTION_OUT_OF_RANGE);
        assertThat(largeReport.cages()).noneMatch(ReportCage::hasFeed);
    }

    @Test
    @DisplayName("refuses any feed in a report of an inactive sector")
    void givenInactiveSector_whenRecordingBySuggestion_thenRefuseAsSectorInactive() {
        // given
        FarmSector inactive = aFarmSector().withId(sector.id()).inactive().build();

        // when
        ProductionErrorCode code = (ProductionErrorCode) refusalCodeOf(
                () -> report.recordFeedBySuggestion(inactive, chosen(POSTURA_PLUS), JOAO, clock.instant()));

        // then
        assertThat(code).isEqualTo(ProductionErrorCode.SECTOR_INACTIVE);
        assertThat(report.cages()).noneMatch(ReportCage::hasFeed);
    }

    @Test
    @DisplayName("keeps the feed pending while a cage has no feed, and complete once every cage has")
    void givenReport_whenFeedingItBySuggestion_thenMoveTheStatusFromPendingToComplete() {
        // given
        FeedStatus before = report.feedStatus();
        int pendingBefore = report.pendingFeedCages();

        // when
        report.recordFeedBySuggestion(sector, chosen(POSTURA_PLUS), JOAO, clock.instant());

        // then
        assertThat(before).isEqualTo(FeedStatus.PENDING);
        assertThat(pendingBefore).isEqualTo(2);
        assertThat(report.feedStatus()).isEqualTo(FeedStatus.COMPLETE);
        assertThat(report.pendingFeedCages()).isZero();
    }
}
