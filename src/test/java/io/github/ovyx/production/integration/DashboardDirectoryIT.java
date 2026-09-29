package io.github.ovyx.production.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.production.application.dashboard.CageWatch;
import io.github.ovyx.production.application.dashboard.CageWatchReading;
import io.github.ovyx.production.application.dashboard.DashboardDirectory;
import io.github.ovyx.production.application.dashboard.DashboardSector;
import io.github.ovyx.production.application.dashboard.DashboardSectors;
import io.github.ovyx.production.application.dashboard.LatestReport;
import io.github.ovyx.production.application.dashboard.ReferenceWeight;
import io.github.ovyx.production.application.dashboard.ReportDay;
import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import io.github.ovyx.production.domain.model.SectorId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * As leituras do painel direto do banco (R-004 da 006): o cabeçalho, o setor e os relatórios somados por dia.
 * Os relatórios são inseridos por SQL, com as gaiolas lançadas como a tela as deixaria.
 */
@DisplayName("Dashboard directory")
class DashboardDirectoryIT extends IntegrationTestSupport {

    /** Um dia que nenhum outro teste usa, para o "completo hoje" contar só os relatórios daqui. */
    private static final LocalDate TODAY = LocalDate.of(2031, 3, 15);

    @Autowired
    private DashboardDirectory directory;

    @Autowired
    private JdbcTemplate jdbc;

    private ProductionFixtures fixtures;
    private UUID formula;

    @BeforeEach
    void setUp() {
        fixtures = new ProductionFixtures(jdbc);
        formula = fixtures.posturaPlus();
    }

    /** Um relatório com uma gaiola, com produção, ração e o dia sem ocorrência confirmado. */
    private UUID completeReport(UUID sectorId, UUID cageId, LocalDate date) {
        UUID reportId = fixtures.report(sectorId, date, 48, true);
        fixtures.cageIn(reportId, cageId, 48);
        fixtures.production(reportId, cageId, 44, 0, 0, 0, 0, 0, 0);
        fixtures.feed(reportId, cageId, formula, 1344);
        return reportId;
    }

    // ---------------------------------------------------------------- cabeçalho

    @Test
    @DisplayName("counts the active sectors and the ones with the report of today complete, and gives the tabs")
    void givenSectorsInEveryState_whenReadingTheOverview_thenCountAndListOnlyTheRightOnes() {
        // given
        DashboardSectors before = directory.overview(TODAY);
        UUID complete = fixtures.sectorNamed("zz-painel a", "ACTIVE");
        completeReport(complete, fixtures.activeCage(complete, "A", 1, 48), TODAY);
        UUID pending = fixtures.sectorNamed("ZZ-PAINEL B", "ACTIVE");
        UUID pendingReport = fixtures.report(pending, TODAY, 48, false);
        UUID pendingCage = fixtures.activeCage(pending, "A", 1, 48);
        fixtures.cageIn(pendingReport, pendingCage, 48);
        fixtures.production(pendingReport, pendingCage, 44, 0, 0, 0, 0, 0, 0);
        UUID withoutReport = fixtures.sectorNamed("zz-painel c", "ACTIVE");
        UUID inactive = fixtures.sectorNamed("zz-painel d", "INACTIVE");
        completeReport(inactive, fixtures.cage(inactive, "A", 1, 48, "INACTIVE"), TODAY);

        // when
        DashboardSectors after = directory.overview(TODAY);

        // then
        assertThat(after.activeSectors() - before.activeSectors()).isEqualTo(3);
        assertThat(after.completeToday() - before.completeToday()).isEqualTo(1);
        assertThat(after.sectors())
                .extracting(DashboardSector::id)
                .containsSubsequence(complete, pending)
                .doesNotContain(withoutReport, inactive);
    }

