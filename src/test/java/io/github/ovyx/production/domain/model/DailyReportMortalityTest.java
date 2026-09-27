package io.github.ovyx.production.domain.model;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.aDailyReport;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.A01;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.B07;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.aFarmSector;
import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalCodeOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalMessageOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.production.domain.valueobject.MortalityEntry;
import io.github.ovyx.production.domain.valueobject.MortalityNote;
import io.github.ovyx.shared.domain.FixedClock;
import io.github.ovyx.shared.domain.Violation;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lançamento da mortalidade de uma gaiola e confirmação do dia sem ocorrência (US3; FR-011 a FR-015,
 * FR-020; invariantes 3, 5 e 6).
 */
@DisplayName("DailyReport mortality")
class DailyReportMortalityTest {

    private final FixedClock clock = FixedClock.at("2026-09-24T11:05:12Z");
    private final FarmSector sector = aFarmSector().build();
    private final DailyReport report = aDailyReport().in(sector).build();

    private FarmSector inactiveSector() {
        return aFarmSector().withId(sector.id()).inactive().build();
    }

    private DailyReport reportOfThreeBirds() {
        return aDailyReport().in(sector).withOpeningBirds(3).build();
    }

    @Test
    @DisplayName("records the mortality in the cage, and who recorded it and when as the last correction")
    void givenOpenReport_whenRecordingMortality_thenRecordItInTheCageAndMarkTheCorrection() {
        // given
        Instant now = clock.instant();

        // when
        report.recordMortality(sector, B07, "1", "1", "Prostração.", JOAO, now);

        // then
        assertThat(report.cage(B07).flatMap(ReportCage::mortality))
                .contains(new MortalityEntry(1, 1, new MortalityNote("Prostração.")));
        assertThat(report.lastCorrectedBy()).contains(JOAO);
        assertThat(report.lastCorrectedAt()).contains(now);
        assertThat(report.mortalityStatus()).isEqualTo(MortalityStatus.RECORDED);
    }

    @Test
    @DisplayName("keeps the birds of the cage as they were at the opening")
    void givenMortalityRecorded_whenReadingTheBirdsOfTheCage_thenFindThemUnchanged() {
        // given
        report.recordMortality(sector, B07, "2", "1", null, JOAO, clock.instant());

        // when
        int birds = report.cage(B07).orElseThrow().birdCount();

        // then
        assertThat(birds).isEqualTo(50);
    }

    @Test
    @DisplayName("refuses deaths and culls above the birds of the cage, in the deaths field, with both numbers")
    void givenRemovalsAboveTheBirdsOfTheCage_whenRecordingMortality_thenRefuseInTheDeathsField() {
        // given
        ThrowingCallable recording = () -> report.recordMortality(sector, B07, "40", "15", null, JOAO, clock.instant());

        // when
        Map<String, String> details = detailsOf(recording);

        // then
        assertThat(details).containsExactly(Map.entry("deaths", "A gaiola tem 50 aves; mortes e descartes somam 55."));
        assertThat(violationsOf(recording))
                .extracting(Violation::code)
                .containsExactly(ProductionErrorCode.REMOVALS_EXCEED_CAGE_BIRDS);
    }

    @Test
    @DisplayName("accepts deaths and culls equal to the birds of the cage")
    void givenRemovalsEqualToTheBirdsOfTheCage_whenRecordingMortality_thenAcceptThem() {
        // given
        String deaths = "40";

        // when
        report.recordMortality(sector, B07, deaths, "10", null, JOAO, clock.instant());

        // then
        assertThat(report.cage(B07).flatMap(ReportCage::mortality)).contains(new MortalityEntry(40, 10, null));
    }

    @Test
    @DisplayName("refuses removals of the day above the birds of the start of the day, in the deaths field")
    void givenRemovalsOfTheDayAboveTheOpeningBirds_whenRecordingMortality_thenRefuseInTheDeathsField() {
        // given
        DailyReport small = reportOfThreeBirds();
        small.recordMortality(sector, A01, "2", "0", null, JOAO, clock.instant());
        ThrowingCallable recording = () -> small.recordMortality(sector, B07, "2", "0", null, JOAO, clock.instant());

        // when
        Map<String, String> details = detailsOf(recording);

        // then
        assertThat(details)
                .containsExactly(Map.entry(
                        "deaths", "Mortes e descartes do dia somariam 4, mais que as 3 aves do início do dia."));
        assertThat(violationsOf(recording))
                .extracting(Violation::code)
                .containsExactly(ProductionErrorCode.REMOVALS_EXCEED_OPENING_BIRDS);
        assertThat(small.cage(B07).flatMap(ReportCage::mortality)).isEmpty();
    }

    @Test
    @DisplayName("takes the old record of the same cage out of the sum when correcting it")
    void givenCageAtTheLimitOfTheDay_whenCorrectingItToLess_thenAcceptIt() {
        // given
        DailyReport small = reportOfThreeBirds();
        small.recordMortality(sector, A01, "3", "0", null, JOAO, clock.instant());

        // when
        small.recordMortality(sector, A01, "2", "0", null, JOAO, clock.instant());

        // then
        assertThat(small.cage(A01).flatMap(ReportCage::mortality)).contains(new MortalityEntry(2, 0, null));
    }

