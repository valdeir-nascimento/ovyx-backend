package io.github.ovyx.production.integration;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.aDailyReport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.production.application.dailyreport.CageFeed;
import io.github.ovyx.production.application.dailyreport.CageMortality;
import io.github.ovyx.production.application.dailyreport.CageProduction;
import io.github.ovyx.production.application.dailyreport.DailyReportDetail;
import io.github.ovyx.production.application.dailyreport.DailyReportDirectory;
import io.github.ovyx.production.application.dailyreport.DailyReportSummary;
import io.github.ovyx.production.application.dailyreport.FeedTotals;
import io.github.ovyx.production.application.dailyreport.LatestDailyReport;
import io.github.ovyx.production.application.dailyreport.ReportCageDetail;
import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.production.domain.model.DailyReport;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.FarmSector;
import io.github.ovyx.production.domain.model.FeedFormulaChoice;
import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.production.domain.port.DailyReportRepository;
import io.github.ovyx.production.domain.port.FarmStructure;
import io.github.ovyx.production.domain.port.FeedCatalog;
import io.github.ovyx.production.domain.valueobject.EggGrades;
import io.github.ovyx.shared.application.PageResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** A leitura dos relatórios direto do banco (R-008): lista, detalhe, gaiola e sugestão. */
@DisplayName("Daily report directory")
class DailyReportDirectoryIT extends IntegrationTestSupport {

    @Autowired
    private DailyReportDirectory directory;

    @Autowired
    private DailyReportRepository repository;

    @Autowired
    private FarmStructure farmStructure;

    @Autowired
    private FeedCatalog feedCatalog;

    @Autowired
    private JdbcTemplate jdbc;

