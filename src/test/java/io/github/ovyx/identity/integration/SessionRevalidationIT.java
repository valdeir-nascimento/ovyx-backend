package io.github.ovyx.identity.integration;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.DEFAULT_PASSWORD;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.randomValidCpf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ovyx.CsrfHandshake;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.application.caretaker.DeactivateCaretakerCommand;
import io.github.ovyx.identity.application.caretaker.UpdateCaretakerCommand;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.Dispatcher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * A sessao aberta nao vale mais do que a situacao atual do responsavel (FR-005, FR-008, US3).
 *
 * <p>A autoridade fica gravada na sessao no login. Sem reconsulta, um administrador inativado
 * continuava administrando pela sessao que ja tinha — inclusive cadastrando outro administrador
 * para continuar dentro —, e o rebaixado continuava administrador ate sair.
 */
@AutoConfigureMockMvc
@DisplayName("Session revalidation")
class SessionRevalidationIT extends IntegrationTestSupport {

    private static final String SIGN_OUTS_OF = """
        SELECT count(*)
          FROM access_event
         WHERE caretaker_id = ?
           AND outcome = 'SIGNED_OUT'
        """;

    private static final String CARETAKERS_WITH_EMAIL = """
        SELECT count(*)
          FROM caretaker
         WHERE email = ?
        """;

    @Autowired
    private MockMvc mockMvc;

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

