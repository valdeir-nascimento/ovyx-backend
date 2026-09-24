package io.github.ovyx.identity.integration;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.DEFAULT_PASSWORD;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.randomValidCpf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ovyx.CsrfHandshake;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrato das operacoes de responsaveis, contra a aplicacao em execucao e PostgreSQL real (T076).
 *
 * <p>Cobre os cenarios 1 a 7 da Historia 2 e os V-06 a V-10 do quickstart. Cada teste age com um
 * administrador proprio, recem-cadastrado, para nao depender da ordem.
 */
@AutoConfigureMockMvc
@DisplayName("Caretaker contract")
class CaretakerContractIT extends IntegrationTestSupport {

    private static final String CARETAKERS = "/api/v1/caretakers";
    private static final String PASSWORD = "AviarioSul2026";

    private static final String OTHER_ACTIVE_ADMINISTRATORS = """
        SELECT id
          FROM caretaker
         WHERE role = 'ADMINISTRATOR'
           AND status = 'ACTIVE'
           AND id <> ?
        """;

    private static final String DEACTIVATE_BY_ID = """
        UPDATE caretaker
           SET status = 'INACTIVE'
         WHERE id = ?
        """;

    private static final String REACTIVATE_BY_ID = """
        UPDATE caretaker
           SET status = 'ACTIVE'
         WHERE id = ?
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
    private Caretaker administrator;
    private Cookie[] session;

    /** Administradores inativados para isolar o ultimo; voltam a ativa depois de cada teste. */
    private List<UUID> setAside = List.of();

    @BeforeEach
    void signInAsAnAdministrator() throws Exception {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("DELETE FROM sign_in_attempt");
        administrator = saved(Role.ADMINISTRATOR);
        session = signIn(administrator.email().value(), DEFAULT_PASSWORD)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }

    @AfterEach
    void bringBackTheAdministratorsSetAside() {
        setAside.forEach(id -> jdbc.update(REACTIVATE_BY_ID, id));
    }

    // ---------------------------------------------------------------------------------- apoio

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

    private ResultActions signIn(String identifier, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/sign-in")
                .with(CsrfHandshake.using(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"identifier": "%s", "password": "%s"}
                        """.formatted(identifier, password)));
    }

    private static String registration(String fullName, String cpf, String email, String mobilePhone) {
        return """
                {
                  "fullName": "%s",
                  "cpf": "%s",
                  "email": "%s",
                  "mobilePhone": "%s",
                  "password": "%s",
                  "role": "USER"
                }
                """.formatted(fullName, cpf, email, mobilePhone, PASSWORD);
    }

    private static String uniqueEmail() {
        return "contrato." + UUID.randomUUID().toString().substring(0, 8) + "@ovyx.com.br";
    }

    private static String uniqueMobilePhone() {
        return "919" + String.format("%08d", Math.floorMod(UUID.randomUUID().hashCode(), 100_000_000));
    }

    private static String validRegistration(String email) {
        return registration("João Pereira de Souza", randomValidCpf(), email, uniqueMobilePhone());
    }

    private MockHttpServletRequestBuilder register(String body) {
        return post(CARETAKERS)
                .with(CsrfHandshake.using(mockMvc))
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder update(UUID id, String body) {
        return put(CARETAKERS + "/" + id)
                .with(CsrfHandshake.using(mockMvc))
                .cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder deactivation(UUID id) {
        return post(CARETAKERS + "/" + id + "/deactivation")
                .with(CsrfHandshake.using(mockMvc))
                .cookie(session);
    }

    private static String updateOf(Caretaker caretaker, Role role) {
        return """
                {
                  "fullName": "%s",
                  "cpf": "%s",
                  "email": "%s",
                  "mobilePhone": "%s",
                  "role": "%s"
                }
                """.formatted(
                caretaker.fullName().value(),
                caretaker.cpf().value(),
                caretaker.email().value(),
                caretaker.mobilePhone().value(),
                role);
    }

    private static JsonNode json(MvcResult result) throws Exception {
        return JsonMapper.builder().build().readTree(result.getResponse().getContentAsString());
    }

    /** Deixa o administrador deste teste como o unico ativo, para a regra do ultimo administrador. */
    private void makeTheOnlyActiveAdministrator() {
        setAside = jdbc.queryForList(OTHER_ACTIVE_ADMINISTRATORS, UUID.class, administrator.id().value());
        setAside.forEach(id -> jdbc.update(DEACTIVATE_BY_ID, id));
    }

    private long caretakersWithEmail(String email) {
        Long result = jdbc.queryForObject(CARETAKERS_WITH_EMAIL, Long.class, email);
        return result == null ? 0 : result;
    }

    // ------------------------------------------------------------------------------ cadastro

    @Test
    @DisplayName("registers a caretaker: 201, Location of the new resource, and the detail without the password")
    void givenValidRegistration_whenRegistering_thenAnswer201WithLocationAndDetail() throws Exception {
        // given
        String email = uniqueEmail();

        // when
        MvcResult result = mockMvc.perform(register(validRegistration(email))).andReturn();

        // then
        JsonNode body = json(result);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo(CARETAKERS + "/" + body.path("id").asString());
        assertThat(body.path("email").asString()).isEqualTo(email);
        assertThat(body.path("status").asString()).isEqualTo("ACTIVE");
        assertThat(body.has("password")).isFalse();
        assertThat(body.has("passwordHash")).isFalse();
    }

    @Test
    @DisplayName("the registered caretaker signs in with the informed password and must change it")
    void givenRegisteredCaretaker_whenSigningInWithTheInformedPassword_thenEnterOwingThePasswordChange()
            throws Exception {
        // given
        String email = uniqueEmail();
        mockMvc.perform(register(validRegistration(email))).andExpect(status().isCreated());

        // when
        ResultActions signIn = signIn(email, PASSWORD);

        // then
        signIn.andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(true));
    }

    @Test
    @DisplayName("reports every invalid field at once, in form order (V-07)")
    void givenEmptyNameInvalidCpfAndMalformedEmail_whenRegistering_thenAnswer400WithTheThreeFieldsInOrder()
            throws Exception {
        // given
        String invalid = registration("", "12345678900", "sem-arroba", uniqueMobilePhone());

        // when
        MvcResult result = mockMvc.perform(register(invalid)).andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getContentType()).startsWith("application/problem+json");
        JsonNode body = json(result);
        assertThat(body.path("code").asString()).isEqualTo("VALIDATION_FAILED");
        assertThat(body.path("details").properties().stream().map(Map.Entry::getKey))
                .containsExactly("fullName", "cpf", "email");
    }

    @Test
    @DisplayName("refuses an email already used by an active caretaker with 409 EMAIL_ALREADY_IN_USE")
    void givenEmailOfAnActiveCaretaker_whenRegistering_thenAnswer409() throws Exception {
        // given
        Caretaker holder = saved(Role.USER);

        // when
        ResultActions response = mockMvc.perform(register(validRegistration(holder.email().value())));

        // then
        response.andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_IN_USE"))
                .andExpect(jsonPath("$.details.email").exists());
    }

    @Test
    @DisplayName("refuses a mobile phone already used by an active caretaker with 409 MOBILE_PHONE_ALREADY_IN_USE")
    void givenMobilePhoneOfAnActiveCaretaker_whenRegistering_thenAnswer409() throws Exception {
        // given
        Caretaker holder = saved(Role.USER);
        String body = registration("João Pereira", randomValidCpf(), uniqueEmail(), holder.mobilePhone().value());

        // when
        ResultActions response = mockMvc.perform(register(body));

        // then
        response.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MOBILE_PHONE_ALREADY_IN_USE"));
    }

    @Test
    @DisplayName("refuses an unavailable response format before registering anyone")
    void givenValidRegistrationAskingForXml_whenRegistering_thenAnswer406AndRegisterNobody() throws Exception {
        // given
        String email = uniqueEmail();

        // when
        ResultActions response = mockMvc.perform(register(validRegistration(email)).accept(MediaType.APPLICATION_XML));

        // then
        response.andExpect(status().isNotAcceptable());
        assertThat(caretakersWithEmail(email)).isZero();
    }

    // --------------------------------------------------------------------- consulta e busca

    @Test
    @DisplayName("finds a caretaker by part of the name, ignoring case (scenario 5)")
    void givenRegisteredCaretaker_whenSearchingByPartOfTheNameInAnotherCase_thenFindIt() throws Exception {
        // given
        String mark = "Z" + UUID.randomUUID().toString().replaceAll("[^a-f]", "");
        mockMvc.perform(register(registration("Ana " + mark + " Pereira", randomValidCpf(), uniqueEmail(), uniqueMobilePhone())))
                .andExpect(status().isCreated());

        // when
        ResultActions response = mockMvc.perform(get(CARETAKERS).param("name", mark.toUpperCase(Locale.ROOT)).cookie(session));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Ana " + mark + " Pereira"))
                .andExpect(jsonPath("$.content[0].cpf").doesNotExist());
    }

    @Test
    @DisplayName("refuses a page size above the contract with 400")
    void givenPageSizeAboveTheContract_whenSearching_thenAnswer400OnTheSize() throws Exception {
        // given
        String tooLarge = "101";

        // when
        ResultActions response = mockMvc.perform(get(CARETAKERS).param("size", tooLarge).cookie(session));

        // then
        response.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.size").exists());
    }

    @Test
    @DisplayName("consults a caretaker by id")
    void givenExistingCaretaker_whenConsulting_thenAnswer200WithTheDetail() throws Exception {
        // given
        Caretaker caretaker = saved(Role.USER);

        // when
        ResultActions response = mockMvc.perform(get(CARETAKERS + "/" + caretaker.id().value()).cookie(session));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(caretaker.cpf().value()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("answers 404 CARETAKER_NOT_FOUND for an unknown caretaker")
    void givenUnknownCaretaker_whenConsulting_thenAnswer404() throws Exception {
        // given
        UUID unknown = UUID.randomUUID();

        // when
        ResultActions response = mockMvc.perform(get(CARETAKERS + "/" + unknown).cookie(session));

        // then
        response.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CARETAKER_NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value(CARETAKERS + "/" + unknown));
    }

    @Test
    @DisplayName("answers 400 for an identifier that is not a UUID")
    void givenMalformedIdentifier_whenConsulting_thenAnswer400() throws Exception {
        // given
        String malformed = "nao-e-um-uuid";

        // when
        ResultActions response = mockMvc.perform(get(CARETAKERS + "/" + malformed).cookie(session));

        // then
        response.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    // ------------------------------------------------------------------------------- edicao

    @Test
    @DisplayName("refuses an unavailable response format before changing anyone")
    void givenValidUpdateAskingForXml_whenUpdating_thenAnswer406AndChangeNothing() throws Exception {
        // given
        Caretaker caretaker = saved(Role.USER);

        // when
        ResultActions response = mockMvc.perform(update(caretaker.id().value(), updateOf(caretaker, Role.ADMINISTRATOR))
                .accept(MediaType.APPLICATION_XML));

        // then
        response.andExpect(status().isNotAcceptable());
        assertThat(caretakerRepository.findById(caretaker.id()).orElseThrow().role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("promotes a common user to administrator (scenario 7)")
    void givenCommonUser_whenPromotingToAdministrator_thenAnswer200WithTheNewRole() throws Exception {
        // given
        Caretaker caretaker = saved(Role.USER);

        // when
        ResultActions response = mockMvc.perform(update(caretaker.id().value(), updateOf(caretaker, Role.ADMINISTRATOR)));

        // then
        response.andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMINISTRATOR"));
    }

    @Test
    @DisplayName("refuses demoting the last active administrator with 409 LAST_ADMINISTRATOR on the role (V-10)")
    void givenTheOnlyActiveAdministrator_whenDemotingIt_thenAnswer409OnTheRole() throws Exception {
        // given
        makeTheOnlyActiveAdministrator();

        // when
        ResultActions response = mockMvc.perform(update(administrator.id().value(), updateOf(administrator, Role.USER)));

        // then
        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_ADMINISTRATOR"))
                .andExpect(jsonPath("$.details.role").exists());
    }

    // --------------------------------------------------------------------------- inativacao

    @Test
    @DisplayName("refuses an unavailable response format before deactivating anyone")
    void givenActiveCaretakerAskingForXml_whenDeactivating_thenAnswer406AndKeepItActive() throws Exception {
        // given
        Caretaker caretaker = saved(Role.USER);

        // when
        ResultActions response =
                mockMvc.perform(deactivation(caretaker.id().value()).accept(MediaType.APPLICATION_XML));

        // then
        response.andExpect(status().isNotAcceptable());
        assertThat(caretakerRepository.findById(caretaker.id()).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("deactivates a caretaker, who can no longer sign in (scenario 6)")
    void givenActiveCaretaker_whenDeactivating_thenAnswer200AndRefuseItsSignIn() throws Exception {
        // given
        Caretaker caretaker = saved(Role.USER);

        // when
        ResultActions response = mockMvc.perform(deactivation(caretaker.id().value()));

        // then
        response.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        signIn(caretaker.email().value(), DEFAULT_PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refuses deactivating the last active administrator with 409 LAST_ADMINISTRATOR on the status (V-10)")
    void givenTheOnlyActiveAdministrator_whenDeactivatingIt_thenAnswer409OnTheStatus() throws Exception {
        // given
        makeTheOnlyActiveAdministrator();

        // when
        ResultActions response = mockMvc.perform(deactivation(administrator.id().value()));

        // then
        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_ADMINISTRATOR"))
                .andExpect(jsonPath("$.details.status").exists())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