    @Test
    @DisplayName("takes the report of today with no death and no confirmation as pending, and one death as recorded")
    void givenMortalityInTwoWays_whenReadingTheOverview_thenFollowTheRuleOfTheReport() {
        // given
        DashboardSectors before = directory.overview(TODAY.plusDays(1));
        UUID confirmedByDeath = fixtures.sectorNamed("zz-painel e", "ACTIVE");
        UUID cage = fixtures.activeCage(confirmedByDeath, "A", 1, 48);
        UUID reportId = fixtures.report(confirmedByDeath, TODAY.plusDays(1), 48, false);
        fixtures.cageIn(reportId, cage, 48);
        fixtures.production(reportId, cage, 44, 0, 0, 0, 0, 0, 0);
        fixtures.feed(reportId, cage, formula, 1344);
        UUID notConfirmed = fixtures.sectorNamed("zz-painel f", "ACTIVE");
        UUID otherCage = fixtures.activeCage(notConfirmed, "A", 1, 48);
        UUID otherReport = fixtures.report(notConfirmed, TODAY.plusDays(1), 48, false);
        fixtures.cageIn(otherReport, otherCage, 48);
        fixtures.production(otherReport, otherCage, 44, 0, 0, 0, 0, 0, 0);
        fixtures.feed(otherReport, otherCage, formula, 1344);
        fixtures.mortality(otherReport, otherCage, 0, 0);
        fixtures.mortality(reportId, cage, 1, 0);

        // when
        DashboardSectors after = directory.overview(TODAY.plusDays(1));

        // then
        assertThat(after.completeToday() - before.completeToday()).isEqualTo(1);
    }

    // ---------------------------------------------------------------- setor

    @Test
    @DisplayName("finds the sector of the dashboard, and nothing for an unknown one")
    void givenSector_whenReadingIt_thenFindItAndNothingForAnother() {
        // given
        UUID sectorId = fixtures.activeSector();

        // when / then
        assertThat(directory.sector(SectorId.of(sectorId))).hasValueSatisfying(sector -> {
            assertThat(sector.name()).isEqualTo(fixtures.nameOf(sectorId));
            assertThat(sector.status()).isEqualTo("ACTIVE");
        });
        assertThat(directory.sector(SectorId.of(UUID.randomUUID()))).isEmpty();
    }

    // ---------------------------------------------------------------- relatórios somados

    @Test
    @DisplayName("sums each report of the window, with the cages, the eggs, each grade, the exact cost and the removals")
    void givenReportsInAndOutOfTheWindow_whenReadingTheDays_thenSumOnlyTheOnesOfTheSectorInTheWindow() {
        // given
        UUID sectorId = fixtures.activeSector();
        UUID a01 = fixtures.activeCage(sectorId, "A", 1, 48);
        UUID b07 = fixtures.activeCage(sectorId, "B", 7, 50);
        UUID today = fixtures.report(sectorId, TODAY, 98, false);
        fixtures.cageIn(today, a01, 48);
        fixtures.cageIn(today, b07, 50);
        fixtures.production(today, a01, 44, 1, 2, 1, 1, 0, 0);
        fixtures.production(today, b07, 45, 0, 1, 1, 2, 1, 0);
        fixtures.mortality(today, a01, 1, 0);
        fixtures.feed(today, a01, formula, 1344);
        fixtures.feed(today, b07, formula, 1400);
        UUID yesterday = fixtures.report(sectorId, TODAY.minusDays(1), 98, true);
        fixtures.cageIn(yesterday, a01, 48);
        fixtures.cageIn(yesterday, b07, 50);
        fixtures.production(yesterday, a01, 40, 0, 0, 0, 0, 0, 0);
        fixtures.feed(yesterday, a01, formula, 1344);
        completeReport(sectorId, a01, TODAY.minusDays(14));
        UUID other = fixtures.activeSector();
        completeReport(other, fixtures.activeCage(other, "A", 1, 48), TODAY);

        // when
        List<ReportDay> days = directory.reportDays(SectorId.of(sectorId), TODAY.minusDays(13), TODAY);

        // then
        assertThat(days).extracting(ReportDay::date).containsExactly(TODAY.minusDays(1), TODAY);
        ReportDay full = days.get(1);
        assertThat(full.reportId()).isEqualTo(today);
        assertThat(full.openingBirdCount()).isEqualTo(98);
        assertThat(full.cages()).isEqualTo(2);
        assertThat(full.cagesWithProduction()).isEqualTo(2);
        assertThat(full.cagesWithFeed()).isEqualTo(2);
        assertThat(full.eggs()).isEqualTo(89);
        assertThat(List.of(full.small(), full.jumbo(), full.dirty(), full.cracked(), full.bloodSpot(), full.abnormal()))
                .containsExactly(1, 3, 2, 3, 1, 0);
        assertThat(full.exactFeedCost()).isEqualByComparingTo(new BigDecimal("7820.40"));
        assertThat(full.removedBirds()).isEqualTo(1);
        assertThat(full.noMortalityConfirmed()).isFalse();
        ReportDay partial = days.get(0);
        assertThat(partial.cagesWithProduction()).isEqualTo(1);
        assertThat(partial.cagesWithFeed()).isEqualTo(1);
        assertThat(partial.eggs()).isEqualTo(40);
        assertThat(partial.noMortalityConfirmed()).isTrue();
    }

