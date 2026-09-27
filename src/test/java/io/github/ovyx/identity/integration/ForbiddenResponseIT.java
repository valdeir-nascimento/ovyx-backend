package io.github.ovyx.identity.integration;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.DEFAULT_PASSWORD;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ovyx.CsrfHandshake;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * O que o 403 diz a quem não tem permissão (FR-010, US3; T094).
 *
 * <p>A recusa não pode servir de sonda: o corpo é o mesmo para um responsável que existe e para um
 * que não existe, não traz dado de ninguém e só tem os campos genéricos do Problem Details. O
 * {@code instance} repete o caminho que a própria pessoa pediu, e nada além dele.
 */
@AutoConfigureMockMvc
@DisplayName("Forbidden response")
class ForbiddenResponseIT extends IntegrationTestSupport {

    private static final String GENERIC_TITLE = "Acesso negado";
    private static final String GENERIC_DETAIL = "Você não tem permissão para executar esta operação.";

    /** Os campos que o 403 publica; o {@code type} padrão, {@code about:blank}, é omitido. */
    private static final String[] PROBLEM_FIELDS = {"title", "status", "detail", "instance", "code"};

    private static final String BODY = """
        {"fullName": "João Pereira de Souza", "cpf": "52998224725", "email": "joao@ovyx.com.br",
         "mobilePhone": "91991234567", "password": "AviarioSul2026", "role": "USER"}
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

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearTheSignInAttempts() {
        new JdbcTemplate(dataSource).update("DELETE FROM sign_in_attempt");
    }

    private static Stream<Arguments> operationsAboutOneCaretaker() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/v1/caretakers/{target}"),
                Arguments.of(HttpMethod.PUT, "/api/v1/caretakers/{target}"),
                Arguments.of(HttpMethod.POST, "/api/v1/caretakers/{target}/deactivation"));
    }

    private static Stream<Arguments> caretakerOperations() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/v1/caretakers"),
                Arguments.of(HttpMethod.GET, "/api/v1/caretakers"),
                Arguments.of(HttpMethod.GET, "/api/v1/caretakers/{target}"),
                Arguments.of(HttpMethod.PUT, "/api/v1/caretakers/{target}"),
                Arguments.of(HttpMethod.POST, "/api/v1/caretakers/{target}/deactivation"));
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

    private Cookie[] signedInAsCommonUser() throws Exception {
        Caretaker user = saved(Role.USER);
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

    private MvcResult call(Cookie[] session, HttpMethod method, String path) throws Exception {
        return mockMvc.perform(request(method, path)
                        .with(CsrfHandshake.using(mockMvc))
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andReturn();
    }

    private static String bodyOf(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private Map<String, Object> fieldsOf(MvcResult result) throws Exception {
        return objectMapper.readValue(bodyOf(result), new TypeReference<Map<String, Object>>() {});
    }

    /** O corpo sem o {@code instance}, que por definição repete o caminho pedido. */
    private Map<String, Object> withoutInstance(MvcResult result) throws Exception {
        Map<String, Object> fields = new HashMap<>(fieldsOf(result));
        fields.remove("instance");
        return fields;
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("operationsAboutOneCaretaker")
    @DisplayName("answers about a caretaker who exists exactly as about one who does not")
    void givenCommonUserSession_whenCallingAboutAnExistingAndAMissingCaretaker_thenAnswerTheSameRefusal(
            HttpMethod method, String template) throws Exception {
        // given
        Cookie[] session = signedInAsCommonUser();
        Caretaker existing = saved(Role.ADMINISTRATOR);
        UUID missing = UUID.randomUUID();

        // when
        MvcResult aboutTheExisting = call(session, method, template.replace("{target}", existing.id().value().toString()));
        MvcResult aboutTheMissing = call(session, method, template.replace("{target}", missing.toString()));

        // then
        assertThat(aboutTheExisting.getResponse().getStatus()).isEqualTo(403);
        assertThat(withoutInstance(aboutTheExisting)).isEqualTo(withoutInstance(aboutTheMissing));
    }

    @Test
    @DisplayName("answers a published route exactly as a route nobody declared, so the refusals do not map the API")
    void givenCommonUserSession_whenCallingAPublishedAndAnUndeclaredRoute_thenAnswerTheSameRefusal() throws Exception {
        // given
        // FR-010 também protege a estrutura: se a recusa da rota publicada diferisse da recusa da rota
        // inexistente, o usuário comum mapearia a área administrativa pela diferença.
        Cookie[] session = signedInAsCommonUser();

        // when
        MvcResult published = call(session, HttpMethod.GET, "/api/v1/caretakers");
        MvcResult undeclared = call(session, HttpMethod.GET, "/api/v1/rota-que-ninguem-declarou");

        // then
        assertThat(published.getResponse().getStatus()).isEqualTo(403);
        assertThat(withoutInstance(published)).isEqualTo(withoutInstance(undeclared));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("caretakerOperations")
    @DisplayName("answers only the generic Problem Details fields, with the path that was asked for")
    void givenCommonUserSession_whenCallingACaretakerOperation_thenAnswerOnlyTheGenericRefusal(
            HttpMethod method, String template) throws Exception {
        // given
        Cookie[] session = signedInAsCommonUser();
        Caretaker target = saved(Role.ADMINISTRATOR);
        String path = template.replace("{target}", target.id().value().toString());

        // when
        MvcResult result = call(session, method, path);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        Map<String, Object> fields = fieldsOf(result);
        assertThat(fields).containsOnlyKeys(PROBLEM_FIELDS);
        assertThat(fields)
                .containsEntry("code", "FORBIDDEN")
                .containsEntry("title", GENERIC_TITLE)
                .containsEntry("detail", GENERIC_DETAIL)
                .containsEntry("instance", path);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("caretakerOperations")
    @DisplayName("carries no data of the caretaker the operation was about")
    void givenCommonUserSession_whenCallingAnOperationAboutAnotherCaretaker_thenRevealNothingAboutThem(
            HttpMethod method, String template) throws Exception {
        // given
        // Documenta o cenário por inteiro; com os campos já presos pelo teste anterior, só um
        // vazamento por campo novo chegaria aqui.
        Cookie[] session = signedInAsCommonUser();
        Caretaker target = saved(Role.ADMINISTRATOR);
        String path = template.replace("{target}", target.id().value().toString());

        // when
        MvcResult result = call(session, method, path);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(bodyOf(result))
                .doesNotContain(target.fullName().value())
                .doesNotContain(target.email().value())
                .doesNotContain(target.cpf().value())
                .doesNotContain(target.mobilePhone().value());
    }
}
