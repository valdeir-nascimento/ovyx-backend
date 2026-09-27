package io.github.ovyx.identity.presentation.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.github.ovyx.CsrfHandshake;
import io.github.ovyx.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A interface de documentação chama a API de outra origem (FR-026, FR-028; V-16): ela vive na
 * porta de gerenciamento, e a API, na da aplicação. Só a origem configurada da documentação passa,
 * com credenciais, e só com os métodos e cabeçalhos que a API usa.
 */
@SpringBootTest(properties = "ovyx.api-docs.allowed-origins=" + ApiDocsCorsIT.DOCS)
@AutoConfigureMockMvc
@DisplayName("API docs CORS")
class ApiDocsCorsIT extends IntegrationTestSupport {

    static final String DOCS = "http://localhost:9090";

    @Autowired
    private MockMvc mockMvc;

    private MockHttpServletResponse preflight(String origin, String method, String path) throws Exception {
        return mockMvc.perform(options(path)
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-xsrf-token"))
                .andReturn()
                .getResponse();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"POST /api/v1/auth/sign-in", "PUT /api/v1/me/password", "POST /api/v1/caretakers"})
    @DisplayName("lets the documentation origin call the API with the session cookie and the CSRF header")
    void givenDocumentationOrigin_whenPreflightingAnOperation_thenAllowItWithCredentials(String operation)
            throws Exception {
        // given
        String[] parts = operation.split(" ");

        // when
        MockHttpServletResponse response = preflight(DOCS, parts[0], parts[1]);

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(DOCS);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                .containsIgnoringCase("x-xsrf-token")
                .containsIgnoringCase("content-type");
    }

    @Test
    @DisplayName("refuses any other origin")
    void givenAnotherOrigin_whenPreflightingTheSignIn_thenRefuseIt() throws Exception {
        // given
        // Com credenciais, uma origem qualquer liberada seria um site alheio agindo com a sessão de
        // quem o visita.
        String stranger = "https://granja-falsa.example";

        // when
        MockHttpServletResponse response = preflight(stranger, "POST", "/api/v1/auth/sign-in");

        // then
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    @Test
    @DisplayName("refuses another origin with the error body the contract promises")
    void givenAnotherOrigin_whenPreflightingTheSignIn_thenAnswerTheGenericForbiddenProblem() throws Exception {
        // given
        // A recusa de CORS do Spring saía em texto ("Invalid CORS request"), fora do RFC 9457 que o
        // documento promete a todo erro. Agora é o mesmo 403 genérico da autorização (FR-010).
        String stranger = "https://granja-falsa.example";

        // when
        MockHttpServletResponse response = preflight(stranger, "POST", "/api/v1/auth/sign-in");

        // then
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .contains("\"code\":\"FORBIDDEN\"")
                .contains("Você não tem permissão para executar esta operação.")
                .contains("\"instance\":\"/api/v1/auth/sign-in\"");
    }

    @Test
    @DisplayName("keeps answering the application itself when CORS is on")
    void givenRequestFromTheApiOwnOrigin_whenSigningInWithCorsOn_thenReachTheSignIn() throws Exception {
        // given
        // Com o CORS ligado, uma requisição da própria origem não é de CORS e segue direto. O Angular
        // chama pelo proxy, que preserva o Host; se ele passasse a parecer outra origem, voltaria 403.
        String ownOrigin = "http://localhost";

        // when
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/sign-in")
                        .with(CsrfHandshake.using(mockMvc))
                        .header(HttpHeaders.ORIGIN, ownOrigin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier": "ninguem@ovyx.com.br", "password": "SenhaQualquer2026"}
                                """))
                .andReturn()
                .getResponse();

        // then
        assertThat(response.getStatus()).as("a entrada respondeu, e não o CORS").isEqualTo(401);
    }

    @Test
    @DisplayName("refuses a method the API does not use, even from the documentation origin")
    void givenDocumentationOrigin_whenPreflightingADelete_thenRefuseIt() throws Exception {
        // given
        String unusedMethod = "DELETE";

        // when
        MockHttpServletResponse response = preflight(DOCS, unusedMethod, "/api/v1/caretakers");

        // then
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("lets the documentation read the answer, also when it is a refusal")
    void givenDocumentationOrigin_whenAskingWhoIsInTheSessionWithoutOne_thenExposeThe401() throws Exception {
        // given
        // A interface precisa mostrar o 401 com o corpo do contrato; sem os cabeçalhos, o navegador o
        // esconderia como falha de rede.

        // when
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.ORIGIN, DOCS))
                .andReturn()
                .getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(DOCS);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
    }

    @Test
    @DisplayName("exposes the Location header in every answer to the documentation origin")
    void givenDocumentationOrigin_whenReadingAnAnswer_thenExposeTheLocationHeader() throws Exception {
        // given
        // O 201 do cadastro documenta o Location; sem a exposição, o navegador o esconde da interface.
        // A exposição vale para toda resposta à origem da documentação, e a leitura da identidade basta
        // para prová-la.

        // when
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.ORIGIN, DOCS))
                .andReturn()
                .getResponse();

        // then
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).containsIgnoringCase("Location");
    }
}