    // ---------------------------------------------------------------- gaiolas para os alertas (US3)

    private void weighing(UUID sectorId, UUID cageId, LocalDate day, String weight, String status) {
        jdbc.update(
                "insert into weighing (id, sector_id, cage_id, weighed_on, average_weight, status, recorded_by_id,"
                        + " recorded_by_name, recorded_at, voided_by_id, voided_by_name, voided_at)"
                        + " values (?, ?, ?, ?, ?, ?, ?, 'Marina Alves', now(), ?, ?, ?)",
                UUID.randomUUID(),
                sectorId,
                cageId,
                day,
                new BigDecimal(weight),
                status,
                UUID.randomUUID(),
                "VOIDED".equals(status) ? UUID.randomUUID() : null,
                "VOIDED".equals(status) ? "Marina Alves" : null,
                "VOIDED".equals(status) ? java.sql.Timestamp.from(java.time.Instant.now()) : null);
    }

    @Test
    @DisplayName("watches the active cages of the sector: the removals of today, the last 3 reports and the last weighing")
    void givenCagesWithHistory_whenWatchingThem_thenBringTheNumbersOfTheAlerts() {
        // given
        UUID sectorId = fixtures.activeSector();
        jdbc.update("update sector set reference_weight_min = 155, reference_weight_max = 175 where id = ?", sectorId);
        UUID a01 = fixtures.activeCage(sectorId, "A", 1, 50);
        UUID b07 = fixtures.activeCage(sectorId, "B", 7, 50);
        UUID inactive = fixtures.cage(sectorId, "C", 3, 50, "INACTIVE");
        int[] eggsOfA01 = {40, 41, 42, 43, 44};
        for (int back = 4; back >= 0; back--) {
            UUID report = fixtures.report(sectorId, TODAY.minusDays(back), 150, true);
            fixtures.cageIn(report, a01, 50);
            fixtures.cageIn(report, b07, 50);
            fixtures.cageIn(report, inactive, 50);
            fixtures.production(report, a01, eggsOfA01[4 - back], 0, 0, 0, 0, 0, 0);
            if (back != 1) {
                fixtures.production(report, b07, 45, 0, 0, 0, 0, 0, 0);
            }
            if (back == 0) {
                fixtures.mortality(report, a01, 2, 1);
            }
        }
        weighing(sectorId, a01, TODAY.minusDays(7), "158.0", "VALID");
        weighing(sectorId, a01, TODAY.minusDays(1), "150.8", "VALID");
        weighing(sectorId, a01, TODAY, "999.0", "VOIDED");
        UUID other = fixtures.activeSector();
        UUID otherCage = fixtures.activeCage(other, "A", 1, 50);
        fixtures.completeReportOf(other, otherCage, TODAY, formula);

        // when
        CageWatchReading reading = directory.cageWatch(SectorId.of(sectorId), TODAY);

        // then
        assertThat(reading.cages()).extracting(CageWatch::code).containsExactly("A-01", "B-07");
        CageWatch first = reading.cages().get(0);
        assertThat(first.removedToday()).isEqualTo(3);
        assertThat(first.recentReports()).isEqualTo(3);
        assertThat(first.recentEggs()).isEqualTo(42 + 43 + 44);
        assertThat(first.recentBirds()).isEqualTo(150);
        assertThat(first.lastWeighedOn()).isEqualTo(TODAY.minusDays(1));
        assertThat(first.lastWeight()).isEqualByComparingTo("150.8");
        CageWatch second = reading.cages().get(1);
        assertThat(second.removedToday()).isZero();
        assertThat(second.recentReports()).isEqualTo(3);
        assertThat(second.recentEggs()).isEqualTo(135);
        assertThat(second.lastWeighedOn()).isNull();
        assertThat(reading.range()).isEqualTo(new ReferenceWeight(155, 175));
    }

