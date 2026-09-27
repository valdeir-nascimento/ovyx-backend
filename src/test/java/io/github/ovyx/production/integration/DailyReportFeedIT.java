package io.github.ovyx.production.integration;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.aDailyReport;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.production.domain.model.DailyReport;
import io.github.ovyx.production.domain.model.FarmSector;
import io.github.ovyx.production.domain.model.FeedFormulaChoice;
import io.github.ovyx.production.domain.model.FeedFormulaId;
import io.github.ovyx.production.domain.model.ReportCage;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.production.domain.port.DailyReportRepository;
import io.github.ovyx.production.domain.port.FarmStructure;
import io.github.ovyx.production.domain.port.FeedCatalog;
import io.github.ovyx.production.domain.valueobject.FeedEntry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A ração das gaiolas gravada e relida pelo repositório do relatório (R-003 e R-012 da 004): as quatro
 * colunas juntas, o preço e o esperado como estavam no lançamento.
 */
@DisplayName("Daily report feed")
class DailyReportFeedIT extends IntegrationTestSupport {

    private static final Instant RECORDED_AT = Instant.parse("2026-09-24T11:20:05Z");

    @Autowired
    private DailyReportRepository repository;

    @Autowired
    private FarmStructure farmStructure;

    @Autowired
    private FeedCatalog catalog;

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

    private FeedFormulaChoice chosen(UUID formulaId) {
        return FeedFormulaChoice.lookup(formulaId.toString(), catalog);
    }

    @Test
    @DisplayName("saves the feed of every cage in the four columns, and reads it back as it was")
    void givenReportFedBySuggestion_whenSavingAndReadingBack_thenFindTheFeedOfEveryCage() {
        // given
        FarmSector sector = sectorWithTwoCages();
        UUID posturaPlus = fixtures.posturaPlus();
        DailyReport report = aDailyReport().in(sector).withRoster(repository).build();
        repository.save(report);
        DailyReport loaded = repository.findById(sector.id(), report.id()).orElseThrow();
        loaded.recordFeedBySuggestion(sector, chosen(posturaPlus), JOAO, RECORDED_AT);

        // when
        repository.save(loaded);

        // then
        DailyReport read = repository.findById(sector.id(), report.id()).orElseThrow();
        assertThat(read.cages())
                .extracting(cage -> cage.feed().orElseThrow())
                .containsExactly(
                        new FeedEntry(FeedFormulaId.of(posturaPlus), new BigDecimal("2.85"), 28, 1344),
                        new FeedEntry(FeedFormulaId.of(posturaPlus), new BigDecimal("2.85"), 28, 1400));
        Map<String, Object> row = jdbc.queryForMap(
                "select feed_formula_id, feed_price_per_kg, feed_expected_intake, feed_consumption from report_cage"
                        + " where report_id = ? and number = 7",
                report.id().value());
        assertThat(row)
                .containsEntry("feed_formula_id", posturaPlus)
                .containsEntry("feed_price_per_kg", new BigDecimal("2.85"))
                .containsEntry("feed_expected_intake", 28)
                .containsEntry("feed_consumption", 1400);
    }

    @Test
    @DisplayName("keeps the cost of the report when the price of the formula changes afterwards (SC-003)")
    void givenFormulaRepricedAfterTheFeed_whenReadingTheReport_thenKeepThePriceKeptInTheFeed() {
        // given
        FarmSector sector = sectorWithTwoCages();
        UUID posturaPlus = fixtures.posturaPlus();
        DailyReport report = aDailyReport().in(sector).withRoster(repository).build();
        report.recordFeedBySuggestion(sector, chosen(posturaPlus), JOAO, RECORDED_AT);
        repository.save(report);

        // when
        fixtures.repriceFormula(posturaPlus, "3.10");

        // then
        DailyReport read = repository.findById(sector.id(), report.id()).orElseThrow();
        assertThat(read.cages())
                .allSatisfy(cage -> assertThat(cage.feed().orElseThrow().pricePerKg()).isEqualByComparingTo("2.85"));
        assertThat(jdbc.queryForObject(
                        "select sum(feed_consumption * feed_price_per_kg) / 1000 from report_cage where report_id = ?",
                        BigDecimal.class,
                        report.id().value()))
                .isEqualByComparingTo("7.8204");
    }

    @Test
    @DisplayName("reads a cage without feed back without feed")
    void givenReportWithoutFeed_whenReadingItBack_thenFindNoFeed() {
        // given
        FarmSector sector = sectorWithTwoCages();
        DailyReport report = aDailyReport().in(sector).withRoster(repository).build();

        // when
        repository.save(report);

        // then
        assertThat(repository.findById(sector.id(), report.id()).orElseThrow().cages()).noneMatch(ReportCage::hasFeed);
    }
}
