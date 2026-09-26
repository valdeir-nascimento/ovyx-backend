package io.github.ovyx.production.domain.model;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.MARINA;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.OPENED_AT;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.TODAY;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.aDailyReport;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.A01;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.B07;
import static io.github.ovyx.production.domain.model.FarmSectorTestDataBuilder.aFarmSector;
import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalCodeOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalMessageOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.production.fixtures.InMemoryDailyReportRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Abertura do relatório do dia (US1; FR-001 a FR-005, FR-018, FR-020; invariantes 1, 2, 3, 3a e 7).
 */
@DisplayName("DailyReport opening")
class DailyReportOpeningTest {

    @Test
    @DisplayName("fixes each active cage of the sector in the report, by battery and number, with nothing recorded")
    void givenSectorWithTwoActiveCages_whenOpening_thenFixBothCagesWithoutEntries() {
        // given
        FarmSector sector = aFarmSector()
                .withCages(new FarmCage(B07, "B", 7, 50), new FarmCage(A01, "A", 1, 48))
                .build();

        // when
        DailyReport report = aDailyReport().in(sector).build();

        // then
        assertThat(report.cages())
                .extracting(ReportCage::cageId, ReportCage::battery, ReportCage::number, ReportCage::birdCount)
                .containsExactly(
                        tuple(A01, "A", 1, 48),
                        tuple(B07, "B", 7, 50));
        assertThat(report.sectorId()).isEqualTo(sector.id());
    }

    @Test
    @DisplayName("records the general data, who opened it and when")
    void givenValidFields_whenOpening_thenRecordTheGeneralDataAndTheOpening() {
        // given
        DailyReportTestDataBuilder builder = aDailyReport();

        // when
        DailyReport report = builder.build();

        // then
        assertThat(report.collectionDate().value()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(report.collectionTime().value()).isEqualTo(LocalTime.of(6, 30));
        assertThat(report.openingBirdCount().value()).isEqualTo(98);
        assertThat(report.flockAge().value()).isEqualTo(20);
        assertThat(report.note()).map(note -> note.value()).contains("Bebedouro da bateria B trocado.");
        assertThat(report.openedBy()).isEqualTo(MARINA);
        assertThat(report.openedAt()).isEqualTo(OPENED_AT);
        assertThat(report.lastCorrectedBy()).isEmpty();
        assertThat(report.lastCorrectedAt()).isEmpty();
    }

    @Test
    @DisplayName("starts with the production pending for every cage, and the mortality pending")
    void givenNewReport_whenReadingTheStatuses_thenFindBothPending() {
        // given
        DailyReport report = aDailyReport().build();

        // when
        ProductionStatus production = report.productionStatus();
        MortalityStatus mortality = report.mortalityStatus();

        // then
        assertThat(production).isEqualTo(ProductionStatus.PENDING);
        assertThat(report.pendingCages()).isEqualTo(2);
        assertThat(mortality).isEqualTo(MortalityStatus.PENDING);
        assertThat(report.noMortalityConfirmed()).isFalse();
    }

    @Test
    @DisplayName("refuses a future date, no birds and 200 weeks at once")
    void givenThreeInvalidFields_whenOpening_thenRefuseWithTheThreeViolationsAtOnce() {
        // given
        FarmSector sector = aFarmSector().build();

        // when
        ThrowingCallable opening = () -> DailyReport.open(
                sector, "2026-09-26", "06:30", "0", "200", null, MARINA, TODAY, (s, d, e) -> false, OPENED_AT);

        // then
        assertThat(refusalCodeOf(opening)).isEqualTo(ProductionErrorCode.VALIDATION_FAILED);
        assertThat(detailsOf(opening))
                .containsOnlyKeys("collectionDate", "openingBirdCount", "flockAge");
    }

    @Test
    @DisplayName("refuses the date of another report of the sector")
    void givenReportOfTheSectorOnTheDate_whenOpeningAnotherOnTheSameDate_thenRefuseAsAlreadyExisting() {
        // given
        InMemoryDailyReportRepository repository = new InMemoryDailyReportRepository();
        FarmSector sector = aFarmSector().build();
        repository.save(aDailyReport().in(sector).build());

        // when
        ThrowingCallable opening = () -> aDailyReport().in(sector).withRoster(repository).build();

        // then
        assertThat(refusalCodeOf(opening)).isEqualTo(ProductionErrorCode.DAILY_REPORT_ALREADY_EXISTS);
        assertThat(detailsOf(opening))
                .containsExactly(Map.entry("collectionDate", "Já existe o relatório de 24/09/2026 neste setor."));
    }

    @Test
    @DisplayName("accepts the same date in another sector")
    void givenReportOfOneSectorOnTheDate_whenOpeningAnotherSectorOnTheSameDate_thenAcceptIt() {
        // given
        InMemoryDailyReportRepository repository = new InMemoryDailyReportRepository();
        repository.save(aDailyReport().in(aFarmSector().build()).build());

        // when
        DailyReport report = aDailyReport().in(aFarmSector().build()).withRoster(repository).build();

        // then
        assertThat(report.collectionDate().value()).isEqualTo(LocalDate.of(2026, 9, 24));
    }

    @Test
    @DisplayName("refuses to open a report in an inactive sector")
    void givenInactiveSector_whenOpening_thenRefuseAsSectorInactive() {
        // given
        FarmSector sector = aFarmSector().inactive().build();

        // when
        ThrowingCallable opening = () -> aDailyReport().in(sector).build();

        // then
        assertThat(refusalCodeOf(opening)).isEqualTo(ProductionErrorCode.SECTOR_INACTIVE);
        assertThat(refusalMessageOf(opening)).isEqualTo("O setor está inativo; os relatórios dele são só para consulta.");
    }

    @Test
    @DisplayName("refuses to open a report in a sector without active cages")
    void givenActiveSectorWithoutActiveCages_whenOpening_thenRefuseAsWithoutCages() {
        // given
        FarmSector sector = aFarmSector().withoutCages().build();

        // when
        ThrowingCallable opening = () -> aDailyReport().in(sector).build();

        // then
        assertThat(refusalCodeOf(opening)).isEqualTo(ProductionErrorCode.SECTOR_WITHOUT_ACTIVE_CAGES);
        assertThat(refusalMessageOf(opening))
                .isEqualTo("O setor não tem gaiola ativa. Cadastre as gaiolas antes de abrir o relatório.");
    }
}
