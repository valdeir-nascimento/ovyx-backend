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
import jakarta.servlet.http.Cookie;
import java.time.Clock;
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