    @Test
    @DisplayName("refuses invalid fields all at once, and changes nothing")
    void givenInvalidFields_whenRecordingMortality_thenRefuseEveryFieldAndChangeNothing() {
        // given
        String note = "b".repeat(501);

        // when
        Map<String, String> details = detailsOf(() -> report.recordMortality(sector, B07, "1,5", "-1", note, JOAO, clock.instant()));

        // then
        assertThat(details).containsOnlyKeys("deaths", "culls", "note");
        assertThat(report.cage(B07).flatMap(ReportCage::mortality)).isEmpty();
        assertThat(report.lastCorrectedAt()).isEmpty();
    }

    @Test
    @DisplayName("refuses a cage that is not in the report")
    void givenCageOutOfTheReport_whenRecordingMortality_thenRefuseAsCageNotFound() {
        // given
        CageId elsewhere = CageId.of(UUID.fromString("7c9e1a3b-5d7f-4a1c-8e3b-5d7f9a1c3e22"));

        // when
        ThrowingCallable recording = () -> report.recordMortality(sector, elsewhere, "1", "0", null, JOAO, clock.instant());

        // then
        assertThat(refusalCodeOf(recording)).isEqualTo(ProductionErrorCode.CAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("keeps the mortality pending with a record of no death and no cull")
    void givenRecordOfZeroDeathsAndZeroCulls_whenReadingTheStatus_thenFindItPending() {
        // given
        report.recordMortality(sector, B07, "0", "0", null, JOAO, clock.instant());

        // when
        MortalityStatus status = report.mortalityStatus();

        // then
        assertThat(status).isEqualTo(MortalityStatus.PENDING);
    }

    @Test
    @DisplayName("confirms the day without occurrence, and records the mortality")
    void givenReportWithoutOccurrence_whenConfirmingNoMortality_thenRecordTheMortality() {
        // given
        Instant now = clock.instant();

        // when
        boolean changed = report.confirmNoMortality(sector, JOAO, now);

        // then
        assertThat(changed).isTrue();
        assertThat(report.noMortalityConfirmed()).isTrue();
        assertThat(report.mortalityStatus()).isEqualTo(MortalityStatus.RECORDED);
        assertThat(report.lastCorrectedBy()).contains(JOAO);
        assertThat(report.lastCorrectedAt()).contains(now);
    }

    @Test
    @DisplayName("changes nothing when confirming again")
    void givenConfirmedReport_whenConfirmingAgain_thenChangeNothing() {
        // given
        Instant first = clock.instant();
        report.confirmNoMortality(sector, JOAO, first);
        clock.advance(Duration.ofMinutes(5));

        // when
        boolean changed = report.confirmNoMortality(sector, JOAO, clock.instant());

        // then
        assertThat(changed).isFalse();
        assertThat(report.lastCorrectedAt()).contains(first);
    }

    @Test
    @DisplayName("confirms the day with a record of no death and no cull")
    void givenRecordOfZeroDeathsAndZeroCulls_whenConfirmingNoMortality_thenAcceptIt() {
        // given
        report.recordMortality(sector, B07, "0", "0", null, JOAO, clock.instant());

        // when
        boolean changed = report.confirmNoMortality(sector, JOAO, clock.instant());

        // then
        assertThat(changed).isTrue();
    }

    @Test
    @DisplayName("refuses to confirm a day with a death or a cull recorded")
    void givenOccurrenceRecorded_whenConfirmingNoMortality_thenRefuseAsAlreadyRecorded() {
        // given
        report.recordMortality(sector, B07, "1", "0", null, JOAO, clock.instant());

        // when
        ThrowingCallable confirming = () -> report.confirmNoMortality(sector, JOAO, clock.instant());

        // then
        assertThat(refusalCodeOf(confirming)).isEqualTo(ProductionErrorCode.MORTALITY_ALREADY_RECORDED);
        assertThat(refusalMessageOf(confirming)).isEqualTo("O relatório já tem mortes ou descartes lançados.");
        assertThat(report.noMortalityConfirmed()).isFalse();
    }

    @Test
    @DisplayName("accepts an occurrence after the confirmation, and keeps the mortality recorded")
    void givenConfirmedReport_whenRecordingAnOccurrence_thenAcceptItAndKeepTheMortalityRecorded() {
        // given
        report.confirmNoMortality(sector, JOAO, clock.instant());

        // when
        report.recordMortality(sector, B07, "1", "0", null, JOAO, clock.instant());

        // then
        assertThat(report.cage(B07).map(ReportCage::hasOccurrence)).contains(true);
        assertThat(report.mortalityStatus()).isEqualTo(MortalityStatus.RECORDED);
    }

    @Test
    @DisplayName("refuses both operations in the report of an inactive sector")
    void givenInactiveSector_whenRecordingOrConfirming_thenRefuseAsSectorInactive() {
        // given
        FarmSector inactive = inactiveSector();

        // when
        ThrowingCallable recording = () -> report.recordMortality(inactive, B07, "1", "0", null, JOAO, clock.instant());
        ThrowingCallable confirming = () -> report.confirmNoMortality(inactive, JOAO, clock.instant());

        // then
        assertThat(refusalCodeOf(recording)).isEqualTo(ProductionErrorCode.SECTOR_INACTIVE);
        assertThat(refusalCodeOf(confirming)).isEqualTo(ProductionErrorCode.SECTOR_INACTIVE);
        assertThat(report.noMortalityConfirmed()).isFalse();
    }
}
