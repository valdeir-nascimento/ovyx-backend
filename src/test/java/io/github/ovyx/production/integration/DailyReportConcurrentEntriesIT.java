package io.github.ovyx.production.integration;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.JOAO;
import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.MARINA;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.production.application.dailyreport.OpenDailyReportCommand;
import io.github.ovyx.production.application.dailyreport.RecordProductionCommand;
import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Result;
import java.util.ArrayList;
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
 * Lançamentos ao mesmo tempo, em gaiolas diferentes do mesmo relatório (R-003 e R-016): a escrita carrega
 * o relatório com a linha dele bloqueada até o fim da transação, e os lançamentos esperam um ao outro.
 * Todos terminam gravados, sem 409 nem 500, com duas pessoas ou com seis.
 */
@DisplayName("Daily report concurrent entries")
class DailyReportConcurrentEntriesIT extends IntegrationTestSupport {

    @Autowired
    private Dispatcher dispatcher;

    @Autowired
    private JdbcTemplate jdbc;

    private static final int WRITERS = 6;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(WRITERS);
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

    private RecordProductionCommand production(UUID sectorId, UUID reportId, UUID cageId, String eggs) {
        return new RecordProductionCommand(
                sectorId.toString(), reportId.toString(), cageId.toString(), eggs, null, null, null, null, null, null, JOAO);
    }

    @RepeatedTest(value = 10, name = "round {currentRepetition} of {totalRepetitions}")
    @DisplayName("of two simultaneous productions in different cages of the same report, both are recorded")
    void givenTwoCagesOfTheSameReport_whenRecordingBothAtOnce_thenRecordBoth() throws Exception {
        // given
        ProductionFixtures fixtures = new ProductionFixtures(jdbc);
        UUID sectorId = fixtures.activeSector();
        UUID a01 = fixtures.activeCage(sectorId, "A", 1, 48);
        UUID b07 = fixtures.activeCage(sectorId, "B", 7, 50);
        UUID reportId = dispatcher
                .dispatch(new OpenDailyReportCommand(sectorId.toString(), "2026-09-24", "06:30", "98", "20", null, MARINA))
                .value()
                .value();

        // when
        List<Result<CageId>> results = simultaneously(
                () -> dispatcher.dispatch(production(sectorId, reportId, a01, "44")),
                () -> dispatcher.dispatch(production(sectorId, reportId, b07, "45")));

        // then
        assertThat(results).allSatisfy(result -> assertThat(result.isSuccess()).isTrue());
        assertThat(jdbc.queryForList(
                        "select eggs from report_cage where report_id = ? order by battery, number",
                        Integer.class,
                        reportId))
                .containsExactly(44, 45);
    }

    @RepeatedTest(value = 5, name = "round {currentRepetition} of {totalRepetitions}")
    @DisplayName("of six simultaneous productions in different cages of the same report, all are recorded")
    void givenSixCagesOfTheSameReport_whenRecordingAllAtOnce_thenRecordEveryOne() throws Exception {
        // given
        // Uma nova tentativa só não basta a seis pessoas lançando o mesmo dia do mesmo setor: a terceira
        // perdia as duas e virava 500 (revisão da T110). Os lançamentos no mesmo relatório esperam um ao
        // outro, pelo bloqueio da linha do relatório.
        ProductionFixtures fixtures = new ProductionFixtures(jdbc);
        UUID sectorId = fixtures.activeSector();
        List<UUID> cages = new ArrayList<>();
        for (int number = 1; number <= WRITERS; number++) {
            cages.add(fixtures.activeCage(sectorId, "A", number, 50));
        }
        UUID reportId = dispatcher
                .dispatch(new OpenDailyReportCommand(sectorId.toString(), "2026-09-24", "06:30", "300", "20", null, MARINA))
                .value()
                .value();
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Result<CageId>>> recordings = new ArrayList<>();
        for (UUID cage : cages) {
            recordings.add(executor.submit(() -> {
                start.await();
                return dispatcher.dispatch(production(sectorId, reportId, cage, "40"));
            }));
        }

        // when
        start.countDown();

        // then
        for (Future<Result<CageId>> recording : recordings) {
            assertThat(recording.get(60, TimeUnit.SECONDS).isSuccess()).isTrue();
        }
        assertThat(jdbc.queryForObject(
                        "select count(*) from report_cage where report_id = ? and eggs = 40", Integer.class, reportId))
                .isEqualTo(WRITERS);
    }
}
