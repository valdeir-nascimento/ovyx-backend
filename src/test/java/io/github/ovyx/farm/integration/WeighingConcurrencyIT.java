package io.github.ovyx.farm.integration;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aUniqueSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.farm.application.weighing.RecordWeighingCommand;
import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.farm.domain.port.SectorRepository;
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
 * Dois registros da mesma gaiola no mesmo dia, ao mesmo tempo (FR-016 e SC-003 da 005; R-005): o índice
 * único recusa o segundo, o dispatcher repete, e a repetição encontra o dia ocupado.
 */
@DisplayName("Weighing concurrency")
class WeighingConcurrencyIT extends IntegrationTestSupport {

    private static final Actor MARINA = new Actor(UUID.fromString("5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22"), "Marina Alves");
    private static final Actor ADMIN = new Actor(UUID.fromString("1c3e5a7c-9d1f-4b3d-8e5a-7c9d1f3b5e88"), "Administrador");

    @Autowired
    private Dispatcher dispatcher;

    @Autowired
    private SectorRepository sectors;

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
    @DisplayName("of two simultaneous weighings of the same cage on the same day, one goes through and the other is a conflict")
    void givenTheSameCageAndDay_whenRecordingTwoWeighingsAtOnce_thenAcceptOneAndRefuseTheOtherAsDayInUse()
            throws Exception {
        // given
        Sector sector = aUniqueSector().withCage("A", 1, 48).build();
        sectors.save(sector);
        String sectorId = sector.id().toString();
        String cageId = sector.cages().get(0).id().toString();
        RecordWeighingCommand first = new RecordWeighingCommand(sectorId, cageId, "2026-09-24", "161", MARINA);
        RecordWeighingCommand second = new RecordWeighingCommand(sectorId, cageId, "2026-09-24", "158", ADMIN);

        // when
        List<Result<WeighingId>> results =
                simultaneously(() -> dispatcher.dispatch(first), () -> dispatcher.dispatch(second));

        // then
        assertThat(results).filteredOn(Result::isSuccess).hasSize(1);
        assertThat(results)
                .filteredOn(Result::isFailure)
                .extracting(result -> result.error().code())
                .containsExactly("WEIGHING_DATE_IN_USE");
        assertThat(jdbc.queryForObject(
                        "select count(*) from weighing where cage_id = ?::uuid and status = 'VALID'",
                        Integer.class,
                        cageId))
                .isEqualTo(1);
    }
}