    @Test
    @DisplayName("takes the mortality of the 7 days before today as the baseline, and nothing of today or before")
    void givenRemovalsAroundTheWeek_whenWatchingTheCages_thenSumOnlyTheSevenDaysBefore() {
        // given
        UUID sectorId = fixtures.activeSector();
        UUID a01 = fixtures.activeCage(sectorId, "A", 1, 50);
        UUID b07 = fixtures.activeCage(sectorId, "B", 7, 50);
        for (int back : new int[] {0, 1, 7, 8}) {
            UUID report = fixtures.report(sectorId, TODAY.minusDays(back), 100, false);
            fixtures.cageIn(report, a01, 50);
            fixtures.cageIn(report, b07, 50);
            fixtures.mortality(report, a01, 1, 1);
        }

        // when
        CageWatchReading reading = directory.cageWatch(SectorId.of(sectorId), TODAY);

        // then
        assertThat(reading.baseline().removedBirds()).isEqualTo(4);
        assertThat(reading.baseline().cageDays()).isEqualTo(4);
        assertThat(reading.range()).isNull();
    }

    // ---------------------------------------------------------------- últimos relatórios (US4)

    @Test
    @DisplayName("gives the latest reports of the sector, newest first, with the entries and how each stands")
    void givenSixReports_whenReadingTheLatest_thenGiveTheFourNewest() {
        // given
        UUID sectorId = fixtures.activeSector();
        UUID a01 = fixtures.activeCage(sectorId, "A", 1, 48);
        UUID b07 = fixtures.activeCage(sectorId, "B", 7, 50);
        for (int back = 5; back >= 1; back--) {
            fixtures.completeReportOf(sectorId, a01, TODAY.minusDays(back), formula);
        }
        UUID today = fixtures.report(sectorId, TODAY, 98, false);
        fixtures.cageIn(today, a01, 48);
        fixtures.cageIn(today, b07, 50);
        fixtures.production(today, a01, 44, 0, 0, 0, 0, 0, 0);
        fixtures.production(today, b07, 45, 0, 0, 0, 0, 0, 0);
        fixtures.mortality(today, b07, 1, 1);
        fixtures.feed(today, a01, formula, 1344);
        UUID other = fixtures.activeSector();
        fixtures.completeReportOf(other, fixtures.activeCage(other, "A", 1, 48), TODAY.plusDays(1), formula);

        // when
        List<LatestReport> latest = directory.latestReports(SectorId.of(sectorId), 4);

        // then
        assertThat(latest).extracting(LatestReport::collectionDate).containsExactly(
                TODAY, TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(3));
        LatestReport first = latest.get(0);
        assertThat(first.id()).isEqualTo(today);
        assertThat(first.collectionTime()).isEqualTo(java.time.LocalTime.of(6, 30));
        assertThat(first.openedBy().name()).isEqualTo("Marina Alves");
        assertThat(first.collectedEggs()).isEqualTo(89);
        assertThat(first.removedBirds()).isEqualTo(2);
        assertThat(first.productionStatus()).isEqualTo(ProductionStatus.COMPLETE);
        assertThat(first.feedStatus()).isEqualTo(FeedStatus.PENDING);
        assertThat(first.mortalityStatus()).isEqualTo(MortalityStatus.RECORDED);
        assertThat(latest.get(1).feedStatus()).isEqualTo(FeedStatus.COMPLETE);
    }

    // ---------------------------------------------------------------- mutação (T045)

    @Test
    @DisplayName("takes a report of today without the feed of a cage as not complete")
    void givenReportWithoutFeed_whenReadingTheOverview_thenNotCountItAsComplete() {
        // given
        LocalDate day = TODAY.plusDays(2);
        DashboardSectors before = directory.overview(day);
        UUID complete = fixtures.activeSector();
        completeReport(complete, fixtures.activeCage(complete, "A", 1, 48), day);
        UUID withoutFeed = fixtures.activeSector();
        UUID cage = fixtures.activeCage(withoutFeed, "A", 1, 48);
        UUID report = fixtures.report(withoutFeed, day, 48, true);
        fixtures.cageIn(report, cage, 48);
        fixtures.production(report, cage, 44, 0, 0, 0, 0, 0, 0);

        // when
        DashboardSectors after = directory.overview(day);

        // then
        assertThat(after.completeToday() - before.completeToday()).isEqualTo(1);
    }

    @Test
    @DisplayName("leaves out of the days a report after the last day asked")
    void givenReportAfterTheWindow_whenReadingTheDays_thenLeaveItOut() {
        // given
        UUID sectorId = fixtures.activeSector();
        UUID a01 = fixtures.activeCage(sectorId, "A", 1, 48);
        completeReport(sectorId, a01, TODAY);
        completeReport(sectorId, a01, TODAY.plusDays(1));

        // when
        List<ReportDay> days = directory.reportDays(SectorId.of(sectorId), TODAY.minusDays(13), TODAY);

        // then
        assertThat(days).extracting(ReportDay::date).containsExactly(TODAY);
    }
}
