package io.github.ovyx.production.domain.model;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.aDailyReport;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.A01;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.B07;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.aFarmSector;
import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalCodeOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalMessageOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.production.domain.valueobject.EggGrades;
import io.github.ovyx.production.domain.valueobject.ProductionEntry;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lançamento da produção de uma gaiola do relatório (US2; FR-007 a FR-010, FR-020; invariantes 3 e 4).
 */
@DisplayName("DailyReport production")
class DailyReportProductionTest {

    private static final EggGrades.Raw FIVE_GRADED = new EggGrades.Raw(null, "1", "1", "2", "1", null);
    private static final EggGrades.Raw NO_GRADES = new EggGrades.Raw(null, null, null, null, null, null);

    private final FixedClock clock = FixedClock.at("2026-09-24T11:05:12Z");
    private final FarmSector sector = aFarmSector().build();
    private final DailyReport report = aDailyReport().in(sector).build();

    private FarmSector inactiveSector() {
        return aFarmSector().withId(sector.id()).inactive().build();
    }

    @Test
    @DisplayName("records the production in the cage, and who recorded it and when as the last correction")
    void givenOpenReport_whenRecordingProduction_thenRecordItInTheCageAndMarkTheCorrection() {
        // given
        Instant now = clock.instant();

        // when
        report.recordProduction(sector, B07, "45", FIVE_GRADED, JOAO, now);

        // then
        assertThat(report.cage(B07).flatMap(ReportCage::production))
                .contains(new ProductionEntry(45, new EggGrades(0, 1, 1, 2, 1, 0)));
        assertThat(report.cage(A01).flatMap(ReportCage::production)).isEmpty();
        assertThat(report.lastCorrectedBy()).contains(JOAO);
        assertThat(report.lastCorrectedAt()).contains(now);
    }

    @Test
    @DisplayName("replaces the production recorded before")
    void givenRecordedProduction_whenRecordingAgain_thenReplaceIt() {
        // given
        report.recordProduction(sector, B07, "45", FIVE_GRADED, JOAO, clock.instant());

        // when
        report.recordProduction(sector, B07, "40", NO_GRADES, JOAO, clock.instant());

        // then
        assertThat(report.cage(B07).flatMap(ReportCage::production))
                .contains(new ProductionEntry(40, new EggGrades(0, 0, 0, 0, 0, 0)));
    }

    @Test
    @DisplayName("refuses a cage that is not in the report")
    void givenCageOutOfTheReport_whenRecordingProduction_thenRefuseAsCageNotFound() {
        // given
        CageId elsewhere = CageId.of(UUID.fromString("7c9e1a3b-5d7f-4a1c-8e3b-5d7f9a1c3e22"));

        // when
        ThrowingCallable recording = () -> report.recordProduction(sector, elsewhere, "45", NO_GRADES, JOAO, clock.instant());

        // then
        assertThat(refusalCodeOf(recording)).isEqualTo(ProductionErrorCode.CAGE_NOT_FOUND);
        assertThat(refusalMessageOf(recording)).isEqualTo("Gaiola não encontrada neste relatório.");
    }

    @Test
    @DisplayName("refuses to record in the report of an inactive sector, and changes nothing")
    void givenInactiveSector_whenRecordingProduction_thenRefuseAsSectorInactive() {
        // given
        FarmSector inactive = inactiveSector();

        // when
        ThrowingCallable recording = () -> report.recordProduction(inactive, B07, "45", NO_GRADES, JOAO, clock.instant());

        // then
        assertThat(refusalCodeOf(recording)).isEqualTo(ProductionErrorCode.SECTOR_INACTIVE);
        assertThat(report.cage(B07).flatMap(ReportCage::production)).isEmpty();
        assertThat(report.lastCorrectedBy()).isEmpty();
    }

    @Test
    @DisplayName("refuses invalid fields all at once, and changes nothing")
    void givenInvalidFields_whenRecordingProduction_thenRefuseEveryFieldAndChangeNothing() {
        // given
        EggGrades.Raw grades = new EggGrades.Raw(null, null, "-1", "2,5", null, null);

        // when
        Map<String, String> details = detailsOf(() -> report.recordProduction(sector, B07, "", grades, JOAO, clock.instant()));

        // then
        assertThat(details).containsOnlyKeys("eggs", "dirty", "cracked");
        assertThat(report.cage(B07).flatMap(ReportCage::production)).isEmpty();
        assertThat(report.lastCorrectedAt()).isEmpty();
    }

    @Test
    @DisplayName("refuses grades above the eggs, in the eggs field")
    void givenGradesAboveTheEggs_whenRecordingProduction_thenRefuseInTheEggsField() {
        // given
        EggGrades.Raw grades = new EggGrades.Raw("10", "10", "10", "4", null, null);

        // when
        Map<String, String> details = detailsOf(() -> report.recordProduction(sector, B07, "30", grades, JOAO, clock.instant()));

        // then
        assertThat(details)
                .containsExactly(Map.entry("eggs", "As classificações somam 34, mais que os 30 ovos coletados."));
    }

    @Test
    @DisplayName("keeps the production pending while one of the two cages lacks it")
    void givenOneOfTwoCagesRecorded_whenReadingTheStatus_thenFindOnePending() {
        // given
        report.recordProduction(sector, A01, "44", NO_GRADES, JOAO, clock.instant());

        // when
        ProductionStatus status = report.productionStatus();

        // then
        assertThat(status).isEqualTo(ProductionStatus.PENDING);
        assertThat(report.pendingCages()).isEqualTo(1);
    }

    @Test
    @DisplayName("completes the production when every cage has it")
    void givenBothCagesRecorded_whenReadingTheStatus_thenFindItComplete() {
        // given
        report.recordProduction(sector, A01, "44", NO_GRADES, JOAO, clock.instant());
        report.recordProduction(sector, B07, "45", FIVE_GRADED, JOAO, clock.instant());

        // when
        ProductionStatus status = report.productionStatus();

        // then
        assertThat(status).isEqualTo(ProductionStatus.COMPLETE);
        assertThat(report.pendingCages()).isZero();
    }

    @Test
    @DisplayName("takes zero eggs as a production recorded, and not as a cage left out")
    void givenZeroEggs_whenRecordingProduction_thenCountTheCageAsRecorded() {
        // given
        report.recordProduction(sector, A01, "44", NO_GRADES, JOAO, clock.instant());

        // when
        report.recordProduction(sector, B07, "0", NO_GRADES, JOAO, clock.instant());

        // then
        assertThat(report.cage(B07).map(ReportCage::hasProduction)).contains(true);
        assertThat(report.productionStatus()).isEqualTo(ProductionStatus.COMPLETE);
    }
}
