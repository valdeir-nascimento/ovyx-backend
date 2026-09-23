package io.github.ovyx.shared.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Comprova que a porta de gerenciamento responde de verdade.
 *
 * <p>Defeito encontrado em execucao: a cadeia de seguranca da API valia tambem na porta 9090, e
 * tudo ali respondia 401 — inclusive o health check, o que derruba qualquer sonda de orquestracao,
 * e a documentacao da API, que ficava inacessivel.
 *
 * <p>Sobe um servidor de verdade, em portas aleatorias: o MockMvc nao passa pelo servidor separado
 * da porta de gerenciamento, e por isso nao veria este defeito.
 *
 * <p>O endpoint {@code beans} e exposto so aqui, de proposito: ele representa qualquer endpoint que
 * alguem venha a expor no futuro, e prova que expor nao basta para liberar (N2).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.server.port=0",
            "management.endpoints.web.exposure.include=health,info,openapi,swagger-ui,beans"
        })
@DisplayName("Management port")
class ManagementPortIT extends IntegrationTestSupport {

    @LocalManagementPort
    private int managementPort;

    private final HttpClient client =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + managementPort + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("the health check answers without authentication")
    void healthCheckAnswersWithoutAuthentication() throws Exception {
        HttpResponse<String> response = get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    @DisplayName("the OpenAPI document is published in Portuguese")
    void openApiDocumentIsPublished() throws Exception {
        HttpResponse<String> response = get("/actuator/openapi");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Ovyx").contains("/api/v1/auth/sign-in");
    }

    @Test
    @DisplayName("the published document follows the contract on security, tags and CSRF")
    void publishedDocumentFollowsTheContract() throws Exception {
        // Defeito 4 do QA: o documento publicado divergia do contrato — sem o paragrafo de CSRF, sem
        // o cookie de sessao exigido nas operacoes e com a troca de senha sob "Acesso".
        JsonNode document = JsonMapper.builder().build().readTree(get("/actuator/openapi").body());

        assertThat(document.at("/security/0/sessionCookie").isArray()).isTrue();
        assertThat(document.at("/paths/~1api~1v1~1auth~1sign-in/post/security").isArray()).isTrue();
        assertThat(document.at("/paths/~1api~1v1~1auth~1sign-in/post/security").isEmpty())
                .as("a entrada e publica")
                .isTrue();
        assertThat(document.at("/paths/~1api~1v1~1me~1password/put/tags").toString()).isEqualTo("[\"Minha conta\"]");
        assertThat(document.at("/info/description").asString()).contains("X-XSRF-TOKEN").contains("PASSWORD_CHANGE_REQUIRED");
        assertThat(document.at("/tags").toString()).doesNotContain("Responsáveis");
    }

    @Test
    @DisplayName("every operation keeps its summary, tag and documented responses")
    void everyOperationKeepsItsDocumentation() throws Exception {
        // A documentacao vive nas interfaces <Recurso>Api, e nao nos controllers. Se o springdoc
        // deixasse de le-la ali, o documento continuaria publicado, so que vazio de descricoes.
        JsonNode paths = JsonMapper.builder().build().readTree(get("/actuator/openapi").body()).path("paths");

        JsonNode signIn = paths.path("/api/v1/auth/sign-in").path("post");
        JsonNode signOut = paths.path("/api/v1/auth/sign-out").path("post");
        JsonNode me = paths.path("/api/v1/auth/me").path("get");
        JsonNode password = paths.path("/api/v1/me/password").path("put");

        assertThat(signIn.path("summary").asString()).isEqualTo("Entrar no sistema");
        assertThat(signIn.path("tags").toString()).isEqualTo("[\"Acesso\"]");
        assertThat(signOut.path("summary").asString()).isEqualTo("Sair do sistema");
        assertThat(signOut.path("responses").has("204")).isTrue();
        assertThat(me.path("summary").asString()).isEqualTo("Consultar o responsável autenticado");
        assertThat(password.path("summary").asString()).isEqualTo("Trocar a própria senha");
        assertThat(password.path("responses").has("204")).isTrue();
    }

    @Test
    @DisplayName("every operation publishes the error responses it can produce")
    void everyOperationPublishesItsErrorResponses() throws Exception {
        // FR-027: documentação sem resposta de erro deixa quem integra descobrindo os códigos por
        // tentativa. O documento saía só com 200 e 204.
        JsonNode document = JsonMapper.builder().build().readTree(get("/actuator/openapi").body());
        JsonNode paths = document.path("paths");

        JsonNode signIn = paths.path("/api/v1/auth/sign-in").path("post").path("responses");
        JsonNode me = paths.path("/api/v1/auth/me").path("get").path("responses");
        JsonNode password = paths.path("/api/v1/me/password").path("put").path("responses");

        assertThat(signIn.has("400")).isTrue();
        assertThat(signIn.has("401")).isTrue();
        assertThat(signIn.has("403")).isTrue();
        assertThat(me.has("401")).isTrue();
        assertThat(password.has("400")).isTrue();
        assertThat(document.at("/components/schemas/Problem/properties/code").isObject())
                .as("o corpo de erro precisa estar publicado, com o código da regra")
                .isTrue();
        assertThat(document.at("/components/schemas/Problem/properties/details").isObject())
                .isTrue();
        assertThat(document.at("/components/schemas/Problem/properties/instance").isObject())
                .as("o caminho da requisição faz parte do corpo de erro")
                .isTrue();
    }

    /** As quatro operações publicadas, pelo ponteiro do documento e pelo caminho que elas atendem. */
    private static final Map<String, String> OPERATIONS = Map.of(
            "/paths/~1api~1v1~1auth~1sign-in/post", "/api/v1/auth/sign-in",
            "/paths/~1api~1v1~1auth~1sign-out/post", "/api/v1/auth/sign-out",
            "/paths/~1api~1v1~1auth~1me/get", "/api/v1/auth/me",
            "/paths/~1api~1v1~1me~1password/put", "/api/v1/me/password");

    @Test
    @DisplayName("every error response of every operation carries its own example")
    void everyErrorResponseCarriesItsOwnExample() throws Exception {
        // Sem exemplo por resposta, a interface repetia o exemplo do schema em todo status: o 401
        // aparecia com o corpo de uma validação, dizendo status 400. Depois disso, o exemplo de uma
        // operação foi emprestado a outra, e o `instance` passou a apontar o endpoint errado.
        // As duas vezes o defeito era o exemplo contradizer a própria resposta.
        JsonNode document = JsonMapper.builder().build().readTree(get("/actuator/openapi").body());

        assertThat(OPERATIONS).allSatisfy((pointer, path) -> {
            JsonNode responses = document.at(pointer).path("responses");

            assertThat(responses.properties()).isNotEmpty();
            responses.properties().forEach(response -> {
                int status = Integer.parseInt(response.getKey());
                if (status < 400) {
                    return;
                }
                eachExampleOf(response.getValue(), response.getKey()).forEach(example -> {
                    assertThat(example.path("status").asInt())
                            .as("%s %s: o exemplo anuncia outro status", path, response.getKey())
                            .isEqualTo(status);
                    assertThat(example.path("instance").asString())
                            .as("%s %s: o exemplo aponta outro endpoint", path, response.getKey())
                            .isEqualTo(path);
                    assertThat(example.path("code").asString())
                            .as("%s %s: exemplo sem código da regra", path, response.getKey())
                            .isNotBlank();
                });
            });
        });
    }

    @Test
    @DisplayName("no operation announces a body where there is none, and success is JSON")
    void successResponsesDeclareTheRightBody() throws Exception {
        JsonNode document = JsonMapper.builder().build().readTree(get("/actuator/openapi").body());

        JsonNode signOut204 = document.at("/paths/~1api~1v1~1auth~1sign-out/post/responses/204");
        JsonNode password204 = document.at("/paths/~1api~1v1~1me~1password/put/responses/204");
        JsonNode signIn200 = document.at("/paths/~1api~1v1~1auth~1sign-in/post/responses/200/content");
        JsonNode me200 = document.at("/paths/~1api~1v1~1auth~1me/get/responses/200/content");

        assertThat(signOut204.has("content")).as("204 não tem corpo").isFalse();
        assertThat(password204.has("content")).as("204 não tem corpo").isFalse();
        assertThat(signIn200.has("application/json")).isTrue();
        assertThat(me200.has("application/json")).isTrue();
    }

    /**
     * Todos os exemplos publicados para o status.
     *
     * <p>O springdoc publica um exemplo sem nome em {@code example}, no singular, e os nomeados em
     * {@code examples}.
     */
    private static List<JsonNode> eachExampleOf(JsonNode response, String status) {
        JsonNode content = response.at("/content/application~1problem+json");
        JsonNode single = content.path("example");
        JsonNode named = content.path("examples");

        if (single.isObject()) {
            return List.of(single);
        }
        assertThat(named.isObject()).as("resposta %s sem exemplo próprio", status).isTrue();
        return named.properties().stream().map(entry -> entry.getValue().path("value")).toList();
    }

    @Test
    @DisplayName("the Swagger UI assets and configuration are reachable")
    void swaggerUiAssetsAreReachable() throws Exception {
        // A liberacao passou a ser por endpoint. Os arquivos da interface vivem abaixo do caminho
        // do endpoint e precisam continuar alcancaveis, ou a pagina abre em branco.
        assertThat(get("/actuator/swagger-ui/swagger-initializer.js").statusCode()).isEqualTo(200);
        assertThat(get("/actuator/swagger-ui/swagger-config").statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("an exposed endpoint outside the documented list is denied")
    void endpointOutsideTheListIsDenied() throws Exception {
        // Antes, a cadeia da porta de gerenciamento liberava qualquer endpoint exposto: bastava
        // alguem acrescentar "beans" ou "env" a exposicao para publica-lo sem autenticacao.
        HttpResponse<String> response = get("/actuator/beans");

        assertThat(response.statusCode()).isIn(401, 403);
        assertThat(response.body()).doesNotContain("ovyxOpenApi");
    }

    @Test
    @DisplayName("the Swagger UI is reachable")
    void swaggerUiIsReachable() throws Exception {
        HttpResponse<String> response = get("/actuator/swagger-ui");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).containsIgnoringCase("swagger");
    }
}
