package io.github.ovyx.identity.infrastructure.bootstrap;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.Dispatcher;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A instalacao que ficou sem administrador ativo volta a subir, contra PostgreSQL real (FR-025).
 *
 * <p>Antes, a semeadura tentava cadastrar de novo o administrador inicial, esbarrava no CPF dele —
 * gravado, mas inativo — e a aplicacao recusava subir: nao havia saida sem mexer no banco a mao.
 *
 * <p>O administrador semeado pelo perfil de teste fica de fora, porque outro IT troca a senha dele.
 * Este usa um administrador proprio e roda o inicializador com o CPF dele.
 */
@DisplayName("Bootstrap administrator recovery")
class BootstrapAdministratorRecoveryIT extends IntegrationTestSupport {

    private static final String PROVISIONAL_PASSWORD = "RecuperarAcesso2026";

    private static final String OTHER_ACTIVE_ADMINISTRATORS = """
        SELECT id
          FROM caretaker
         WHERE role = 'ADMINISTRATOR'
           AND status = 'ACTIVE'
           AND id <> ?
        """;

    private static final String ACTIVE_ADMINISTRATORS = """
        SELECT count(*)
          FROM caretaker
         WHERE role = 'ADMINISTRATOR'
           AND status = 'ACTIVE'
        """;

    private static final String SET_STATUS = """
        UPDATE caretaker
           SET status = ?
         WHERE id = ?
        """;

    @Autowired
    private Dispatcher dispatcher;

    @Autowired
    private CaretakerRepository caretakerRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private Clock clock;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    /** Administradores inativados para zerar os ativos; voltam a ativa depois do teste. */
    private List<UUID> setAside = List.of();

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void tearDown() {
        setAside.forEach(id -> jdbc.update(SET_STATUS, "ACTIVE", id));
    }

    /** O banco chega ao estado que as regras nao deixam alcancar: nenhum administrador ativo. */
    private void leaveNoActiveAdministrator(Caretaker initial) {
        setAside = jdbc.queryForList(OTHER_ACTIVE_ADMINISTRATORS, UUID.class, initial.id().value());
        setAside.forEach(id -> jdbc.update(SET_STATUS, "INACTIVE", id));
        jdbc.update(SET_STATUS, "INACTIVE", initial.id().value());
    }

    private long activeAdministrators() {
        Long result = jdbc.queryForObject(ACTIVE_ADMINISTRATORS, Long.class);
        return result == null ? 0 : result;
    }

    @Test
    @DisplayName("starts with no active administrator by giving the administration back to the initial one")
    void givenNoActiveAdministratorAndTheInitialOneInactive_whenStarting_thenRestoreThemAsTheOnlyActiveAdministrator() {
        // given
        Caretaker initial = aUniqueCaretaker()
                .withRole(Role.ADMINISTRATOR)
                .withHasher(passwordHasher)
                .withRoster(caretakerRepository)
                .withClock(clock)
                .build();
        caretakerRepository.save(initial);
        leaveNoActiveAdministrator(initial);
        BootstrapAdministratorInitializer initializer = new BootstrapAdministratorInitializer(
                dispatcher,
                new BootstrapAdministratorProperties(
                        "Administrador do Sistema",
                        initial.cpf().value(),
                        "recuperacao." + UUID.randomUUID().toString().substring(0, 8) + "@ovyx.com.br",
                        "11977770000",
                        PROVISIONAL_PASSWORD));

        // when
        initializer.run(null);

        // then
        Caretaker restored = caretakerRepository.findById(initial.id()).orElseThrow();
        assertThat(restored.isActive()).isTrue();
        assertThat(restored.role()).isEqualTo(Role.ADMINISTRATOR);
        assertThat(restored.mustChangePassword()).isTrue();
        assertThat(restored.authenticate(PROVISIONAL_PASSWORD, passwordHasher)).isTrue();
        assertThat(activeAdministrators()).as("nobody else registered").isEqualTo(1);
    }
}
