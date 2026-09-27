package io.github.ovyx.production.integration;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.MARINA;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.production.application.dailyreport.OpenDailyReportCommand;
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
 * Duas aberturas ao mesmo tempo (R-005, SC-004): o índice único recusa a segunda gravação, e o
 * despachante repete o comando, que então recebe do agregado o conflito com o código da regra.
 */
@DisplayName("Daily report concurrency")
class DailyReportConcurrencyIT extends IntegrationTestSupport {

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
    @DisplayName("of two simultaneous openings of the same day, one goes through and the other is a conflict")
    void givenTheSameDay_whenOpeningTwoReportsAtOnce_thenAcceptOneAndRefuseTheOtherAsAlreadyExisting()
            throws Exception {
        // given
        UUID sectorId = new ProductionFixtures(jdbc).sectorWithTwoCages();
        OpenDailyReportCommand opening =
                new OpenDailyReportCommand(sectorId.toString(), "2026-09-24", "06:30", "98", "20", null, MARINA);

        // when
        List<Result<DailyReportId>> results =
                simultaneously(() -> dispatcher.dispatch(opening), () -> dispatcher.dispatch(opening));

        // then
        assertThat(results).filteredOn(Result::isSuccess).hasSize(1);
        assertThat(results)
                .filteredOn(result -> !result.isSuccess())
                .singleElement()
                .satisfies(refused -> assertThat(refused.error().code()).isEqualTo("DAILY_REPORT_ALREADY_EXISTS"));
        assertThat(jdbc.queryForObject(
                        "select count(*) from daily_report where sector_id = ? and collection_date = date '2026-09-24'",
                        Integer.class,
                        sectorId))
                .isOne();
    }
}
