package io.github.ovyx.production.integration;

import static io.github.ovyx.production.domain.model.DailyReportTestDataBuilder.MARINA;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.production.application.dailyreport.DailyReportDetail;
import io.github.ovyx.production.application.dailyreport.DailyReportDirectory;
import io.github.ovyx.production.application.dailyreport.OpenDailyReportCommand;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.shared.application.Dispatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Leituras simultâneas do detalhe (revisão da T110): cada uma usa uma conexão por vez. Com a consulta das
 * gaiolas aninhada na leitura do relatório, cada detalhe segurava duas conexões, e mais leituras que o
 * pool travavam a API até o tempo de espera do pool — a mesma classe de falha do T285 da 002.
 */
@DisplayName("Daily report concurrent reads")
class DailyReportConcurrentReadsIT extends IntegrationTestSupport {

    private static final int READERS = 30;

    @Autowired
    private Dispatcher dispatcher;

    @Autowired
    private DailyReportDirectory directory;

    @Autowired
    private JdbcTemplate jdbc;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(READERS);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    @DisplayName("reads the detail thirty times at once, more than the pool has, and answers all of them")
    void givenThirtySimultaneousReaders_whenReadingTheDetail_thenAnswerEveryOne() throws Exception {
        // given
        UUID sectorId = new ProductionFixtures(jdbc).sectorWithTwoCages();
        DailyReportId reportId = dispatcher
                .dispatch(new OpenDailyReportCommand(sectorId.toString(), "2026-09-24", "06:30", "98", "20", null, MARINA))
                .value();
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Optional<DailyReportDetail>>> readings = new ArrayList<>();
        for (int reader = 0; reader < READERS; reader++) {
            readings.add(executor.submit(() -> {
                start.await();
                return directory.findDetail(SectorId.of(sectorId), reportId);
            }));
        }

        // when
        start.countDown();

        // then
        for (Future<Optional<DailyReportDetail>> reading : readings) {
            assertThat(reading.get(20, TimeUnit.SECONDS)).hasValueSatisfying(detail -> assertThat(detail.cages()).hasSize(2));
        }
    }
}
