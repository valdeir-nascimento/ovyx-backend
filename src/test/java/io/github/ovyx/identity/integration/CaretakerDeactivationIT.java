package io.github.ovyx.identity.integration;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.application.caretaker.CaretakerDetail;
import io.github.ovyx.identity.application.caretaker.DeactivateCaretakerCommand;
import io.github.ovyx.identity.application.caretaker.FindCaretakerByIdQuery;
import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.identity.domain.port.AccessEventRecorder;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Result;
import java.time.Clock;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A inativacao nao remove nada (FR-018, cenario V-09).
 *
 * <p>Registros de outras areas vao referenciar o responsavel; a linha fica, e a trilha de acesso
 * dele continua consultavel.
 */
@DisplayName("Caretaker deactivation")
class CaretakerDeactivationIT extends IntegrationTestSupport {

    private static final String ROWS_OF_THE_CARETAKER = """
        SELECT count(*)
          FROM caretaker
         WHERE id = ?
        """;

    private static final String ACCESS_EVENTS_OF_THE_CARETAKER = """
        SELECT count(*)
          FROM access_event
         WHERE caretaker_id = ?
        """;

    @Autowired
    private Dispatcher dispatcher;

    @Autowired
    private CaretakerRepository repository;

    @Autowired
    private AccessEventRecorder accessEventRecorder;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private Clock clock;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
    }

    private Caretaker caretakerWithAnAccess() {
        Caretaker caretaker =
                aUniqueCaretaker().withHasher(passwordHasher).withRoster(repository).withClock(clock).build();
        repository.save(caretaker);
        accessEventRecorder.record(
                AccessEvent.granted(caretaker.email().value(), caretaker.id(), "127.0.0.1", clock));
        return caretaker;
    }

    private long count(String sql, CaretakerId id) {
        Long result = jdbc.queryForObject(sql, Long.class, id.value());
        return result == null ? 0 : result;
    }

    @Test
    @DisplayName("keeps the row and the access trail of a deactivated caretaker")
    void givenCaretakerWithAnAccess_whenDeactivating_thenKeepTheRowAndTheAccessTrail() {
        // given
        Caretaker caretaker = caretakerWithAnAccess();

        // when
        dispatcher.dispatch(new DeactivateCaretakerCommand(caretaker.id()));

        // then
        assertThat(count(ROWS_OF_THE_CARETAKER, caretaker.id())).isEqualTo(1);
        assertThat(count(ACCESS_EVENTS_OF_THE_CARETAKER, caretaker.id())).isEqualTo(1);
    }

    @Test
    @DisplayName("a deactivated caretaker stays consultable, marked as inactive")
    void givenDeactivatedCaretaker_whenConsulting_thenFindItAsInactive() {
        // given
        Caretaker caretaker = caretakerWithAnAccess();
        dispatcher.dispatch(new DeactivateCaretakerCommand(caretaker.id()));

        // when
        Result<CaretakerDetail> detail = dispatcher.ask(new FindCaretakerByIdQuery(caretaker.id()));

        // then
        assertThat(detail.value().status()).isEqualTo(CaretakerStatus.INACTIVE);
    }
}
