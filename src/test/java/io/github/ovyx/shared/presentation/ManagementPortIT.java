package io.github.ovyx.shared.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
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

    private static final String SIGN_IN = "/paths/~1api~1v1~1auth~1sign-in/post";
    private static final String SIGN_OUT = "/paths/~1api~1v1~1auth~1sign-out/post";
    private static final String ME = "/paths/~1api~1v1~1auth~1me/get";
    private static final String PASSWORD = "/paths/~1api~1v1~1me~1password/put";

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");

    /** Uma operacao publicada: o ponteiro no documento, o metodo e o caminho — talvez com variaveis. */
    private record Operation(String pointer, String method, String path) {

        String label() {
            return method.toUpperCase(Locale.ROOT) + " " + path;
        }

        /** O {@code instance} de um exemplo e um caminho concreto; o da operacao pode ter {@code {id}}. */
        boolean matches(String instance) {
            return instance.matches(path.replaceAll("\\{[^/]+}", "[^/]+"));
        }
    }

    /** Um exemplo de resposta de erro, com o que ele precisa respeitar: status e caminho da operacao. */
    private record ErrorExample(Operation operation, int status, String name, JsonNode body) {

        String label() {
            return operation.label() + " " + status + " " + name;
        }
    }

    @LocalManagementPort
    private int managementPort;

    @LocalServerPort
    private int serverPort;

    private final HttpClient client =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + managementPort + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode document() throws Exception {
        return JsonMapper.builder().build().readTree(get("/actuator/openapi").body());
    }

    /**
     * Todas as operacoes que o documento publica, lidas dele mesmo.
     *
     * <p>Uma lista fixa aqui deixaria de fora, em silencio, toda operacao nova: as da US2 gerariam
     * zero verificacoes e a suite continuaria verde.
     */
    private static List<Operation> operations(JsonNode document) {
        return document.path("paths").properties().stream()
                .flatMap(path -> path.getValue().properties().stream()
                        .filter(method -> HTTP_METHODS.contains(method.getKey()))
                        .map(method -> new Operation(
                                "/paths/" + path.getKey().replace("~", "~0").replace("/", "~1") + "/"
                                        + method.getKey(),
                                method.getKey(),
                                path.getKey())))
                .toList();
    }

    /**
     * Todos os exemplos de erro publicados para a operacao.
     *
     * <p>O springdoc publica um exemplo sem nome em {@code example}, no singular, e os nomeados em
     * {@code examples}. Resposta de erro sem nenhum dos dois vira um exemplo sem corpo, para que o
     * teste gerado para ela reprove dizendo qual e.
     */
    private static List<ErrorExample> errorExamples(JsonNode document, Operation operation) {
        return document.at(operation.pointer()).path("responses").properties().stream()
                .filter(response -> Integer.parseInt(response.getKey()) >= 400)
                .flatMap(response -> examplesOf(operation, response.getKey(), response.getValue()))
                .toList();
    }

    private static Stream<ErrorExample> examplesOf(Operation operation, String status, JsonNode response) {
        JsonNode content = response.at("/content/application~1problem+json");
        int code = Integer.parseInt(status);
        if (content.path("example").isObject()) {
            return Stream.of(new ErrorExample(operation, code, "example", content.path("example")));
        }
        if (!content.path("examples").isObject()) {
            return Stream.of(new ErrorExample(operation, code, "(sem exemplo)", null));
        }
        return content.path("examples").properties().stream()
                .map(named -> new ErrorExample(operation, code, named.getKey(), named.getValue().path("value")));
    }

    @Test
    @DisplayName("the health check answers without authentication")
    void givenAnonymousProbe_whenCallingTheHealthCheck_thenAnswerUp() throws Exception {
        // given
        String health = "/actuator/health";

        // when
        HttpResponse<String> response = get(health);

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    @DisplayName("the OpenAPI document is published in Portuguese")
    void givenAnonymousReader_whenRequestingTheOpenApiDocument_thenPublishIt() throws Exception {
        // given
        String openApi = "/actuator/openapi";

        // when
        HttpResponse<String> response = get(openApi);

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Ovyx").contains("/api/v1/auth/sign-in");
    }

    @Test
    @DisplayName("the published document requires the session cookie globally")
    void givenPublishedDocument_whenReadingTheGlobalSecurity_thenRequireTheSessionCookie() throws Exception {
        // given
        // Defeito 4 do QA: o documento publicado divergia do contrato — sem o paragrafo de CSRF, sem
        // o cookie de sessao exigido nas operacoes e com a troca de senha sob "Acesso".
        JsonNode document = document();

        // when
        JsonNode sessionCookie = document.at("/security/0/sessionCookie");

        // then
        assertThat(sessionCookie.isArray()).isTrue();
    }

    @Test
    @DisplayName("the published document marks sign-in as public")
    void givenPublishedDocument_whenReadingTheSignInSecurity_thenDeclareItPublic() throws Exception {
        // given
        JsonNode document = document();

        // when
        JsonNode security = document.at(SIGN_IN + "/security");

        // then
        assertThat(security.isArray()).isTrue();
        assertThat(security.isEmpty()).as("a entrada e publica").isTrue();
    }

    @ParameterizedTest(name = "{0} under \"{1}\"")
    @CsvSource({SIGN_IN + ", Acesso", PASSWORD + ", Minha conta"})
    @DisplayName("each operation is published under the tag the contract assigns")
    void givenPublishedDocument_whenReadingAnOperationTag_thenUseTheTagTheContractAssigns(String pointer, String tag)
            throws Exception {
        // given
        JsonNode document = document();

        // when
        JsonNode tags = document.at(pointer + "/tags");

        // then
        assertThat(tags).hasSize(1);
        assertThat(tags.get(0).asString()).isEqualTo(tag);
    }

    @Test
    @DisplayName("the published description explains CSRF and the pending password change")
    void givenPublishedDocument_whenReadingTheDescription_thenExplainCsrfAndThePendingPasswordChange()
            throws Exception {
        // given
        JsonNode document = document();

        // when
        String description = document.at("/info/description").asString();

        // then
        assertThat(description).contains("X-XSRF-TOKEN").contains("PASSWORD_CHANGE_REQUIRED");
    }

    @Test
    @DisplayName("the published description tells what an undeclared route really answers")
    void givenPublishedDocument_whenReadingTheDescription_thenTellWhatAnUndeclaredRouteReallyAnswers()
            throws Exception {
        // given
        // O texto prometeu primeiro um 405 que o sistema nunca devolve, e depois um 403 que so vale
        // para quem tem sessao. A resposta real depende de quem pergunta, e o texto precisa dizer isso.
        JsonNode document = document();

        // when
        String description = document.at("/info/description").asString();

        // then
        assertThat(description)
                .contains("sem sessão, a resposta é 401 `UNAUTHENTICATED`")
                .contains("com sessão, 403 `FORBIDDEN`")
                .contains("sem o cabeçalho `X-XSRF-TOKEN` é recusada antes disso, com 403 `CSRF_TOKEN_INVALID`")
                .contains("`TRACE`, que o próprio servidor recusa com 405");
    }

    @ParameterizedTest(name = "TRACE {0}")
    @ValueSource(strings = {"/api/v1/auth/me", "/api/v1/rota-que-nao-existe"})
    @DisplayName("TRACE is refused by the server itself with 405, as the published description warns")
    void givenTraceRequest_whenReachingTheApplicationPort_thenTheServerAnswers405(String path) throws Exception {
        // given
        // O texto publicado diz que rota ou metodo nao declarados nao produzem 405, com esta excecao. O
        // TRACE e recusado pelo Tomcat antes da cadeia de seguranca, e por isso so aparece com um
        // servidor de verdade: o MockMvc nao passa por ele.
        HttpRequest trace = HttpRequest.newBuilder(URI.create("http://localhost:" + serverPort + path))
                .method("TRACE", HttpRequest.BodyPublishers.noBody())
                .build();

        // when
        HttpResponse<String> response = client.send(trace, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(response.statusCode()).isEqualTo(405);
    }

    @Test
    @DisplayName("no caretaker administration tag is published before the story that delivers it")
    void givenPublishedDocument_whenReadingTheTags_thenPublishNoCaretakerAdministrationYet() throws Exception {
        // given
        JsonNode document = document();

        // when
        String tags = document.at("/tags").toString();

        // then
        assertThat(tags).doesNotContain("Responsáveis");
    }

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource({
        SIGN_IN + ", Entrar no sistema",
        SIGN_OUT + ", Sair do sistema",
        ME + ", Consultar o responsável autenticado",
        PASSWORD + ", Trocar a própria senha"
    })
    @DisplayName("every operation keeps its summary")
    void givenPublishedDocument_whenReadingAnOperation_thenKeepItsSummary(String pointer, String summary)
            throws Exception {
        // given
        // A documentacao vive nas interfaces <Recurso>Api, e nao nos controllers. Se o springdoc
        // deixasse de le-la ali, o documento continuaria publicado, so que vazio de descricoes.
        JsonNode document = document();

        // when
        String published = document.at(pointer + "/summary").asString();

        // then
        assertThat(published).isEqualTo(summary);
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
        SIGN_IN + ", 400",
        SIGN_IN + ", 401",
        SIGN_IN + ", 403",
        SIGN_IN + ", 406",
        ME + ", 401",
        ME + ", 406",
        PASSWORD + ", 400"
    })
    @DisplayName("every operation publishes the error responses it can produce")
    void givenPublishedDocument_whenReadingAnOperationResponses_thenPublishTheErrorsItCanProduce(
            String pointer, String status) throws Exception {
        // given
        // FR-027: documentacao sem resposta de erro deixa quem integra descobrindo os codigos por
        // tentativa. O documento saia so com 200 e 204.
        JsonNode document = document();

        // when
        JsonNode responses = document.at(pointer + "/responses");

        // then
        assertThat(responses.has(status)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"code", "details", "instance"})
    @DisplayName("the published error body carries the rule code, the field details and the request path")
    void givenPublishedDocument_whenReadingTheProblemSchema_thenDeclareTheProperty(String property)
            throws Exception {
        // given
        JsonNode document = document();

        // when
        JsonNode declared = document.at("/components/schemas/Problem/properties/" + property);

        // then
        assertThat(declared.isObject()).isTrue();
    }

    @Test
    @DisplayName("the operations under verification are read from the published document")
    void givenPublishedDocument_whenListingItsOperations_thenFindEveryPublishedOperation() throws Exception {
        // given
        // Guarda do que vem abaixo: se a leitura das operacoes quebrasse e devolvesse nada, os testes
        // dinamicos seriam zero e passariam em silencio.
        JsonNode document = document();

        // when
        List<String> operations = operations(document).stream().map(Operation::label).toList();

        // then
        assertThat(operations)
                .contains(
                        "POST /api/v1/auth/sign-in",
                        "POST /api/v1/auth/sign-out",
                        "GET /api/v1/auth/me",
                        "PUT /api/v1/me/password");
    }

    @TestFactory
    @DisplayName("every published operation publishes at least one error example")
    Stream<DynamicTest> givenPublishedDocument_whenCollectingEachOperationErrorExamples_thenFindAtLeastOne()
            throws Exception {
        // given
        // Uma operacao sem nenhum exemplo de erro nao geraria teste algum na verificacao seguinte, e
        // passaria em silencio.
        JsonNode document = document();

        // when
        List<Operation> operations = operations(document);

        // then
        return operations.stream().map(operation -> DynamicTest.dynamicTest(
                operation.label(), () -> assertThat(errorExamples(document, operation)).isNotEmpty()));
    }

    @TestFactory
    @DisplayName("every error example matches the status, the endpoint and carries a rule code")
    Stream<DynamicTest> givenPublishedDocument_whenReadingEveryErrorExample_thenEachMatchesItsOwnResponse()
            throws Exception {
        // given
        // Sem exemplo por resposta, a interface repetia o exemplo do schema em todo status: o 401
        // aparecia com o corpo de uma validacao, dizendo status 400. Depois disso, o exemplo de uma
        // operacao foi emprestado a outra, e o `instance` passou a apontar o endpoint errado.
        // As duas vezes o defeito era o exemplo contradizer a propria resposta.
        JsonNode document = document();

        // when
        List<ErrorExample> examples = operations(document).stream()
                .flatMap(operation -> errorExamples(document, operation).stream())
                .toList();

        // then
        return examples.stream().map(example -> DynamicTest.dynamicTest(example.label(), () -> {
            assertThat(example.body()).as("resposta sem exemplo próprio").isNotNull();
            assertThat(example.body().path("status").asInt())
                    .as("o exemplo anuncia outro status")
                    .isEqualTo(example.status());
            assertThat(example.body().path("instance").asString())
                    .as("o exemplo aponta outro endpoint")
                    .matches(example.operation()::matches);
            assertThat(example.body().path("code").asString())
                    .as("exemplo sem código da regra")
                    .isNotBlank();
        }));
    }

    @ParameterizedTest
    @ValueSource(strings = {SIGN_OUT + "/responses/204", PASSWORD + "/responses/204"})
    @DisplayName("no operation announces a body where there is none")
    void givenPublishedDocument_whenReadingA204Response_thenAnnounceNoBody(String pointer) throws Exception {
        // given
        JsonNode document = document();

        // when
        JsonNode noContent = document.at(pointer);

        // then
        assertThat(noContent.has("content")).as("204 não tem corpo").isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {SIGN_IN + "/responses/200/content", ME + "/responses/200/content"})
    @DisplayName("every successful body is published as JSON")
    void givenPublishedDocument_whenReadingA200Response_thenPublishItAsJson(String pointer) throws Exception {
        // given
        JsonNode document = document();

        // when
        JsonNode content = document.at(pointer);

        // then
        assertThat(content.has("application/json")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/swagger-ui/swagger-initializer.js", "/actuator/swagger-ui/swagger-config"})
    @DisplayName("the Swagger UI assets and configuration are reachable")
    void givenSwaggerUiAsset_whenRequestingIt_thenAnswerOk(String asset) throws Exception {
        // given — asset from @ValueSource
        // A liberacao passou a ser por endpoint. Os arquivos da interface vivem abaixo do caminho
        // do endpoint e precisam continuar alcancaveis, ou a pagina abre em branco.

        // when
        HttpResponse<String> response = get(asset);

        // then
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("an exposed endpoint outside the documented list is denied")
    void givenExposedEndpointOutsideTheList_whenRequestingIt_thenDenyWithoutLeakingItsContent() throws Exception {
        // given
        // Antes, a cadeia da porta de gerenciamento liberava qualquer endpoint exposto: bastava
        // alguem acrescentar "beans" ou "env" a exposicao para publica-lo sem autenticacao.
        String beans = "/actuator/beans";

        // when
        HttpResponse<String> response = get(beans);

        // then
        assertThat(response.statusCode()).isIn(401, 403);
        assertThat(response.body()).doesNotContain("ovyxOpenApi");
    }

    @Test
    @DisplayName("the Swagger UI is reachable")
    void givenAnonymousReader_whenOpeningTheSwaggerUi_thenServeThePage() throws Exception {
        // given
        String swaggerUi = "/actuator/swagger-ui";

        // when
        HttpResponse<String> response = get(swaggerUi);

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).containsIgnoringCase("swagger");
    }
}
