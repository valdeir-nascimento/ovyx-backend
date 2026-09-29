package io.github.ovyx.farm.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.ovyx.IntegrationSessions;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Quem pode o quê nas rotas do contexto farm (FR-018, FR-019; US4; SC-004; S-10, S-11; e FR-006, FR-021 e
 * R-010 da 004).
 *
 * <p>Consultar é de qualquer responsável autenticado; cadastrar, editar, inativar e reativar é só do
 * administrador, para setores, gaiolas e fórmulas de ração. O usuário comum recebe o 403 genérico da
 * feature 001, o mesmo para o que existe e para o que não existe; e o visitante sem sessão, o 401.
 */
@AutoConfigureMockMvc
@DisplayName("Farm authorization")
class FarmAuthorizationIT extends IntegrationTestSupport {

    private static final String SECTORS = "/api/v1/sectors";
    private static final String FORMULAS = "/api/v1/feed-formulas";

    /** Os campos da recusa genérica, e só eles, como na feature 001. */
    private static final String[] PROBLEM_FIELDS = {"title", "status", "detail", "instance", "code"};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaretakerRepository caretakerRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private Clock clock;

    @Autowired
    private ObjectMapper objectMapper;

    private IntegrationSessions sessions;
    private Cookie[] commonUser;
    private String sectorPath;
    private String cagePath;
    private String formulaPath;