    private ProductionFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new ProductionFixtures(jdbc);
    }

    private FarmSector sectorWithTwoCages() {
        return farmStructure.sectorOf(SectorId.of(fixtures.sectorWithTwoCages())).orElseThrow();
    }

    private static final Instant RECORDED_AT = Instant.parse("2026-09-24T11:05:12Z");
    private static final EggGrades.Raw NO_GRADES = new EggGrades.Raw(null, null, null, null, null, null);

    private DailyReport savedOn(FarmSector sector, String date) {
        DailyReport report = aDailyReport().in(sector).on(date).withRoster(repository).build();
        repository.save(report);
        return report;
    }

    /**
     * O relatório de 24/09 com as duas gaiolas lançadas, como o exemplo do contrato: A-01 com 44 ovos e
     * B-07 com 45, 89 ao todo, 10 classificados e 4 não comercializáveis, sobre 98 aves.
     */
    private DailyReport savedWithBothCagesRecorded(FarmSector sector) {
        DailyReport report = aDailyReport().in(sector).withRoster(repository).build();
        report.recordProduction(
                sector, cageOf(report, 0), "44", new EggGrades.Raw("1", "2", "1", "1", null, null), JOAO, RECORDED_AT);
        report.recordProduction(
                sector, cageOf(report, 1), "45", new EggGrades.Raw(null, "1", "1", "2", "1", null), JOAO, RECORDED_AT);
        repository.save(report);
        return report;
    }

    private static CageId cageOf(DailyReport report, int index) {
        return report.cages().get(index).cageId();
    }

    @Test
    @DisplayName("lists the reports of the sector from the most recent day, with the sector")
    void givenReportsOnThreeDays_whenListing_thenFindThemFromTheMostRecent() {
        // given
        FarmSector sector = sectorWithTwoCages();
        savedOn(sector, "2026-09-22");
        savedOn(sector, "2026-09-24");
        savedOn(sector, "2026-09-23");
        savedOn(sectorWithTwoCages(), "2026-09-24");

        // when
        PageResponse<DailyReportSummary> page = directory.list(sector.id(), null, 0, 20);

        // then
        assertThat(page.content())
                .extracting(DailyReportSummary::collectionDate)
                .containsExactly(LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 22));
        assertThat(page.metadata().totalElements()).isEqualTo(3);
        assertThat(directory.sectorOf(sector.id()))
                .hasValueSatisfying(found -> {
                    assertThat(found.name()).isEqualTo(sector.name());
                    assertThat(found.status()).isEqualTo("ACTIVE");
                });
    }

    @Test
    @DisplayName("filters by the date of the collection")
    void givenReportsOnTwoDays_whenFilteringByOne_thenFindOnlyThatDay() {
        // given
        FarmSector sector = sectorWithTwoCages();
        savedOn(sector, "2026-09-23");
        DailyReport wanted = savedOn(sector, "2026-09-24");

        // when
        PageResponse<DailyReportSummary> page = directory.list(sector.id(), LocalDate.of(2026, 9, 24), 0, 20);

        // then
        assertThat(page.content()).extracting(DailyReportSummary::id).containsExactly(wanted.id().value());
    }

    @Test
    @DisplayName("pages the reports")
    void givenThreeReports_whenAskingForPagesOfTwo_thenSplitThem() {
        // given
        FarmSector sector = sectorWithTwoCages();
        savedOn(sector, "2026-09-22");
        savedOn(sector, "2026-09-23");
        savedOn(sector, "2026-09-24");

        // when
        PageResponse<DailyReportSummary> second = directory.list(sector.id(), null, 1, 2);

        // then
        assertThat(second.content()).extracting(DailyReportSummary::collectionDate).containsExactly(LocalDate.of(2026, 9, 22));
        assertThat(second.metadata().totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("sums nothing for a report without entries, with both launches pending")
    void givenReportWithoutEntries_whenListing_thenFindZeroTotalsAndPendingStatuses() {
        // given
        FarmSector sector = sectorWithTwoCages();
        savedOn(sector, "2026-09-24");

        // when
        DailyReportSummary summary = directory.list(sector.id(), null, 0, 20).content().getFirst();

        // then
        assertThat(summary.openedByName()).isEqualTo("Marina Alves");
        assertThat(summary.flockAge()).isEqualTo(20);
        assertThat(summary.collectedEggs()).isZero();
        assertThat(summary.removedBirds()).isZero();
        assertThat(summary.closingBirdCount()).isEqualTo(98);
        assertThat(summary.productionStatus()).isEqualTo(ProductionStatus.PENDING);
        assertThat(summary.pendingCages()).isEqualTo(2);
        assertThat(summary.mortalityStatus()).isEqualTo(MortalityStatus.PENDING);
    }

    @Test
    @DisplayName("details the report with its cages, by battery and number, and the pending totals")
    void givenReportWithoutEntries_whenReadingTheDetail_thenFindTheCagesAndPendingTotals() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = savedOn(sector, "2026-09-24");

        // when
        DailyReportDetail detail = directory.findDetail(sector.id(), report.id()).orElseThrow();

        // then
        assertThat(detail.sector().name()).isEqualTo(sector.name());
        assertThat(detail.openingBirdCount()).isEqualTo(98);
        assertThat(detail.openedBy().name()).isEqualTo("Marina Alves");
        assertThat(detail.cages()).extracting(ReportCageDetail::code).containsExactly("A-01", "B-07");
        assertThat(detail.cages()).allSatisfy(cage -> {
            assertThat(cage.production()).isNull();
            assertThat(cage.mortality()).isNull();
        });
        assertThat(detail.production().status()).isEqualTo(ProductionStatus.PENDING);
        assertThat(detail.production().pendingCages()).isEqualTo(2);
        assertThat(detail.production().layingRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(detail.mortality().closingBirdCount()).isEqualTo(98);
    }

    @Test
    @DisplayName("details each cage with its production, and sums the recorded ones in the totals of the day")
    void givenBothCagesRecorded_whenReadingTheDetail_thenFindEachProductionAndTheTotals() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = savedWithBothCagesRecorded(sector);

        // when
        DailyReportDetail detail = directory.findDetail(sector.id(), report.id()).orElseThrow();

        // then
        assertThat(detail.cages())
                .extracting(ReportCageDetail::code, ReportCageDetail::production)
                .containsExactly(
                        tuple("A-01", new CageProduction(44, 1, 2, 1, 1, 0, 0)),
                        tuple("B-07", new CageProduction(45, 0, 1, 1, 2, 1, 0)));
        assertThat(detail.production().status()).isEqualTo(ProductionStatus.COMPLETE);
        assertThat(detail.production().pendingCages()).isZero();
        assertThat(detail.production().collectedEggs()).isEqualTo(89);
        assertThat(detail.production().standardEggs()).isEqualTo(79);
        assertThat(detail.production().unsellableEggs()).isEqualTo(4);
        assertThat(detail.production().layingRate()).isEqualByComparingTo("90.82");
    }

    @Test
    @DisplayName("sums only the recorded cages, and keeps the others as pending, without production")
    void givenOneOfTwoCagesRecorded_whenReadingTheDetail_thenSumOnlyTheRecordedOne() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = aDailyReport().in(sector).withRoster(repository).build();
        report.recordProduction(sector, cageOf(report, 1), "45", NO_GRADES, JOAO, RECORDED_AT);
        repository.save(report);

        // when
        DailyReportDetail detail = directory.findDetail(sector.id(), report.id()).orElseThrow();

        // then
        assertThat(detail.cages().get(0).production()).isNull();
        assertThat(detail.cages().get(1).production()).isEqualTo(new CageProduction(45, 0, 0, 0, 0, 0, 0));
        assertThat(detail.production().status()).isEqualTo(ProductionStatus.PENDING);
        assertThat(detail.production().pendingCages()).isEqualTo(1);
        assertThat(detail.production().collectedEggs()).isEqualTo(45);
        assertThat(detail.production().standardEggs()).isEqualTo(45);
    }

    @Test
    @DisplayName("writes the laying rate with two decimals: 1 egg over 3 birds is 33.33")
    void givenOneEggOverThreeBirds_whenReadingTheDetail_thenFindThirtyThreePointThirtyThree() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = aDailyReport().in(sector).withOpeningBirds(3).withRoster(repository).build();
        report.recordProduction(sector, cageOf(report, 0), "1", NO_GRADES, JOAO, RECORDED_AT);
        repository.save(report);

        // when
        DailyReportDetail detail = directory.findDetail(sector.id(), report.id()).orElseThrow();

        // then
        assertThat(detail.production().layingRate()).isEqualByComparingTo("33.33");
    }

    @Test
    @DisplayName("rounds the laying rate half up: 1 egg over 32 birds is 3.13, and not 3.12")
    void givenOneEggOverThirtyTwoBirds_whenReadingTheDetail_thenRoundHalfUp() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = aDailyReport().in(sector).withOpeningBirds(32).withRoster(repository).build();
        report.recordProduction(sector, cageOf(report, 0), "1", NO_GRADES, JOAO, RECORDED_AT);
        repository.save(report);

        // when
        DailyReportDetail detail = directory.findDetail(sector.id(), report.id()).orElseThrow();

        // then
        assertThat(detail.production().layingRate()).isEqualByComparingTo("3.13");
    }

    @Test
    @DisplayName("lists the eggs and the pending cages as the detail counts them")
    void givenOneOfTwoCagesRecorded_whenListing_thenFindTheSameCountsOfTheDetail() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = aDailyReport().in(sector).withRoster(repository).build();
        report.recordProduction(sector, cageOf(report, 0), "44", NO_GRADES, JOAO, RECORDED_AT);
        repository.save(report);

        // when
        DailyReportSummary summary = directory.list(sector.id(), null, 0, 20).content().getFirst();

        // then
        DailyReportDetail detail = directory.findDetail(sector.id(), report.id()).orElseThrow();
        assertThat(summary.collectedEggs()).isEqualTo(detail.production().collectedEggs()).isEqualTo(44);
        assertThat(summary.pendingCages()).isEqualTo(detail.production().pendingCages()).isEqualTo(1);
        assertThat(summary.productionStatus()).isEqualTo(ProductionStatus.PENDING);
    }

    @Test
    @DisplayName("lists the production as complete when every cage is recorded")
    void givenBothCagesRecorded_whenListing_thenFindTheProductionComplete() {
        // given
        FarmSector sector = sectorWithTwoCages();
        savedWithBothCagesRecorded(sector);

        // when
        DailyReportSummary summary = directory.list(sector.id(), null, 0, 20).content().getFirst();

        // then
        assertThat(summary.productionStatus()).isEqualTo(ProductionStatus.COMPLETE);
        assertThat(summary.pendingCages()).isZero();
        assertThat(summary.collectedEggs()).isEqualTo(89);
    }

    @Test
    @DisplayName("finds a cage of the report with the same data of its row in the detail")
    void givenRecordedReport_whenFindingOneCage_thenFindTheRowOfTheDetail() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = savedWithBothCagesRecorded(sector);
        DailyReportDetail detail = directory.findDetail(sector.id(), report.id()).orElseThrow();

        // when
        ReportCageDetail cage = directory.findCage(sector.id(), report.id(), cageOf(report, 1)).orElseThrow();

        // then
        assertThat(cage).isEqualTo(detail.cages().get(1));
    }

    @Test
    @DisplayName("finds no cage out of the report, nor through another report or sector")
    void givenReport_whenFindingACageThatIsNotOfIt_thenFindNothing() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = savedOn(sector, "2026-09-24");
        DailyReport other = savedOn(sectorWithTwoCages(), "2026-09-24");

        // when / then
        assertThat(directory.findCage(sector.id(), report.id(), cageOf(other, 0))).isEmpty();
        assertThat(directory.findCage(sector.id(), other.id(), cageOf(other, 0))).isEmpty();
        assertThat(directory.findCage(SectorId.of(UUID.randomUUID()), report.id(), cageOf(report, 0))).isEmpty();
    }

    @Test
    @DisplayName("tells whether the report exists in the sector")
    void givenReport_whenAskingWhetherItExists_thenAnswerOnlyForItsSector() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = savedOn(sector, "2026-09-24");

        // when / then
        assertThat(directory.reportExists(sector.id(), report.id())).isTrue();
        assertThat(directory.reportExists(SectorId.of(UUID.randomUUID()), report.id())).isFalse();
        assertThat(directory.reportExists(sector.id(), DailyReportId.generate())).isFalse();
    }

    @Test
    @DisplayName("sums the deaths and the culls of the cages, with the rate and the balance of the day")
    void givenOneDeathAndOneCull_whenReadingTheDetailAndTheList_thenFindTheTotalsOfTheMortality() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = aDailyReport().in(sector).withRoster(repository).build();
        report.recordMortality(sector, cageOf(report, 0), "1", "0", null, JOAO, RECORDED_AT);
        report.recordMortality(sector, cageOf(report, 1), "0", "1", "Prostração.", JOAO, RECORDED_AT);
        repository.save(report);

        // when
        DailyReportDetail detail = directory.findDetail(sector.id(), report.id()).orElseThrow();
        DailyReportSummary summary = directory.list(sector.id(), null, 0, 20).content().getFirst();

        // then
        assertThat(detail.mortality().status()).isEqualTo(MortalityStatus.RECORDED);
        assertThat(detail.mortality().deaths()).isEqualTo(1);
        assertThat(detail.mortality().culls()).isEqualTo(1);
        assertThat(detail.mortality().removalRate()).isEqualByComparingTo("2.04");
        assertThat(detail.mortality().closingBirdCount()).isEqualTo(96);
        assertThat(detail.cages().get(1).mortality()).isEqualTo(new CageMortality(0, 1, "Prostração."));
        assertThat(summary.removedBirds()).isEqualTo(2);
        assertThat(summary.closingBirdCount()).isEqualTo(96);
        assertThat(summary.mortalityStatus()).isEqualTo(MortalityStatus.RECORDED);
    }

    @Test
    @DisplayName("writes the rate of the day with two decimals: 3 removals over 2,400 birds is 0.13")
    void givenThreeRemovalsOverTwoThousandFourHundredBirds_whenReadingTheDetail_thenFindZeroPointThirteen() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = aDailyReport().in(sector).withOpeningBirds(2400).withRoster(repository).build();
        report.recordMortality(sector, cageOf(report, 0), "2", "0", null, JOAO, RECORDED_AT);
        report.recordMortality(sector, cageOf(report, 1), "0", "1", null, JOAO, RECORDED_AT);
        repository.save(report);

        // when
        DailyReportDetail detail = directory.findDetail(sector.id(), report.id()).orElseThrow();

        // then
        assertThat(detail.mortality().removalRate()).isEqualByComparingTo("0.13");
        assertThat(detail.mortality().closingBirdCount()).isEqualTo(2397);
    }

    @Test
    @DisplayName("takes the confirmation of a day without occurrence as the mortality recorded")
    void givenConfirmedDay_whenReadingTheDetailAndTheList_thenFindTheMortalityRecordedWithNothingRemoved() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = aDailyReport().in(sector).withRoster(repository).build();
        report.confirmNoMortality(sector, JOAO, RECORDED_AT);
        repository.save(report);

        // when
        DailyReportDetail detail = directory.findDetail(sector.id(), report.id()).orElseThrow();
        DailyReportSummary summary = directory.list(sector.id(), null, 0, 20).content().getFirst();

        // then
        assertThat(detail.noMortalityConfirmed()).isTrue();
        assertThat(detail.mortality().status()).isEqualTo(MortalityStatus.RECORDED);
        assertThat(detail.mortality().removalRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(detail.mortality().closingBirdCount()).isEqualTo(98);
        assertThat(summary.mortalityStatus()).isEqualTo(MortalityStatus.RECORDED);
    }

    @Test
    @DisplayName("keeps the mortality pending with a record of no death and no cull")
    void givenRecordOfZeroDeathsAndZeroCulls_whenListing_thenFindTheMortalityPending() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = aDailyReport().in(sector).withRoster(repository).build();
        report.recordMortality(sector, cageOf(report, 0), "0", "0", null, JOAO, RECORDED_AT);
        repository.save(report);

        // when
        DailyReportSummary summary = directory.list(sector.id(), null, 0, 20).content().getFirst();

        // then
        assertThat(summary.mortalityStatus()).isEqualTo(MortalityStatus.PENDING);
        assertThat(directory.findDetail(sector.id(), report.id()).orElseThrow().cages().getFirst().mortality())
                .isEqualTo(new CageMortality(0, 0, null));
    }

    @Test
    @DisplayName("suggests the next day from the balance of the latest report, after the deaths and the culls")
    void givenLatestReportWithRemovals_whenAskingForTheLatest_thenFindItsBalance() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = aDailyReport().in(sector).withRoster(repository).build();
        report.recordMortality(sector, cageOf(report, 0), "2", "1", null, JOAO, RECORDED_AT);
        repository.save(report);

        // when
        LatestDailyReport latest = directory.latestOf(sector.id()).orElseThrow();

        // then
        assertThat(latest.closingBirdCount()).isEqualTo(95);
    }

    @Test
    @DisplayName("finds no detail through another sector")
    void givenReport_whenReadingTheDetailThroughAnotherSector_thenFindNothing() {
        // given
        DailyReport report = savedOn(sectorWithTwoCages(), "2026-09-24");

        // when / then
        assertThat(directory.findDetail(SectorId.of(UUID.randomUUID()), report.id())).isEmpty();
    }

    @Test
    @DisplayName("finds the most recent report of the sector by the collection date, for the suggestion")
    void givenReportsOnTwoDays_whenAskingForTheLatest_thenFindTheMostRecentDay() {
        // given
        FarmSector sector = sectorWithTwoCages();
        savedOn(sector, "2026-09-24");
        savedOn(sector, "2026-09-22");

        // when
        LatestDailyReport latest = directory.latestOf(sector.id()).orElseThrow();

        // then
        assertThat(latest.collectionDate()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(latest.flockAge()).isEqualTo(20);
        assertThat(latest.closingBirdCount()).isEqualTo(98);
    }

    @Test
    @DisplayName("sums the birds of the active cages of the sector, for the first report")
    void givenSectorWithActiveAndInactiveCages_whenSummingTheBirds_thenCountOnlyTheActive() {
        // given
        UUID sectorId = fixtures.activeSector();
        fixtures.activeCage(sectorId, "A", 1, 48);
        fixtures.activeCage(sectorId, "A", 2, 50);
        fixtures.cage(sectorId, "A", 3, 40, "INACTIVE");

        // when
        int birds = directory.activeBirdsOf(SectorId.of(sectorId));

        // then
        assertThat(birds).isEqualTo(98);
    }

    // ------------------------------------------------------------------ ração (feature 004)

    /** O relatório de 24/09 com as duas gaiolas lançadas e a ração do setor pela sugestão da fórmula. */
    private DailyReport savedFedWith(FarmSector sector, UUID formulaId) {
        DailyReport report = savedWithBothCagesRecorded(sector);
        DailyReport loaded = repository.findById(sector.id(), report.id()).orElseThrow();
        loaded.recordFeedBySuggestion(
                sector, FeedFormulaChoice.lookup(formulaId.toString(), feedCatalog), JOAO, RECORDED_AT);
        repository.save(loaded);
        return loaded;
    }

    @Test
    @DisplayName("gives each cage of the detail its feed, with the price and the intake kept, and the totals of the day")
    void givenReportFedBySuggestion_whenFindingTheDetail_thenFindTheFeedOfEachCageAndTheTotals() {
        // given
        FarmSector sector = sectorWithTwoCages();
        UUID posturaPlus = fixtures.posturaPlus();
        DailyReport report = savedFedWith(sector, posturaPlus);

        // when
        DailyReportDetail detail = directory.findDetail(sector.id(), report.id()).orElseThrow();

        // then
        CageFeed b07 = detail.cages().get(1).feed();
        assertThat(b07.formulaId()).isEqualTo(posturaPlus);
        assertThat(b07.formulaName()).isEqualTo(fixtures.formulaNameOf(posturaPlus));
        assertThat(b07.pricePerKg()).isEqualByComparingTo("2.85");
        assertThat(b07.expectedIntake()).isEqualTo(28);
        assertThat(b07.consumption()).isEqualTo(1400);
        assertThat(b07.cost()).isEqualByComparingTo("3.99");
        assertThat(b07.intakePerBird()).isEqualByComparingTo("28.0");
        assertThat(b07.deviation()).isEqualByComparingTo("0.0");
        FeedTotals feed = detail.feed();
        assertThat(feed.status()).isEqualTo(FeedStatus.COMPLETE);
        assertThat(feed.pendingCages()).isZero();
        assertThat(feed.consumption()).isEqualTo(2744);
        assertThat(feed.cost()).isEqualByComparingTo("7.82");
        assertThat(feed.costPerEgg()).isEqualByComparingTo("0.088");
        assertThat(feed.intakePerBird()).isEqualByComparingTo("28.0");
        assertThat(feed.expectedIntakePerBird()).isEqualByComparingTo("28.0");
    }

    @Test
    @DisplayName("shows the current name of the formula, and the price kept in the entry after the formula changes")
    void givenFormulaRenamedAndRepriced_whenFindingTheDetail_thenShowTheNewNameAndTheOldPrice() {
        // given
        FarmSector sector = sectorWithTwoCages();
        UUID posturaPlus = fixtures.posturaPlus();
        DailyReport report = savedFedWith(sector, posturaPlus);
        jdbc.update("update feed_formula set name = ? where id = ?", "Postura Plus " + posturaPlus, posturaPlus);
        fixtures.repriceFormula(posturaPlus, "3.10");

        // when
        DailyReportDetail detail = directory.findDetail(sector.id(), report.id()).orElseThrow();

        // then
        assertThat(detail.cages().get(0).feed().formulaName()).isEqualTo("Postura Plus " + posturaPlus);
        assertThat(detail.cages().get(0).feed().pricePerKg()).isEqualByComparingTo("2.85");
        assertThat(detail.feed().cost()).isEqualByComparingTo("7.82");
    }

    @Test
    @DisplayName("leaves the feed of the cages out, and the feed pending, before any feed")
    void givenReportWithoutFeed_whenFindingTheDetail_thenFindTheFeedPending() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = savedWithBothCagesRecorded(sector);

        // when
        DailyReportDetail detail = directory.findDetail(sector.id(), report.id()).orElseThrow();

        // then
        assertThat(detail.cages()).allSatisfy(cage -> assertThat(cage.feed()).isNull());
        assertThat(detail.feed().status()).isEqualTo(FeedStatus.PENDING);
        assertThat(detail.feed().pendingCages()).isEqualTo(2);
        assertThat(detail.feed().consumption()).isZero();
        assertThat(detail.feed().intakePerBird()).isNull();
    }

    @Test
    @DisplayName("gives the cage of the report its feed")
    void givenReportFedBySuggestion_whenFindingACage_thenFindItsFeed() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = savedFedWith(sector, fixtures.posturaPlus());

        // when
        ReportCageDetail a01 = directory.findCage(sector.id(), report.id(), cageOf(report, 0)).orElseThrow();

        // then
        assertThat(a01.feed().consumption()).isEqualTo(1344);
        assertThat(a01.feed().cost()).isEqualByComparingTo("3.83");
    }

    @Test
    @DisplayName("lists each report with the status of its feed and the cages still without it")
    void givenOneReportFedAndOneNot_whenListing_thenFindTheStatusOfTheFeedOfEach() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport fed = savedFedWith(sector, fixtures.posturaPlus());
        DailyReport notFed = savedOn(sector, "2026-09-23");

        // when
        PageResponse<DailyReportSummary> page = directory.list(sector.id(), null, 0, 20);

        // then
        assertThat(page.content())
                .extracting(DailyReportSummary::id, DailyReportSummary::feedStatus, DailyReportSummary::feedPendingCages)
                .containsExactly(
                        tuple(fed.id().value(), FeedStatus.COMPLETE, 0),
                        tuple(notFed.id().value(), FeedStatus.PENDING, 2));
    }

    @Test
    @DisplayName("weighs the expected intake of the day by the birds of each cage, with two formulas (004, US4)")
    void givenCagesFedWithTwoFormulas_whenFindingTheDetail_thenWeighTheExpectedIntakeByTheBirds() {
        // given
        // A-01 (48 aves) com 24 g e B-07 (50 aves) com 28 g: (48 × 24 + 50 × 28) ÷ 98 = 26,04 g.
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = savedWithBothCagesRecorded(sector);
        DailyReport loaded = repository.findById(sector.id(), report.id()).orElseThrow();
        loaded.recordFeed(
                sector,
                cageOf(loaded, 0),
                FeedFormulaChoice.lookup(fixtures.formula("3.10", 24, "ACTIVE").toString(), feedCatalog),
                "1100",
                JOAO,
                RECORDED_AT);
        loaded.recordFeed(
                sector,
                cageOf(loaded, 1),
                FeedFormulaChoice.lookup(fixtures.posturaPlus().toString(), feedCatalog),
                "1250",
                JOAO,
                RECORDED_AT);
        repository.save(loaded);

        // when
        DailyReportDetail detail = directory.findDetail(sector.id(), report.id()).orElseThrow();

        // then
        assertThat(detail.feed().expectedIntakePerBird()).isEqualByComparingTo("26.0");
        assertThat(detail.feed().intakePerBird()).isEqualByComparingTo("24.0");
        assertThat(detail.cages().get(0).feed().deviation()).isEqualByComparingTo("-4.5");
        assertThat(detail.cages().get(1).feed().intakePerBird()).isEqualByComparingTo("25.0");
        assertThat(detail.cages().get(1).feed().deviation()).isEqualByComparingTo("-10.7");
    }

    @Test
    @DisplayName("leaves the cost per egg out of a report fed without any egg collected (004, US4)")
    void givenReportFedWithoutProduction_whenFindingTheDetail_thenLeaveTheCostPerEggOut() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = savedOn(sector, "2026-09-24");
        DailyReport loaded = repository.findById(sector.id(), report.id()).orElseThrow();
        loaded.recordFeedBySuggestion(
                sector, FeedFormulaChoice.lookup(fixtures.posturaPlus().toString(), feedCatalog), JOAO, RECORDED_AT);
        repository.save(loaded);

        // when
        DailyReportDetail detail = directory.findDetail(sector.id(), report.id()).orElseThrow();

        // then
        assertThat(detail.feed().cost()).isEqualByComparingTo("7.82");
        assertThat(detail.feed().costPerEgg()).isNull();
    }

    // ------------------------------------------------------------------ relatórios de um intervalo (feature 007)

    /** Um relatório completo no dia: as duas gaiolas com produção, uma morte na A-01 e a ração pela sugestão. */
    private DailyReport savedCompleteOn(FarmSector sector, String date, UUID formulaId) {
        DailyReport report = aDailyReport().in(sector).on(date).withRoster(repository).build();
        report.recordProduction(
                sector, cageOf(report, 0), "44", new EggGrades.Raw("1", "2", "1", "1", null, null), JOAO, RECORDED_AT);
        report.recordProduction(sector, cageOf(report, 1), "45", NO_GRADES, JOAO, RECORDED_AT);
        report.recordMortality(sector, cageOf(report, 0), "1", "0", "Bicada.", JOAO, RECORDED_AT);
        report.recordFeedBySuggestion(
                sector, FeedFormulaChoice.lookup(formulaId.toString(), feedCatalog), JOAO, RECORDED_AT);
        repository.save(report);
        return report;
    }

    @Test
    @DisplayName("reads the reports of the interval, both ends included, from the oldest")
    void givenReportsAroundTheInterval_whenReadingTheInterval_thenFindOnlyTheDaysInsideItFromTheOldest() {
        // given
        FarmSector sector = sectorWithTwoCages();
        UUID formula = fixtures.posturaPlus();
        savedOn(sector, "2026-09-21");
        savedCompleteOn(sector, "2026-09-24", formula);
        savedOn(sector, "2026-09-22");
        savedCompleteOn(sector, "2026-09-23", formula);
        savedOn(sector, "2026-09-25");
        savedOn(sectorWithTwoCages(), "2026-09-23");

        // when
        List<DailyReportDetail> details =
                directory.detailsBetween(sector.id(), LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 24));

        // then
        assertThat(details)
                .extracting(DailyReportDetail::collectionDate)
                .containsExactly(LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 24));
        assertThat(details).allSatisfy(detail -> assertThat(detail.sector().id()).isEqualTo(sector.id().value()));
    }

    @Test
    @DisplayName("reads each report of the interval as the detail reads it, cages, entries and totals")
    void givenCompleteAndEmptyReports_whenReadingTheInterval_thenFindTheSameDetailOfEachOne() {
        // given
        FarmSector sector = sectorWithTwoCages();
        UUID formula = fixtures.posturaPlus();
        DailyReport empty = savedOn(sector, "2026-09-22");
        DailyReport complete = savedCompleteOn(sector, "2026-09-23", formula);

        // when
        List<DailyReportDetail> details =
                directory.detailsBetween(sector.id(), LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 23));

        // then
        assertThat(details).hasSize(2);
        assertThat(details.get(0)).isEqualTo(directory.findDetail(sector.id(), empty.id()).orElseThrow());
        assertThat(details.get(1)).isEqualTo(directory.findDetail(sector.id(), complete.id()).orElseThrow());
        assertThat(details.get(1).cages())
                .extracting(ReportCageDetail::code)
                .containsExactly("A-01", "B-07");
        assertThat(details.get(0).cages())
                .allSatisfy(cage -> {
                    assertThat(cage.production()).isNull();
                    assertThat(cage.mortality()).isNull();
                    assertThat(cage.feed()).isNull();
                });
        assertThat(details.get(1).feed().status()).isEqualTo(FeedStatus.COMPLETE);
        assertThat(details.get(1).production().collectedEggs()).isEqualTo(89);
        assertThat(details.get(1).mortality().deaths()).isEqualTo(1);
    }

    @Test
    @DisplayName("reads nothing from an interval without reports")
    void givenNoReportInTheInterval_whenReadingTheInterval_thenFindNothing() {
        // given
        FarmSector sector = sectorWithTwoCages();
        savedOn(sector, "2026-09-21");

        // when
        List<DailyReportDetail> details =
                directory.detailsBetween(sector.id(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        // then
        assertThat(details).isEmpty();
    }

    @Test
    @DisplayName("reads the cages of each report of the interval by battery and then by number")
    void givenCagesWhoseNumbersGoAgainstTheBatteries_whenReadingTheInterval_thenOrderByBatteryFirst() {
        // given
        UUID sectorId = fixtures.activeSector();
        fixtures.activeCage(sectorId, "B", 1, 50);
        fixtures.activeCage(sectorId, "A", 7, 48);
        FarmSector sector = farmStructure.sectorOf(SectorId.of(sectorId)).orElseThrow();
        savedOn(sector, "2026-09-22");

        // when
        List<DailyReportDetail> details =
                directory.detailsBetween(sector.id(), LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 22));

        // then
        assertThat(details.getFirst().cages()).extracting(ReportCageDetail::code).containsExactly("A-07", "B-01");
    }
}
