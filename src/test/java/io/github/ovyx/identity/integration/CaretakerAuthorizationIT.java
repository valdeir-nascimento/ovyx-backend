package io.github.ovyx.identity.integration;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.DEFAULT_PASSWORD;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.randomValidCpf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ovyx.CsrfHandshake;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.util.UUID;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Autorizacao das cinco operacoes de responsaveis (FR-008, FR-009, FR-010, SC-003).
 *
 * <p>Toda operacao responde 401 sem sessao e 403 {@code FORBIDDEN} ao usuario comum. Nesses casos
 * as escritas levam o token de protecao, para que a recusa seja de permissao, e nao de CSRF; sem o
 * token, nem o administrador escreve.
 */
@AutoConfigureMockMvc
@DisplayName("Caretaker authorization")
class CaretakerAuthorizationIT extends IntegrationTestSupport {

    private static final String ANY_ID = "9f8e7d6c-5b4a-4938-2716-0f1e2d3c4b5a";
    private static final String BODY = """
        {"fullName": "João Pereira de Souza", "cpf": "52998224725", "email": "joao@ovyx.com.br",
         "mobilePhone": "91991234567", "password": "AviarioSul2026", "role": "USER"}
        """;

    private static final String CARETAKERS_WITH_EMAIL = """
        SELECT count(*)
          FROM caretaker
         WHERE email = ?
        """;

    @Autowired
    private MockMvc mockMvc;

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
    void clearTheSignInAttempts() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("DELETE FROM sign_in_attempt");
    }

    private static Stream<Arguments> administrativeOperations() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/v1/caretakers"),
                Arguments.of(HttpMethod.GET, "/api/v1/caretakers"),
                Arguments.of(HttpMethod.GET, "/api/v1/caretakers/" + ANY_ID),
                Arguments.of(HttpMethod.PUT, "/api/v1/caretakers/" + ANY_ID),
                Arguments.of(HttpMethod.POST, "/api/v1/caretakers/" + ANY_ID + "/deactivation"));
    }

    private MockHttpServletRequestBuilder operation(HttpMethod method, String path) {
        return request(method, path)
                .with(CsrfHandshake.using(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY);
    }

    private static Stream<Arguments> writeOperations() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/v1/caretakers"),
                Arguments.of(HttpMethod.PUT, "/api/v1/caretakers/{target}"),
                Arguments.of(HttpMethod.POST, "/api/v1/caretakers/{target}/deactivation"));
    }

    private Caretaker saved(CaretakerTestDataBuilder builder) {
        Caretaker caretaker = builder.withHasher(passwordHasher)
                .withRoster(caretakerRepository)
                .withClock(clock)
                .build();
        caretakerRepository.save(caretaker);
        return caretaker;
    }

    private Caretaker saved(Role role) {
        return saved(aUniqueCaretaker().withRole(role));
    }

    private long caretakersWithEmail(String email) {
        Long result = jdbc.queryForObject(CARETAKERS_WITH_EMAIL, Long.class, email);
        return result == null ? 0 : result;
    }

    private Cookie[] signedIn(Role role) throws Exception {
        return signedIn(saved(role));
    }

    private Cookie[] signedIn(Caretaker user) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/sign-in")
                        .with(CsrfHandshake.using(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier": "%s", "password": "%s"}
                                """.formatted(user.email().value(), DEFAULT_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("administrativeOperations")
    @DisplayName("refuses every caretaker operation without a session with 401")
    void givenNoSession_whenCallingACaretakerOperation_thenAnswer401(HttpMethod method, String path)
            throws Exception {
        // given — sem cookie de sessao

        // when
        ResultActions response = mockMvc.perform(operation(method, path));

        // then
        response.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("administrativeOperations")
    @DisplayName("refuses every caretaker operation to a common user with 403 FORBIDDEN, revealing nothing")
    void givenCommonUserSession_whenCallingACaretakerOperation_thenAnswer403(HttpMethod method, String path)
            throws Exception {
        // given
        Cookie[] session = signedIn(Role.USER);

        // when
        ResultActions response = mockMvc.perform(operation(method, path).cookie(session));

        // then
        response.andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("administrativeOperations")
    @DisplayName("refuses every caretaker operation to an administrator who still owes the password change")
    void givenAdministratorOwingThePasswordChange_whenCallingACaretakerOperation_thenAnswer403PasswordChangeRequired(
            HttpMethod method, String path) throws Exception {
        // given
        // O perfil libera a rota; e a senha provisoria que segura o administrador semeado ate a
        // troca (FR-025). Antes da US2 nenhuma rota chegava aqui: todas caiam na negacao por omissao.
        Cookie[] session = signedIn(saved(aUniqueCaretaker().withRole(Role.ADMINISTRATOR).withPendingPasswordChange()));

        // when
        ResultActions response = mockMvc.perform(operation(method, path).cookie(session));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("writeOperations")
    @DisplayName("refuses every caretaker write without the CSRF token, even to an administrator, and changes nothing")
    void givenAdministratorSessionWithoutCsrfToken_whenWritingACaretaker_thenAnswer403CsrfTokenInvalidAndChangeNothing(
            HttpMethod method, String path) throws Exception {
        // given
        // A sessao de administrador passaria pela autorizacao: a recusa aqui so pode ser do CSRF.
        // O corpo promoveria o alvo e gravaria um e-mail novo, e a inativacao o tiraria da ativa.
        Cookie[] session = signedIn(Role.ADMINISTRATOR);
        Caretaker target = saved(Role.USER);
        String email = "sem.token." + UUID.randomUUID().toString().substring(0, 8) + "@ovyx.com.br";

        // when
        ResultActions response = mockMvc.perform(request(method, path.replace("{target}", target.id().value().toString()))
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName": "Escrita Sem Token", "cpf": "%s", "email": "%s", "mobilePhone": "91991230000",
                         "password": "AviarioSul2026", "role": "ADMINISTRATOR"}
                        """.formatted(randomValidCpf(), email)));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
        assertThat(caretakersWithEmail(email)).as("nobody registered or updated").isZero();
        Caretaker unchanged = caretakerRepository.findById(target.id()).orElseThrow();
        assertThat(unchanged.role()).as("not promoted").isEqualTo(Role.USER);
        assertThat(unchanged.isActive()).as("not deactivated").isTrue();
    }
}
