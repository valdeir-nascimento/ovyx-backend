package io.github.ovyx.production.integration;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.aDailyReport;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.production.domain.model.DailyReport;
import io.github.ovyx.production.domain.model.FarmSector;
import io.github.ovyx.production.domain.model.ReportCage;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.production.domain.port.DailyReportRepository;
import io.github.ovyx.production.domain.port.FarmStructure;
import io.github.ovyx.production.domain.valueobject.EggGrades;
import io.github.ovyx.production.domain.valueobject.ProductionEntry;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A produção das gaiolas no repositório, contra PostgreSQL real (US2, R-003): cada gravação e cada
 * leitura passam pelo adaptador, em transações separadas, e o que volta vem do banco.
 */
@DisplayName("Daily report production persistence")
class DailyReportProductionIT extends IntegrationTestSupport {

    private static final Instant RECORDED_AT = Instant.parse("2026-09-24T11:05:12Z");
    private static final EggGrades.Raw FIVE_GRADED = new EggGrades.Raw(null, "1", "1", "2", "1", null);

    @Autowired
    private DailyReportRepository repository;

    @Autowired
    private FarmStructure farmStructure;

    @Autowired
    private JdbcTemplate jdbc;

    private FarmSector sector;
    private DailyReport report;

    @BeforeEach
    void setUp() {
        sector = farmStructure
                .sectorOf(SectorId.of(new ProductionFixtures(jdbc).sectorWithTwoCages()))
                .orElseThrow();
        report = aDailyReport().in(sector).withRoster(repository).build();
        repository.save(report);
    }

    private CageId a01() {
        return report.cages().get(0).cageId();
    }

    private CageId b07() {
        return report.cages().get(1).cageId();
    }

    private DailyReport readBack() {
        return repository.findById(sector.id(), report.id()).orElseThrow();
    }

    private Map<String, Object> rowOf(CageId cageId) {
        return jdbc.queryForMap(
                "select eggs, small, jumbo, dirty, cracked, blood_spot, abnormal from report_cage"
                        + " where report_id = ? and cage_id = ?",
                report.id().value(),
                cageId.value());
    }

    private long versionOf() {
        return jdbc.queryForObject("select version from daily_report where id = ?", Long.class, report.id().value());
    }

    @Test
    @DisplayName("stores the production of a cage in its seven columns and reads it back")
    void givenProductionOfOneCage_whenSavingAndReadingBack_thenFindItInTheSevenColumns() {
        // given
        DailyReport loaded = readBack();
        loaded.recordProduction(sector, b07(), "45", FIVE_GRADED, JOAO, RECORDED_AT);

        // when
        repository.save(loaded);

        // then
        assertThat(rowOf(b07()))
                .containsEntry("eggs", 45)
                .containsEntry("small", 0)
                .containsEntry("jumbo", 1)
                .containsEntry("dirty", 1)
                .containsEntry("cracked", 2)
                .containsEntry("blood_spot", 1)
                .containsEntry("abnormal", 0);
        assertThat(readBack().cage(b07()).flatMap(ReportCage::production))
                .contains(new ProductionEntry(45, new EggGrades(0, 1, 1, 2, 1, 0)));
    }

    @Test
    @DisplayName("reads back a report with half of the cages recorded as it was: one with production, one without")
    void givenOneOfTwoCagesRecorded_whenReadingBack_thenFindBothHalves() {
        // given
        DailyReport loaded = readBack();
        loaded.recordProduction(sector, a01(), "44", FIVE_GRADED, JOAO, RECORDED_AT);

        // when
        repository.save(loaded);

        // then
        DailyReport read = readBack();
        assertThat(read.cage(a01()).map(ReportCage::hasProduction)).contains(true);
        assertThat(read.cage(b07()).map(ReportCage::hasProduction)).contains(false);
        assertThat(read.pendingCages()).isEqualTo(1);
        assertThat(rowOf(b07())).containsEntry("eggs", null).containsEntry("abnormal", null);
    }

    @Test
    @DisplayName("replaces the production of a cage when it is recorded again")
    void givenRecordedProduction_whenRecordingAgainAndSaving_thenStoreTheNewOne() {
        // given
        DailyReport first = readBack();
        first.recordProduction(sector, b07(), "45", FIVE_GRADED, JOAO, RECORDED_AT);
        repository.save(first);
        DailyReport second = readBack();
        second.recordProduction(sector, b07(), "40", new EggGrades.Raw(null, null, null, null, null, null), JOAO, RECORDED_AT);

        // when
        repository.save(second);

        // then
        assertThat(rowOf(b07())).containsEntry("eggs", 40).containsEntry("cracked", 0).containsEntry("blood_spot", 0);
    }

    @Test
    @DisplayName("stores the production recorded before the first save of the report")
    void givenProductionRecordedBeforeTheFirstSave_whenSaving_thenStoreIt() {
        // given
        FarmSector other = farmStructure
                .sectorOf(SectorId.of(new ProductionFixtures(jdbc).sectorWithTwoCages()))
                .orElseThrow();
        DailyReport fresh = aDailyReport().in(other).withRoster(repository).build();
        CageId first = fresh.cages().get(0).cageId();
        fresh.recordProduction(other, first, "44", FIVE_GRADED, JOAO, RECORDED_AT);

        // when
        repository.save(fresh);

        // then
        assertThat(repository.findById(other.id(), fresh.id()).orElseThrow().cage(first).flatMap(ReportCage::production))
                .contains(new ProductionEntry(44, new EggGrades(0, 1, 1, 2, 1, 0)));
    }

    @Test
    @DisplayName("stores who recorded the production and when, and moves the version of the report")
    void givenProductionOfOneCage_whenSaving_thenStoreTheCorrectionAndMoveTheVersion() {
        // given
        long before = versionOf();
        DailyReport loaded = readBack();
        loaded.recordProduction(sector, b07(), "45", FIVE_GRADED, JOAO, RECORDED_AT);

        // when
        repository.save(loaded);

        // then
        DailyReport read = readBack();
        assertThat(read.lastCorrectedBy()).contains(JOAO);
        assertThat(read.lastCorrectedAt()).contains(RECORDED_AT);
        assertThat(versionOf()).isGreaterThan(before);
    }
}
