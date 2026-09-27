package io.github.ovyx.production.integration;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.MARINA;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.production.application.dailyreport.OpenDailyReportCommand;
import io.github.ovyx.production.application.dailyreport.RecordFeedBySuggestionCommand;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Result;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Duas pessoas lançam a ração do setor pela sugestão ao mesmo tempo (FR-022; R-003 da 004): a segunda
 * espera a primeira, pelo bloqueio da linha do relatório, e encontra as gaiolas já lançadas. As duas
 * terminam bem, e cada gaiola fica com um lançamento só, o da primeira.
 */
@DisplayName("Daily report concurrent feed")
class DailyReportConcurrentFeedIT extends IntegrationTestSupport {

    @Autowired
    private Dispatcher dispatcher;

    @Autowired
    private JdbcTemplate jdbc;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    /** Roda as duas ações ao mesmo tempo: as duas esperam o mesmo sinal para partir. */
    private <T> List<T> simultaneously(Callable<T> first, Callable<T> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Future<T> one = executor.submit(() -> {
            start.await();
            return first.call();
        });
        Future<T> other = executor.submit(() -> {
            start.await();
            return second.call();
        });
        start.countDown();
        return List.of(one.get(60, TimeUnit.SECONDS), other.get(60, TimeUnit.SECONDS));
    }

    @RepeatedTest(value = 10, name = "round {currentRepetition} of {totalRepetitions}")
    @DisplayName("of two simultaneous feeds by suggestion, both succeed and each cage is fed once")
    void givenTwoPeopleFeedingTheSectorAtOnce_whenBothConfirm_thenFeedEachCageOnce() throws Exception {
        // given
        ProductionFixtures fixtures = new ProductionFixtures(jdbc);
        UUID sectorId = fixtures.sectorWithTwoCages();
        UUID posturaPlus = fixtures.posturaPlus();
        UUID recria = fixtures.formula("3.10", 24, "ACTIVE");
        UUID reportId = dispatcher
                .dispatch(new OpenDailyReportCommand(sectorId.toString(), "2026-09-24", "06:30", "98", "20", null, MARINA))
                .value()
                .value();

        // when
        List<Result<DailyReportId>> results = simultaneously(
                () -> dispatcher.dispatch(new RecordFeedBySuggestionCommand(
                        sectorId.toString(), reportId.toString(), posturaPlus.toString(), JOAO)),
                () -> dispatcher.dispatch(new RecordFeedBySuggestionCommand(
                        sectorId.toString(), reportId.toString(), recria.toString(), MARINA)));

        // then
        assertThat(results).allSatisfy(result -> assertThat(result.isSuccess()).isTrue());
        List<UUID> formulas = jdbc.queryForList(
                "select feed_formula_id from report_cage where report_id = ? order by battery, number",
                UUID.class,
                reportId);
        assertThat(formulas).hasSize(2).doesNotContainNull();
        assertThat(formulas.get(0)).as("as duas gaiolas vêm do mesmo lançamento").isEqualTo(formulas.get(1));
    }
}