    @BeforeEach
    void registerASectorAndSignInAsACommonUser() throws Exception {
        sessions = new IntegrationSessions(mockMvc, caretakerRepository, passwordHasher, clock);
        Cookie[] administrator = sessions.administrator().cookies();
        String response = mockMvc.perform(post(SECTORS)
                        .with(sessions.csrf())
                        .cookie(administrator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Autorização %s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        sectorPath = SECTORS + "/" + JsonPath.read(response, "$.id");
        String cage = mockMvc.perform(post(sectorPath + "/cages")
                        .with(sessions.csrf())
                        .cookie(administrator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"battery": "B", "number": 7, "birdCount": 50}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        cagePath = sectorPath + "/cages/" + JsonPath.read(cage, "$.id");
        String formula = mockMvc.perform(post(FORMULAS)
                        .with(sessions.csrf())
                        .cookie(administrator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Autorização %s", "pricePerKg": "2,85", "expectedIntake": 28}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        formulaPath = FORMULAS + "/" + JsonPath.read(formula, "$.id");
        commonUser = sessions.commonUser().cookies();
    }

    /** A rota com os identificadores cadastrados para o teste no lugar dos marcadores. */
    private MockHttpServletRequestBuilder request(HttpMethod method, String route) {
        return MockMvcRequestBuilders.request(method, pathOf(route));
    }

    /** O caminho da rota, com o setor, a gaiola e a fórmula cadastrados para o teste. */
    private String pathOf(String route) {
        return route.replace("{cage}", cagePath).replace("{sector}", sectorPath).replace("{formula}", formulaPath);
    }

    /**
     * Um corpo que serve às escritas de setor, de gaiola, de fórmula e de pesagem: cada uma lê só os campos
     * dela.
     */
    private static MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder request) {
        return request.contentType(MediaType.APPLICATION_JSON).content("""
                {"name": "Codornas — Galpão 9", "battery": "C", "number": 9, "birdCount": 50,
                 "pricePerKg": "2,85", "expectedIntake": 28, "weighedOn": "2026-09-24", "averageWeight": 161}
                """);
    }

    @Test
    @DisplayName("lets a common user list the sectors")
    void givenCommonUser_whenListingTheSectors_thenAnswerOk() throws Exception {
        // given
        Cookie[] session = commonUser;

        // when
        ResultActions response = mockMvc.perform(get(SECTORS).cookie(session));

        // then
        response.andExpect(status().isOk());
    }

    @Test
    @DisplayName("lets a common user find a sector")
    void givenCommonUser_whenFindingASector_thenAnswerOk() throws Exception {
        // given
        Cookie[] session = commonUser;

        // when
        ResultActions response = mockMvc.perform(get(sectorPath).cookie(session));

        // then
        response.andExpect(status().isOk());
    }

    @Test
    @DisplayName("forbids a common user to register a sector")
    void givenCommonUser_whenRegisteringASector_thenForbid() throws Exception {
        // given
        MockHttpServletRequestBuilder registration = withBody(post(SECTORS));

        // when
        ResultActions response = mockMvc.perform(registration.with(sessions.csrf()).cookie(commonUser));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("forbids a common user to update a sector")
    void givenCommonUser_whenUpdatingASector_thenForbid() throws Exception {
        // given
        MockHttpServletRequestBuilder update = withBody(put(sectorPath));

        // when
        ResultActions response = mockMvc.perform(update.with(sessions.csrf()).cookie(commonUser));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
        "GET, /api/v1/sectors",
        "GET, {sector}",
        "POST, /api/v1/sectors",
        "PUT, {sector}",
        "GET, {sector}/cages",
        "GET, {cage}",
        "POST, {sector}/cages",
        "PUT, {cage}",
        "POST, {sector}/deactivation",
        "POST, {sector}/reactivation",
        "POST, {cage}/deactivation",
        "POST, {cage}/reactivation",
        "GET, /api/v1/feed-formulas",
        "GET, {formula}",
        "POST, /api/v1/feed-formulas",
        "PUT, {formula}",
        "POST, {formula}/deactivation",
        "POST, {formula}/reactivation",
        "GET, {cage}/weighings",
        "POST, {cage}/weighings",
        "GET, {cage}/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77",
        "PUT, {cage}/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77",
        "POST, {cage}/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77/voiding"
    })
    @DisplayName("requires a session on every sector, cage, formula and weighing route")
    void givenAnonymousVisitor_whenCallingAFarmRoute_thenRequireAuthentication(String method, String route)
            throws Exception {
        // given
        MockHttpServletRequestBuilder request = withBody(request(HttpMethod.valueOf(method), route));

        // when
        ResultActions response = mockMvc.perform(request.with(sessions.csrf()));

        // then
        response.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"{sector}/cages", "{cage}"})
    @DisplayName("lets a common user search and find the cages of a sector")
    void givenCommonUser_whenReadingTheCages_thenAnswerOk(String route) throws Exception {
        // given
        MockHttpServletRequestBuilder request = request(HttpMethod.GET, route);

        // when
        ResultActions response = mockMvc.perform(request.cookie(commonUser));

        // then
        response.andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"/api/v1/feed-formulas", "{formula}"})
    @DisplayName("lets a common user list and find the formulas")
    void givenCommonUser_whenReadingTheFormulas_thenAnswerOk(String route) throws Exception {
        // given
        MockHttpServletRequestBuilder request = request(HttpMethod.GET, route);

        // when
        ResultActions response = mockMvc.perform(request.cookie(commonUser));

        // then
        response.andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
        "POST, /api/v1/feed-formulas",
        "PUT, {formula}",
        "POST, {formula}/deactivation",
        "POST, {formula}/reactivation"
    })
    @DisplayName("forbids a common user to register, update, deactivate or reactivate a formula")
    void givenCommonUser_whenWritingAFormula_thenForbid(String method, String route) throws Exception {
        // given
        MockHttpServletRequestBuilder request = withBody(request(HttpMethod.valueOf(method), route));

        // when
        ResultActions response = mockMvc.perform(request.with(sessions.csrf()).cookie(commonUser));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get(formulaPath).cookie(commonUser))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.pricePerKg").value(2.85));
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({"POST, {sector}/cages", "PUT, {cage}"})
    @DisplayName("forbids a common user to register or update a cage")
    void givenCommonUser_whenWritingACage_thenForbid(String method, String route) throws Exception {
        // given
        MockHttpServletRequestBuilder request = withBody(request(HttpMethod.valueOf(method), route));

        // when
        ResultActions response = mockMvc.perform(request.with(sessions.csrf()).cookie(commonUser));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"{sector}/deactivation", "{sector}/reactivation", "{cage}/deactivation", "{cage}/reactivation"})
    @DisplayName("forbids a common user to deactivate or reactivate a sector or a cage")
    void givenCommonUser_whenDeactivatingOrReactivating_thenForbid(String route) throws Exception {
        // given
        MockHttpServletRequestBuilder request = request(HttpMethod.POST, route);

        // when
        ResultActions response = mockMvc.perform(request.with(sessions.csrf()).cookie(commonUser));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // ------------------------------------------------------------------ matriz completa (US4)

    /** A rota com um setor, uma gaiola e uma fórmula que não existem no lugar dos marcadores. */
    private static String missing(String route) {
        return route.replace("{cage}", SECTORS + "/" + UUID.randomUUID() + "/cages/" + UUID.randomUUID())
                .replace("{sector}", SECTORS + "/" + UUID.randomUUID())
                .replace("{formula}", FORMULAS + "/" + UUID.randomUUID());
    }

    private MvcResult write(Cookie[] session, HttpMethod method, String path) throws Exception {
        return mockMvc.perform(withBody(MockMvcRequestBuilders.request(method, path))
                        .with(sessions.csrf())
                        .cookie(session))
                .andReturn();
    }

    private Map<String, Object> fieldsOf(MvcResult result) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8), new TypeReference<>() {});
    }

    /** O corpo sem o {@code instance}, que por definição repete o caminho pedido. */
    private Map<String, Object> withoutInstance(MvcResult result) throws Exception {
        Map<String, Object> fields = new HashMap<>(fieldsOf(result));
        fields.remove("instance");
        return fields;
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
        "PUT, {sector}",
        "POST, {sector}/deactivation",
        "POST, {sector}/reactivation",
        "POST, {sector}/cages",
        "PUT, {cage}",
        "POST, {cage}/deactivation",
        "POST, {cage}/reactivation",
        "PUT, {formula}",
        "POST, {formula}/deactivation",
        "POST, {formula}/reactivation"
    })
    @DisplayName("refuses a common user about an item that exists exactly as about one that does not")
    void givenCommonUser_whenWritingAboutAnExistingAndAMissingItem_thenAnswerTheSameRefusal(String method, String route)
            throws Exception {
        // given
        HttpMethod verb = HttpMethod.valueOf(method);

        // when
        MvcResult aboutTheExisting = write(commonUser, verb, pathOf(route));
        MvcResult aboutTheMissing = write(commonUser, verb, missing(route));

        // then
        assertThat(aboutTheExisting.getResponse().getStatus()).isEqualTo(403);
        assertThat(aboutTheMissing.getResponse().getStatus()).isEqualTo(403);
        assertThat(withoutInstance(aboutTheExisting)).isEqualTo(withoutInstance(aboutTheMissing));
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
        "POST, /api/v1/sectors",
        "PUT, {sector}",
        "POST, {sector}/deactivation",
        "POST, {sector}/reactivation",
        "POST, {sector}/cages",
        "PUT, {cage}",
        "POST, {cage}/deactivation",
        "POST, {cage}/reactivation",
        "POST, /api/v1/feed-formulas",
        "PUT, {formula}",
        "POST, {formula}/deactivation",
        "POST, {formula}/reactivation"
    })
    @DisplayName("answers a common user only the generic refusal, with the path that was asked for")
    void givenCommonUser_whenWriting_thenAnswerOnlyTheGenericRefusal(String method, String route) throws Exception {
        // given
        HttpMethod verb = HttpMethod.valueOf(method);
        String path = pathOf(route);

        // when
        MvcResult result = write(commonUser, verb, path);

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(fieldsOf(result))
                .containsOnlyKeys(PROBLEM_FIELDS)
                .containsEntry("code", "FORBIDDEN")
                .containsEntry("title", "Acesso negado")
                .containsEntry("detail", "Você não tem permissão para executar esta operação.")
                .containsEntry("instance", path);
    }

    @Test
    @DisplayName("holds a common user who owes the provisional password before the sectors")
    void givenCommonUserOwingThePasswordChange_whenListingTheSectors_thenRequireThePasswordChange() throws Exception {
        // given
        Cookie[] owing = sessions.commonUserOwingThePasswordChange().cookies();

        // when
        ResultActions response = mockMvc.perform(get(SECTORS).cookie(owing));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
    }

    // ------------------------------------------------------------------ pesagens (005)

    @Test
    @DisplayName("lets a common user read the weighings of a cage")
    void givenCommonUser_whenReadingTheWeighings_thenAnswerOk() throws Exception {
        // given
        MockHttpServletRequestBuilder request = get(cagePath + "/weighings");

        // when
        ResultActions response = mockMvc.perform(request.cookie(commonUser));

        // then
        response.andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("lets a common user and an administrator record a weighing, the first write of the farm open to both")
    void givenAnyCaretaker_whenRecordingAWeighing_thenAnswerCreated(boolean administrator) throws Exception {
        // given
        Cookie[] session = administrator ? sessions.administrator().cookies() : commonUser;
        String day = administrator ? "2026-09-17" : "2026-09-10";

        // when
        ResultActions response = mockMvc.perform(post(cagePath + "/weighings")
                .with(sessions.csrf())
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"weighedOn": "%s", "averageWeight": 158}
                        """.formatted(day)));

        // then
        response.andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("lets a common user and an administrator correct and void a weighing")
    void givenAnyCaretaker_whenCorrectingAndVoidingAWeighing_thenAnswerOkAndNoContent(boolean administrator)
            throws Exception {
        // given
        Cookie[] session = administrator ? sessions.administrator().cookies() : commonUser;
        String day = administrator ? "2026-09-03" : "2026-08-27";
        String created = mockMvc.perform(post(cagePath + "/weighings")
                        .with(sessions.csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weighedOn": "%s", "averageWeight": 150}
                                """.formatted(day)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String weighing = cagePath + "/weighings/" + JsonPath.read(created, "$.id");

        // when
        ResultActions found = mockMvc.perform(get(weighing).cookie(session));
        ResultActions corrected = mockMvc.perform(put(weighing)
                .with(sessions.csrf())
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"weighedOn": "%s", "averageWeight": 151}
                        """.formatted(day)));
        ResultActions voided = mockMvc.perform(post(weighing + "/voiding").with(sessions.csrf()).cookie(session));

        // then
        found.andExpect(status().isOk());
        corrected.andExpect(status().isOk());
        voided.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("refuses a weighing without the protection token")
    void givenCommonUserWithoutTheCsrfToken_whenRecordingAWeighing_thenRefuseTheMissingToken() throws Exception {
        // given
        MockHttpServletRequestBuilder request = withBody(post(cagePath + "/weighings"));

        // when
        ResultActions response = mockMvc.perform(request.cookie(commonUser));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("refuses a write of an administrator without the protection token")
    void givenAdministratorWithoutTheCsrfToken_whenRegisteringASector_thenRefuseTheMissingToken() throws Exception {
        // given
        Cookie[] administrator = sessions.administrator().cookies();

        // when
        ResultActions response = mockMvc.perform(withBody(post(SECTORS)).cookie(administrator));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("refuses a formula write of an administrator without the protection token")
    void givenAdministratorWithoutTheCsrfToken_whenRegisteringAFormula_thenRefuseTheMissingToken() throws Exception {
        // given
        Cookie[] administrator = sessions.administrator().cookies();

        // when
        ResultActions response = mockMvc.perform(withBody(post(FORMULAS)).cookie(administrator));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("forbids even an administrator to delete a formula: nothing is deleted, and the route does not exist")
    void givenAdministrator_whenDeletingAFormula_thenForbid() throws Exception {
        // given
        Cookie[] administrator = sessions.administrator().cookies();

        // when
        ResultActions response = mockMvc.perform(delete(formulaPath).with(sessions.csrf()).cookie(administrator));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("forbids even an administrator to delete a sector: nothing is deleted, and the route does not exist")
    void givenAdministrator_whenDeletingASector_thenForbid() throws Exception {
        // given
        Cookie[] administrator = sessions.administrator().cookies();

        // when
        ResultActions response = mockMvc.perform(delete(sectorPath).with(sessions.csrf()).cookie(administrator));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
