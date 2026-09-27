package io.github.ovyx.production.domain.model;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.MARINA;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.TODAY;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.aDailyReport;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.A01;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.B07;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.aFarmSector;
import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalCodeOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.production.domain.valueobject.EggGrades;
import io.github.ovyx.production.domain.valueobject.ProductionEntry;
import io.github.ovyx.production.fixtures.InMemoryDailyReportRepository;
import io.github.ovyx.shared.domain.FixedClock;
import io.github.ovyx.shared.domain.Violation;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Correção dos dados gerais do relatório (US4; FR-003, FR-018, FR-020; invariantes 1, 2, 3 e 5): as
 * mesmas regras da abertura, e as aves do início do dia não ficam abaixo do que o relatório já removeu.
 */
@DisplayName("DailyReport correction")
class DailyReportCorrectionTest {

    private final FixedClock clock = FixedClock.at("2026-09-25T11:05:12Z");
    private final InMemoryDailyReportRepository roster = new InMemoryDailyReportRepository();
    private final FarmSector sector = aFarmSector().build();
    private final DailyReport report = stored(aDailyReport().in(sector).withRoster(roster).build());

    private DailyReport stored(DailyReport opened) {
        roster.save(opened);
        return opened;
    }

    private void correct(DailyReport target, String date, String birds, String age) {
        target.correct(sector, date, "06:45", birds, age, "Corrigido.", JOAO, TODAY, roster, clock.instant());
    }

    @Test
    @DisplayName("replaces the general data, and keeps who opened the report")
    void givenValidFields_whenCorrecting_thenReplaceTheGeneralData() {
        // given
        Instant now = clock.instant();

        // when
        report.correct(sector, "2026-09-23", "06:45", "96", "21", "Corrigido.", JOAO, TODAY, roster, now);

        // then
        assertThat(report.collectionDate().value()).isEqualTo(LocalDate.of(2026, 9, 23));
        assertThat(report.collectionTime().value()).isEqualTo(LocalTime.of(6, 45));
        assertThat(report.openingBirdCount().value()).isEqualTo(96);
        assertThat(report.flockAge().value()).isEqualTo(21);
        assertThat(report.note()).map(note -> note.value()).contains("Corrigido.");
        assertThat(report.openedBy()).isEqualTo(MARINA);
        assertThat(report.lastCorrectedBy()).contains(JOAO);
        assertThat(report.lastCorrectedAt()).contains(now);
    }

    @Test
    @DisplayName("clears the note when it comes blank")
    void givenBlankNote_whenCorrecting_thenClearTheNote() {
        // given
        String blank = "  ";

        // when
        report.correct(sector, "2026-09-24", "06:30", "98", "20", blank, JOAO, TODAY, roster, clock.instant());

        // then
        assertThat(report.note()).isEmpty();
    }

    @Test
    @DisplayName("keeps its own date without a conflict")
    void givenTheSameDate_whenCorrecting_thenAcceptIt() {
        // given
        String ownDate = "2026-09-24";

        // when
        correct(report, ownDate, "98", "21");

        // then
        assertThat(report.flockAge().value()).isEqualTo(21);
    }

    @Test
    @DisplayName("refuses the date of another report of the sector")
    void givenDateOfAnotherReport_whenCorrecting_thenRefuseAsAlreadyExisting() {
        // given
        stored(aDailyReport().in(sector).on("2026-09-23").withRoster(roster).build());

        // when
        ThrowingCallable correcting = () -> correct(report, "2026-09-23", "98", "20");

        // then
        assertThat(refusalCodeOf(correcting)).isEqualTo(ProductionErrorCode.DAILY_REPORT_ALREADY_EXISTS);
        assertThat(detailsOf(correcting))
                .containsExactly(Map.entry("collectionDate", "Já existe o relatório de 23/09/2026 neste setor."));
        assertThat(report.collectionDate().value()).isEqualTo(LocalDate.of(2026, 9, 24));
    }

    @Test
    @DisplayName("refuses every invalid field at once, as the opening does, and changes nothing")
    void givenInvalidFields_whenCorrecting_thenRefuseEveryFieldAndChangeNothing() {
        // given
        ThrowingCallable correcting = () -> report.correct(
                sector, "2026-09-26", "25:00", "0", "200", "b".repeat(501), JOAO, TODAY, roster, clock.instant());

        // when
        Map<String, String> details = detailsOf(correcting);

        // then
        assertThat(details)
                .containsOnlyKeys("collectionDate", "collectionTime", "openingBirdCount", "flockAge", "note");
        assertThat(report.openingBirdCount().value()).isEqualTo(98);
        assertThat(report.lastCorrectedBy()).isEmpty();
    }

    @Test
    @DisplayName("refuses opening birds below the birds already removed, in the opening birds field")
    void givenTwoBirdsRemoved_whenCorrectingTheOpeningBirdsToOne_thenRefuseInTheOpeningBirdsField() {
        // given
        report.recordMortality(sector, B07, "1", "1", null, MARINA, clock.instant());
        ThrowingCallable correcting = () -> correct(report, "2026-09-24", "1", "20");

        // when
        Map<String, String> details = detailsOf(correcting);

        // then
        assertThat(details)
                .containsExactly(Map.entry(
                        "openingBirdCount",
                        "O relatório já tem 2 aves removidas; as aves do início do dia não podem ficar abaixo disso."));
        assertThat(violationsOf(correcting))
                .extracting(Violation::code)
                .containsExactly(ProductionErrorCode.REMOVALS_EXCEED_OPENING_BIRDS);
    }

    @Test
    @DisplayName("accepts opening birds equal to the birds already removed")
    void givenTwoBirdsRemoved_whenCorrectingTheOpeningBirdsToTwo_thenAcceptIt() {
        // given
        report.recordMortality(sector, B07, "1", "1", null, MARINA, clock.instant());

        // when
        correct(report, "2026-09-24", "2", "20");

        // then
        assertThat(report.openingBirdCount().value()).isEqualTo(2);
    }

    @Test
    @DisplayName("keeps the cages and their entries as they were")
    void givenRecordedEntries_whenCorrecting_thenKeepTheCagesAndTheEntries() {
        // given
        report.recordProduction(sector, A01, "44", new EggGrades.Raw(null, null, null, null, null, null), MARINA, clock.instant());

        // when
        correct(report, "2026-09-24", "96", "21");

        // then
        assertThat(report.cages()).extracting(ReportCage::cageId).containsExactly(A01, B07);
        assertThat(report.cage(A01).flatMap(ReportCage::production))
                .contains(new ProductionEntry(44, new EggGrades(0, 0, 0, 0, 0, 0)));
    }

    @Test
    @DisplayName("refuses the correction of the report of an inactive sector")
    void givenInactiveSector_whenCorrecting_thenRefuseAsSectorInactive() {
        // given
        FarmSector inactive = aFarmSector().withId(sector.id()).inactive().build();

        // when
        ThrowingCallable correcting = () ->
                report.correct(inactive, "2026-09-24", "06:30", "98", "21", null, JOAO, TODAY, roster, clock.instant());

        // then
        assertThat(refusalCodeOf(correcting)).isEqualTo(ProductionErrorCode.SECTOR_INACTIVE);
        assertThat(report.flockAge().value()).isEqualTo(20);
    }
}
