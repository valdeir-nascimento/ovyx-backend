package io.github.ovyx.production.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.ovyx.IntegrationSessions;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Quem pode o quê nos relatórios (FR-019, FR-020; ProductionRouteAuthorization): qualquer responsável
 * autenticado, usuário comum ou administrador, faz as nove operações, e ninguém sem sessão. Quem deve a
 * troca de senha só troca a senha, a escrita sem o token CSRF é recusada, e o relatório de um setor
 * inativo só é consultado.
 */
@AutoConfigureMockMvc
@DisplayName("Production authorization")
class ProductionAuthorizationIT extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaretakerRepository caretakerRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private Clock clock;

    @Autowired
    private JdbcTemplate jdbc;

    private IntegrationSessions sessions;
    private ProductionFixtures fixtures;
    private UUID sectorId;

    @BeforeEach
    void setUp() {
        sessions = new IntegrationSessions(mockMvc, caretakerRepository, passwordHasher, clock);
        fixtures = new ProductionFixtures(jdbc);
        sectorId = fixtures.sectorWithTwoCages();
    }

    private String reports() {
        return "/api/v1/sectors/" + sectorId + "/daily-reports";
    }

    private static String generalData(String date) {
        return """
                {"collectionDate": "%s", "collectionTime": "06:42", "openingBirdCount": 98, "flockAge": 20}
                """.formatted(date);
    }

    /** O caminho de um relatório de 01/09 aberto pela própria API, e o da B-07 nele. */
    private record Report(String path, String b07) {}

    private Report openedReport(Cookie[] cookies) throws Exception {
        String opened = mockMvc.perform(post(reports())
                        .with(sessions.csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generalData("2026-09-01")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String path = reports() + "/" + JsonPath.read(opened, "$.id");
        return new Report(path, path + "/cages/" + JsonPath.read(opened, "$.cages[1].cageId"));
    }

    /** As quatro leituras: a lista, a sugestão, o relatório e a gaiola. */
    private List<MockHttpServletRequestBuilder> readsOf(Report report) {
        return List.of(get(reports()), get(reports() + "/suggestion"), get(report.path()), get(report.b07()));
    }

    /** As cinco escritas: abrir outro dia, corrigir, lançar a produção, confirmar o dia e lançar a mortalidade. */
    private List<MockHttpServletRequestBuilder> writesOf(Report report) {
        return List.of(
                post(reports()).contentType(MediaType.APPLICATION_JSON).content(generalData("2026-09-02")),
                put(report.path()).contentType(MediaType.APPLICATION_JSON).content(generalData("2026-09-01")),
                put(report.b07() + "/production").contentType(MediaType.APPLICATION_JSON).content("{\"eggs\": 45}"),
                post(report.path() + "/mortality-confirmation"),
                put(report.b07() + "/mortality").contentType(MediaType.APPLICATION_JSON).content("{\"deaths\": 1}"));
    }

    /** As nove operações do contrato, as leituras antes das escritas. */
    private List<MockHttpServletRequestBuilder> operationsOf(Report report) {
        List<MockHttpServletRequestBuilder> operations = new ArrayList<>(readsOf(report));
        operations.addAll(writesOf(report));
        return operations;
    }

    /** O status e o código de cada resposta, na ordem das requisições, cada uma ajustada por {@code as}. */
    private List<String> outcomesOf(List<MockHttpServletRequestBuilder> requests, UnaryOperator<MockHttpServletRequestBuilder> as)
            throws Exception {
        List<String> outcomes = new ArrayList<>();
        for (MockHttpServletRequestBuilder request : requests) {
            MockHttpServletResponse response = mockMvc.perform(as.apply(request)).andReturn().getResponse();
            // O código só nas recusas: nas respostas de sucesso, "code" é o da gaiola, e não o de um problema.
            String code = response.getStatus() >= 400 ? " " + JsonPath.read(response.getContentAsString(), "$.code") : "";
            outcomes.add(response.getStatus() + code);
        }
        return outcomes;
    }

    @Test
    @DisplayName("lets a common user do the nine operations")
    void givenCommonUser_whenDoingTheNineOperations_thenLetThemThrough() throws Exception {
        // given
        Cookie[] commonUser = sessions.commonUser().cookies();
        Report report = openedReport(commonUser);

        // when
        List<String> outcomes =
                outcomesOf(operationsOf(report), request -> request.with(sessions.csrf()).cookie(commonUser));

        // then
        assertThat(outcomes).containsExactly("200", "200", "200", "200", "201", "200", "200", "200", "200");
    }

    @Test
    @DisplayName("lets an administrator do the nine operations")
    void givenAdministrator_whenDoingTheNineOperations_thenLetThemThrough() throws Exception {
        // given
        Cookie[] administrator = sessions.administrator().cookies();
        Report report = openedReport(administrator);

        // when
        List<String> outcomes =
                outcomesOf(operationsOf(report), request -> request.with(sessions.csrf()).cookie(administrator));

        // then
        assertThat(outcomes).containsExactly("200", "200", "200", "200", "201", "200", "200", "200", "200");
    }

    @Test
    @DisplayName("refuses a visitor without session on the nine operations: 401")
    void givenVisitorWithoutSession_whenDoingTheNineOperations_thenAnswerUnauthenticated() throws Exception {
        // given
        Report report = openedReport(sessions.commonUser().cookies());

        // when
        List<String> outcomes = outcomesOf(operationsOf(report), request -> request.with(sessions.csrf()));

        // then
        assertThat(outcomes).hasSize(9).allMatch("401 UNAUTHENTICATED"::equals);
    }

    @Test
    @DisplayName("refuses whoever owes the password change on the nine operations: 403")
    void givenUserOwingThePasswordChange_whenDoingTheNineOperations_thenAnswerPasswordChangeRequired()
            throws Exception {
        // given
        Report report = openedReport(sessions.commonUser().cookies());
        Cookie[] owing = sessions.commonUserOwingThePasswordChange().cookies();

        // when
        List<String> outcomes = outcomesOf(operationsOf(report), request -> request.with(sessions.csrf()).cookie(owing));

        // then
        assertThat(outcomes).hasSize(9).allMatch("403 PASSWORD_CHANGE_REQUIRED"::equals);
    }

    @Test
    @DisplayName("refuses the five writes without the CSRF token: 403")
    void givenWritesWithoutCsrfToken_whenWriting_thenAnswerCsrfTokenInvalid() throws Exception {
        // given
        Cookie[] commonUser = sessions.commonUser().cookies();
        Report report = openedReport(commonUser);

        // when
        List<String> outcomes = outcomesOf(writesOf(report), request -> request.cookie(commonUser));

        // then
        assertThat(outcomes).hasSize(5).allMatch("403 CSRF_TOKEN_INVALID"::equals);
    }

    @Test
    @DisplayName("keeps the reports of an inactive sector for consultation only: the writes 409, the reads 200")
    void givenInactiveSector_whenReadingAndWriting_thenRefuseTheWritesAndAnswerTheReads() throws Exception {
        // given
        Cookie[] commonUser = sessions.commonUser().cookies();
        Report report = openedReport(commonUser);
        fixtures.deactivate(sectorId);
        UnaryOperator<MockHttpServletRequestBuilder> asTheUser = request -> request.with(sessions.csrf()).cookie(commonUser);

        // when
        List<String> writes = outcomesOf(writesOf(report), asTheUser);
        List<String> reads = outcomesOf(readsOf(report), asTheUser);

        // then
        assertThat(writes).hasSize(5).allMatch("409 SECTOR_INACTIVE"::equals);
        assertThat(reads).containsExactly("200", "200", "200", "200");
    }
}
