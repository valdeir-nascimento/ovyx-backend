package io.github.ovyx.farm.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.farm.application.cage.RegisterCageCommand;
import io.github.ovyx.farm.application.sector.RegisterSectorCommand;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
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
 * Duas escritas ao mesmo tempo contra PostgreSQL real (spec, casos de borda; R-003, R-005).
 *
 * <p>As duas passam pela checagem do agregado antes de qualquer uma gravar; o índice único parcial
 * recusa a segunda, e o despachante repete o comando, que então encontra o conflito e recusa com o
 * código certo. Cada teste se repete porque uma corrida pode passar por sorte numa rodada só.
 */
@DisplayName("Sector concurrency")
class SectorConcurrencyIT extends IntegrationTestSupport {

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
    @DisplayName("of two simultaneous registrations of the same name, one goes through and the other is a conflict")
    void givenTheSameName_whenRegisteringTwoSectorsAtOnce_thenAcceptOneAndRefuseTheOtherAsNameInUse()
            throws Exception {
        // given
        String name = "Corrida " + UUID.randomUUID().toString().substring(0, 8);
        RegisterSectorCommand registration = new RegisterSectorCommand(name, null);

        // when
        List<Result<SectorId>> results =
                simultaneously(() -> dispatcher.dispatch(registration), () -> dispatcher.dispatch(registration));

        // then
        assertThat(results).filteredOn(Result::isSuccess).hasSize(1);
        assertThat(results)
                .filteredOn(Result::isFailure)
                .extracting(result -> result.error().code())
                .containsExactly("SECTOR_NAME_IN_USE");
        assertThat(jdbc.queryForObject("select count(*) from sector where name = ?", Integer.class, name))
                .isEqualTo(1);
    }

    /** Um setor recém-cadastrado, para as corridas nas gaiolas dele. */
    private String registeredSector() {
        Result<SectorId> registered = dispatcher.dispatch(
                new RegisterSectorCommand("Corrida " + UUID.randomUUID().toString().substring(0, 8), null));
        return registered.value().toString();
    }

    @RepeatedTest(value = 10, name = "round {currentRepetition} of {totalRepetitions}")
    @DisplayName("of two simultaneous registrations of the same cage, one goes through and the other is a conflict")
    void givenTheSameCage_whenRegisteringItTwiceAtOnce_thenAcceptOneAndRefuseTheOtherAsAlreadyExisting()
            throws Exception {
        // given
        String sectorId = registeredSector();
        RegisterCageCommand registration = new RegisterCageCommand(sectorId, "B", "7", "50");

        // when
        List<Result<CageId>> results =
                simultaneously(() -> dispatcher.dispatch(registration), () -> dispatcher.dispatch(registration));

        // then
        assertThat(results).filteredOn(Result::isSuccess).hasSize(1);
        assertThat(results)
                .filteredOn(Result::isFailure)
                .extracting(result -> result.error().code())
                .containsExactly("CAGE_ALREADY_EXISTS");
        assertThat(jdbc.queryForObject("select count(*) from cage where sector_id = ?::uuid", Integer.class, sectorId))
                .isEqualTo(1);
    }

    @RepeatedTest(value = 10, name = "round {currentRepetition} of {totalRepetitions}")
    @DisplayName("two simultaneous registrations of different cages of one sector both go through")
    void givenDifferentCages_whenRegisteringThemAtOnceInOneSector_thenAcceptBoth() throws Exception {
        // given
        // As duas carregam o setor na mesma versão; a segunda a gravar encontra a versão mudada, e o
        // despachante a repete com o setor relido (R-003).
        String sectorId = registeredSector();

        // when
        List<Result<CageId>> results = simultaneously(
                () -> dispatcher.dispatch(new RegisterCageCommand(sectorId, "A", "1", "50")),
                () -> dispatcher.dispatch(new RegisterCageCommand(sectorId, "B", "1", "50")));

        // then
        assertThat(results).allMatch(Result::isSuccess);
        assertThat(jdbc.queryForObject("select count(*) from cage where sector_id = ?::uuid", Integer.class, sectorId))
                .isEqualTo(2);
    }
}