    @Autowired
    private MeterRegistry meterRegistry;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("DELETE FROM sign_in_attempt");
    }

    private Caretaker saved(Role role) {
        Caretaker caretaker = aUniqueCaretaker()
                .withRole(role)
                .withHasher(passwordHasher)
                .withRoster(caretakerRepository)
                .withClock(clock)
                .build();
        caretakerRepository.save(caretaker);
        return caretaker;
    }

    private Cookie[] signedIn(Caretaker caretaker) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/sign-in")
                        .with(CsrfHandshake.using(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier": "%s", "password": "%s"}
                                """.formatted(caretaker.email().value(), DEFAULT_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }

    private void changeRole(Caretaker caretaker, Role role) {
        dispatcher.dispatch(new UpdateCaretakerCommand(
                caretaker.id(),
                caretaker.fullName().value(),
                caretaker.cpf().value(),
                caretaker.email().value(),
                caretaker.mobilePhone().value(),
                role));
    }

    private double unavailableRefusalsCounted() {
        return Optional.ofNullable(meterRegistry
                        .find("ovyx.result.failure")
                        .tag("code", "CARETAKER_UNAVAILABLE")
                        .counter())
                .map(Counter::count)
                .orElse(0.0);
    }

    private long signOutsOf(Caretaker caretaker) {
        Long result = jdbc.queryForObject(SIGN_OUTS_OF, Long.class, caretaker.id().value());
        return result == null ? 0 : result;
    }

    private long caretakersWithEmail(String email) {
        Long result = jdbc.queryForObject(CARETAKERS_WITH_EMAIL, Long.class, email);
        return result == null ? 0 : result;
    }

    private static Stream<Arguments> administrativeOperations() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/v1/caretakers"),
                Arguments.of(HttpMethod.GET, "/api/v1/caretakers"),
                Arguments.of(HttpMethod.GET, "/api/v1/caretakers/{target}"),
                Arguments.of(HttpMethod.PUT, "/api/v1/caretakers/{target}"),
                Arguments.of(HttpMethod.POST, "/api/v1/caretakers/{target}/deactivation"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("administrativeOperations")
    @DisplayName("an administrator deactivated with the session open is refused and loses the session")
    void givenAdministratorDeactivatedWithTheSessionOpen_whenCallingACaretakerOperation_thenAnswer401AndEndTheSession(
            HttpMethod method, String path) throws Exception {
        // given
        Caretaker administrator = saved(Role.ADMINISTRATOR);
        Caretaker target = saved(Role.USER);
        Cookie[] session = signedIn(administrator);
        dispatcher.dispatch(new DeactivateCaretakerCommand(administrator.id()));
        String email = "conta.paralela." + UUID.randomUUID().toString().substring(0, 8) + "@ovyx.com.br";

        // when
        ResultActions response = mockMvc.perform(request(method, path.replace("{target}", target.id().value().toString()))
                .with(CsrfHandshake.using(mockMvc))
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName": "Conta Paralela", "cpf": "%s", "email": "%s", "mobilePhone": "91991230000",
                         "password": "AviarioSul2026", "role": "ADMINISTRATOR"}
                        """.formatted(randomValidCpf(), email)));

        // then
        response.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("CARETAKER_UNAVAILABLE"));
        assertThat(caretakersWithEmail(email)).as("nothing registered").isZero();
        assertThat(caretakerRepository.findById(target.id()).orElseThrow().isActive())
                .as("nothing deactivated")
                .isTrue();
        mockMvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refuses a deactivated caretaker like any failure of a use case, and counts the refusal")
    void givenAdministratorDeactivatedWithTheSessionOpen_whenListingCaretakers_thenAnswerThroughTheResultMapper()
            throws Exception {
        // given
        // A recusa vem do caso de uso e sai pelo ResultHttpMapper, como no /auth/me: status, titulo e
        // metrica sao os de qualquer falha, e nao escritos a mao no filtro (principio IV).
        Caretaker administrator = saved(Role.ADMINISTRATOR);
        Cookie[] session = signedIn(administrator);
        dispatcher.dispatch(new DeactivateCaretakerCommand(administrator.id()));
        double countedBefore = unavailableRefusalsCounted();

        // when
        ResultActions response = mockMvc.perform(get("/api/v1/caretakers").cookie(session));

        // then
        response.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andExpect(jsonPath("$.instance").value("/api/v1/caretakers"));
        assertThat(unavailableRefusalsCounted()).isEqualTo(countedBefore + 1);
    }

    @Test
    @DisplayName("an administrator restored with the session open owes the password change right away (FR-025)")
    void givenAdministratorRestoredWithTheSessionOpen_whenListingCaretakers_thenAnswer403PasswordChangeRequired()
            throws Exception {
        // given
        // A semeadura restaura com senha provisoria. A sessao que ja estava aberta passa a dever a
        // troca na requisicao seguinte, como uma entrada nova com a senha provisoria deveria.
        Caretaker administrator = saved(Role.ADMINISTRATOR);
        Cookie[] session = signedIn(administrator);
        Caretaker stored = caretakerRepository.findById(administrator.id()).orElseThrow();
        stored.restoreAsInitialAdministrator("RecuperarAcesso2026", passwordHasher, caretakerRepository, clock);
        caretakerRepository.save(stored);

        // when
        ResultActions response = mockMvc.perform(get("/api/v1/caretakers").cookie(session));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
    }

    @Test
    @DisplayName("a caretaker deactivated with the session open still signs out, and the sign-out is audited (FR-006)")
    void givenCaretakerDeactivatedWithTheSessionOpen_whenSigningOut_thenAnswer204AndAuditTheSignOut() throws Exception {
        // given
        // A saida nao passa pela reconferencia: encerrar a sessao do inativado so antecipa o que ela
        // mesma faz, e passar por ela registra o evento na auditoria.
        Caretaker caretaker = saved(Role.USER);
        Cookie[] session = signedIn(caretaker);
        dispatcher.dispatch(new DeactivateCaretakerCommand(caretaker.id()));

        // when
        ResultActions response = mockMvc.perform(
                post("/api/v1/auth/sign-out").with(CsrfHandshake.using(mockMvc)).cookie(session));

        // then
        response.andExpect(status().isNoContent());
        assertThat(signOutsOf(caretaker)).isEqualTo(1);
    }

    @Test
    @DisplayName("an administrator demoted with the session open loses the administration right away")
    void givenAdministratorDemotedWithTheSessionOpen_whenListingCaretakers_thenAnswer403() throws Exception {
        // given
        Caretaker administrator = saved(Role.ADMINISTRATOR);
        Cookie[] session = signedIn(administrator);
        changeRole(administrator, Role.USER);

        // when
        ResultActions response = mockMvc.perform(get("/api/v1/caretakers").cookie(session));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("a common user promoted with the session open reaches the administration without signing in again")
    void givenUserPromotedWithTheSessionOpen_whenListingCaretakers_thenAnswer200() throws Exception {
        // given
        Caretaker user = saved(Role.USER);
        Cookie[] session = signedIn(user);
        changeRole(user, Role.ADMINISTRATOR);

        // when
        ResultActions response = mockMvc.perform(get("/api/v1/caretakers").cookie(session));

        // then
        response.andExpect(status().isOk());
    }
}
