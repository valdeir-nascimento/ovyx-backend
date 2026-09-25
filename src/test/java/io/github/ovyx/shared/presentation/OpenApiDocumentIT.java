package io.github.ovyx.shared.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.ovyx.IntegrationTestSupport;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * O documento da API ensina a usá-la sem ler o código (US5 da 001; FR-027 a FR-029, SC-007, SC-008;
 * FR-021 e R-012 da 002): um documento só, com a união dos contratos {@code identity-api.yaml} e
 * {@code farm-api.yaml}, exemplo real em toda requisição e em toda resposta, nada que a interface
 * precise inventar, e uma interface que consegue chamar a API.
 *
 * <p>Os contratos de referência são as cópias em {@code src/test/resources/contract/}: a pasta
 * {@code specs/} fica fora do repositório. Onde ela existe, um teste exige que cada cópia seja igual à
 * original, para as duas não se separarem em silêncio.
 *
 * <p>Mesma configuração do {@link ManagementPortIT}, para reaproveitar o contexto: um servidor de
 * verdade, com a documentação na porta de gerenciamento.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.server.port=0",
            "management.endpoints.web.exposure.include=health,info,openapi,swagger-ui,beans"
        })
@DisplayName("OpenAPI document")
class OpenApiDocumentIT extends IntegrationTestSupport {

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");

    /** Valores que o Swagger UI inventa quando o documento não traz exemplo. */
    private static final List<String> PLACEHOLDERS = List.of("string", "additionalProp");

    /**
     * Os únicos campos em que zero ou booleano é exemplo de verdade, e não valor genérico: a primeira
     * página é a 0; o responsável tem {@code mustChangePassword} verdadeiro ou falso; e o setor
     * recém-cadastrado, ou inativado, tem zero gaiolas ativas e zero aves. Um campo novo com zero ou
     * booleano reprova até alguém decidir que o valor é legítimo e acrescentá-lo aqui.
     *
     * <p>Os campos das operações de gaiola levam o prefixo {@code cage.}: as aves zeradas do setor não
     * liberam um exemplo genérico de gaiola com zero aves.
     */
    private static final Set<String> FIELDS_WITH_ZERO_OR_BOOLEAN_EXAMPLE =
            Set.of("page", "mustChangePassword", "activeCageCount", "birdCount", "cage.page");

    private static final String IDENTITY_CONTRACT = "identity-api.yaml";
    private static final String FARM_CONTRACT = "farm-api.yaml";

    /** Os contratos originais, fora do repositório; existem na máquina de quem mantém as specs. */
    private static final Map<String, Path> SPEC_CONTRACTS = Map.of(
            IDENTITY_CONTRACT, Path.of("../specs/001-auth-foundation/contracts/identity-api.yaml"),
            FARM_CONTRACT, Path.of("../specs/002-sectors-cages/contracts/farm-api.yaml"));

    /** O título do documento único (R-012 da 002): nenhum dos dois contratos é o documento inteiro. */
    private static final String PUBLISHED_TITLE = "Ovyx — API";

    /**
     * O parágrafo do contrato do farm que o documento único dispensa: ele remete à API de Identidade,
     * que no documento único é o próprio texto acima dele.
     */
    private static final String FARM_PARAGRAPH_ABOUT_THE_IDENTITY_API = "**Sessão, erros, CSRF e acesso negado**";

    @LocalManagementPort
    private int managementPort;

    private final HttpClient client =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    private String get(String path) throws Exception {
        return get("localhost", path);
    }

    private String get(String host, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://" + host + ":" + managementPort + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private JsonNode document() throws Exception {
        return JsonMapper.builder().build().readTree(get("/actuator/openapi"));
    }

    /** Uma operação: o rótulo "MÉTODO caminho" e o nó dela no documento. */
    private record Operation(String label, JsonNode node) {}

    private static List<Operation> operations(JsonNode document) {
        return document.path("paths").properties().stream()
                .flatMap(path -> path.getValue().properties().stream()
                        .filter(method -> HTTP_METHODS.contains(method.getKey()))
                        .map(method -> new Operation(
                                method.getKey().toUpperCase(Locale.ROOT) + " " + path.getKey(), method.getValue())))
                .toList();
    }

    /** O esquema de um conteúdo, seguindo a referência para {@code components/schemas}. */
    private static JsonNode resolve(JsonNode document, JsonNode schema) {
        String reference = schema.path("$ref").asString("");
        return reference.isEmpty() ? schema : document.at(reference.substring(1));
    }

    /**
     * Os campos sem exemplo de um esquema, e dos esquemas que ele contém (a página contém os itens).
     * Um campo sem exemplo é o que o Swagger UI preenche com {@code "string"}, {@code 0} ou {@code true}.
     */
    private static List<String> fieldsWithoutExample(JsonNode document, JsonNode schema, String where) {
        JsonNode resolved = resolve(document, schema);
        List<String> missing = new ArrayList<>();
        resolved.path("properties").properties().forEach(property -> {
            JsonNode field = property.getValue();
            String name = where + "." + property.getKey();
            if (field.has("$ref")) {
                missing.addAll(fieldsWithoutExample(document, field, name));
            } else if ("array".equals(field.path("type").asString(""))) {
                missing.addAll(fieldsWithoutExample(document, field.path("items"), name + "[]"));
            } else if (!field.has("example")) {
                missing.add(name);
            }
        });
        return missing;
    }

    /** Todos os valores de exemplo do documento: os de campo e os de corpo inteiro, com os nomes de campo. */
    private static List<String> exampleValues(JsonNode node) {
        List<String> values = new ArrayList<>();
        collectExamples(node, false, values);
        return values;
    }

    private static void collectExamples(JsonNode node, boolean insideExample, List<String> values) {
        if (node.isObject()) {
            node.properties().forEach(entry -> {
                boolean example = insideExample || entry.getKey().equals("example") || entry.getKey().equals("examples");
                if (insideExample) {
                    values.add(entry.getKey());
                }
                collectExamples(entry.getValue(), example, values);
            });
        } else if (node.isArray()) {
            node.forEach(item -> collectExamples(item, insideExample, values));
        } else if (insideExample) {
            values.add(node.asString(""));
        }
    }

    /**
     * Os nomes de campo cujo exemplo é zero ou booleano, em qualquer exemplo do documento; nas operações
     * de gaiola, com o prefixo {@code cage.}.
     */
    private static List<String> fieldsWithZeroOrBoolean(JsonNode node) {
        List<String> fields = new ArrayList<>();
        node.properties().forEach(entry -> {
            if (entry.getKey().equals("paths")) {
                entry.getValue().properties().forEach(path -> {
                    List<String> found = new ArrayList<>();
                    collectZeroOrBoolean(path.getValue(), "", false, found);
                    String prefix = path.getKey().contains("/cages") ? "cage." : "";
                    found.forEach(field -> fields.add(prefix + field));
                });
            } else {
                collectZeroOrBoolean(entry.getValue(), entry.getKey(), false, fields);
            }
        });
        return fields;
    }

    private static void collectZeroOrBoolean(JsonNode node, String field, boolean insideExample, List<String> out) {
        if (node.isObject()) {
            // Um parâmetro responde pelo próprio nome; um campo de esquema, pela chave que o contém.
            String owner = node.has("in") && node.has("name") ? node.path("name").asString("") : field;
            node.properties().forEach(entry -> {
                boolean example = entry.getKey().equals("example") || entry.getKey().equals("examples");
                String name = insideExample || !example ? entry.getKey() : owner;
                collectZeroOrBoolean(entry.getValue(), name, insideExample || example, out);
            });
        } else if (node.isArray()) {
            node.forEach(item -> collectZeroOrBoolean(item, field, insideExample, out));
        } else if (insideExample && (node.isBoolean() || (node.isNumber() && node.asDouble() == 0))) {
            out.add(field);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> contract(String file) throws Exception {
        try (InputStream contract = OpenApiDocumentIT.class.getResourceAsStream("/contract/" + file)) {
            return new Yaml().load(contract);
        }
    }

    /** O que o documento publicado precisa conter: os dois contratos, inteiros (T097). */
    private static List<Map<String, Object>> expectedContracts() throws Exception {
        return List.of(contract(IDENTITY_CONTRACT), contract(FARM_CONTRACT));
    }

    /** As operações de um contrato, como mapas, na ordem dos caminhos. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> operationsOf(Map<String, Object> document) {
        return ((Map<String, Map<String, Object>>) document.get("paths")).values().stream()
                .flatMap(entries -> entries.entrySet().stream()
                        .filter(entry -> HTTP_METHODS.contains(entry.getKey()))
                        .map(entry -> (Map<String, Object>) entry.getValue()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> published() throws Exception {
        return JsonMapper.builder().build().readValue(get("/actuator/openapi"), LinkedHashMap.class);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> operationsWithResponses(Map<String, Object> document) {
        Map<String, List<String>> operations = new TreeMap<>();
        ((Map<String, Map<String, Object>>) document.get("paths")).forEach((path, methods) -> methods.forEach(
                (method, operation) -> {
                    if (HTTP_METHODS.contains(method)) {
                        Map<Object, Object> responses =
                                (Map<Object, Object>) ((Map<String, Object>) operation).get("responses");
                        operations.put(
                                method.toUpperCase(Locale.ROOT) + " " + path,
                                responses.keySet().stream().map(String::valueOf).sorted().toList());
                    }
                }));
        return operations;
    }

    /** Os exemplos nomeados de cada corpo: "MÉTODO caminho requisição" ou "MÉTODO caminho status". */
    @SuppressWarnings("unchecked")
    private static Map<String, Set<String>> namedExamplesOf(Map<String, Object> document) {
        Map<String, Set<String>> named = new TreeMap<>();
        ((Map<String, Map<String, Object>>) document.get("paths")).forEach((path, methods) -> methods.forEach(
                (method, value) -> {
                    if (!HTTP_METHODS.contains(method)) {
                        return;
                    }
                    Map<String, Object> operation = (Map<String, Object>) value;
                    String label = method.toUpperCase(Locale.ROOT) + " " + path;
                    Map<String, Object> body = (Map<String, Object>) operation.get("requestBody");
                    if (body != null) {
                        collectNamed(named, label + " requisição", (Map<String, Object>) body.get("content"));
                    }
                    ((Map<Object, Object>) operation.get("responses")).forEach((status, response) -> collectNamed(
                            named,
                            label + " " + status,
                            (Map<String, Object>) resolved(document, (Map<String, Object>) response).get("content")));
                }));
        return named;
    }

    /**
     * A resposta, seguindo o {@code $ref} para {@code components/responses}. O contrato declara assim
     * os 401 e 403 repetidos; sem resolver, os exemplos deles não eram comparados.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> resolved(Map<String, Object> document, Map<String, Object> response) {
        Object reference = response.get("$ref");
        if (reference == null) {
            return response;
        }
        String name = reference.toString().substring("#/components/responses/".length());
        return (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) document.get("components"))
                        .get("responses"))
                .get(name);
    }

    @SuppressWarnings("unchecked")
    private static void collectNamed(Map<String, Set<String>> named, String label, Map<String, Object> content) {
        if (content == null) {
            return;
        }
        content.values().forEach(media -> {
            Map<String, Object> examples = (Map<String, Object>) ((Map<String, Object>) media).get("examples");
            if (examples != null) {
                named.computeIfAbsent(label, key -> new TreeSet<>()).addAll(examples.keySet());
            }
        });
    }

    @Test
    @DisplayName("publishes the same operations and response codes as the contract")
    void givenContract_whenComparingThePublishedOperations_thenMatchPathsMethodsAndResponseCodes() throws Exception {
        // given
        // T112: as operações dos contratos, com os mesmos caminhos, métodos e códigos de resposta. Uma
        // resposta a mais ou a menos no código, sem o contrato saber, é o desvio que isto pega: as 9
        // da identidade e as 12 do farm.
        Map<String, List<String>> contract = new TreeMap<>();
        for (Map<String, Object> expected : expectedContracts()) {
            contract.putAll(operationsWithResponses(expected));
        }

        // when
        Map<String, List<String>> published = operationsWithResponses(published());

        // then
        assertThat(contract).hasSize(21);
        assertThat(published).isEqualTo(contract);
    }

    @Test
    @DisplayName("publishes every named example of the contract")
    void givenContract_whenComparingNamedExamples_thenPublishEveryOneTheContractNames() throws Exception {
        // given
        // A T112 comparava só os códigos: a entrada por celular e o administrador semeado, pedidos na
        // T113, faltavam no documento sem nenhum teste reprovar. O documento pode ter exemplos a mais.
        Map<String, Set<String>> contract = new TreeMap<>();
        for (Map<String, Object> expected : expectedContracts()) {
            contract.putAll(namedExamplesOf(expected));
        }

        // when
        Map<String, Set<String>> published = namedExamplesOf(published());

        // then
        assertThat(contract).isNotEmpty();
        assertThat(contract).allSatisfy((body, names) -> assertThat(published.getOrDefault(body, Set.of()))
                .as(body)
                .containsAll(names));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IDENTITY_CONTRACT, FARM_CONTRACT})
    @DisplayName("keeps the copy of the contract equal to the specification it copies")
    void givenSpecificationAtHand_whenComparingTheCopy_thenFindTheSameContract(String file) throws Exception {
        // given
        // A cópia em src/test/resources é a referência da T112; separada da spec, a comparação passaria
        // a proteger um contrato velho. Onde a spec não existe (fora da máquina de quem a mantém), pula.
        Path specContract = SPEC_CONTRACTS.get(file);
        assumeTrue(Files.exists(specContract), "a spec não está ao lado do repositório");

        // when
        String specification = Files.readString(specContract, StandardCharsets.UTF_8).replace("\r\n", "\n");
        String copy;
        try (InputStream stream = OpenApiDocumentIT.class.getResourceAsStream("/contract/" + file)) {
            copy = new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }

        // then
        assertThat(copy).isEqualTo(specification);
    }

    /** O texto sem as quebras de linha e os espaços repetidos, que o YAML e o Java quebram cada um do seu jeito. */
    private static String normalized(String text) {
        return text.replaceAll("\\s+", " ").strip();
    }

    /** A introdução do contrato do farm sem o parágrafo que remete à API de Identidade. */
    private static String farmIntroductionForTheSingleDocument(String farmIntroduction) {
        return Arrays.stream(farmIntroduction.split("\n\\s*\n"))
                .filter(paragraph -> !paragraph.strip().startsWith(FARM_PARAGRAPH_ABOUT_THE_IDENTITY_API))
                .collect(Collectors.joining("\n\n"));
    }

    @Test
    @DisplayName("publishes the title, the introduction, the tags and the server the contracts describe")
    @SuppressWarnings("unchecked")
    void givenContracts_whenReadingThePublishedTexts_thenFindTheSameIntroductionTagsAndServer() throws Exception {
        // given
        // FR-029: textos em português, e os mesmos dos contratos. O servidor era o "Generated server url"
        // do springdoc, e quatro parágrafos da introdução tinham ficado mais curtos que os do contrato.
        // R-012 da 002: um documento só, com a introdução da identidade seguida da do farm, e as tags
        // dos dois contratos, na ordem deles.
        Map<String, Object> identity = contract(IDENTITY_CONTRACT);
        Map<String, Object> farm = contract(FARM_CONTRACT);
        Map<String, Object> published = published();
        List<Object> tags = new ArrayList<>((List<Object>) identity.get("tags"));
        tags.addAll((List<Object>) farm.get("tags"));

        // when
        String contractIntroduction = normalized(((Map<String, String>) identity.get("info")).get("description")
                + "\n\n"
                + farmIntroductionForTheSingleDocument(((Map<String, String>) farm.get("info")).get("description")));
        Map<String, String> info = (Map<String, String>) published.get("info");

        // then
        assertThat(info.get("title")).isEqualTo(PUBLISHED_TITLE);
        assertThat(normalized(info.get("description"))).isEqualTo(contractIntroduction);
        assertThat(published.get("tags")).isEqualTo(tags);
        assertThat(published.get("servers")).isEqualTo(identity.get("servers")).isEqualTo(farm.get("servers"));
    }

    @Test
    @DisplayName("keeps the configured server, whatever host the first reader used")
    void givenReaderUsingAnotherHost_whenReadingTheServerAfterwards_thenKeepTheConfiguredOne() throws Exception {
        // given
        // O servidor calculado pelo springdoc vinha do Host da requisição; traduzida a descrição, ele
        // ficava preso ao do primeiro leitor, e um Host forjado mandaria as senhas do "Try it out" para
        // outro servidor. Agora ele vem da configuração.
        get("127.0.0.1", "/actuator/openapi");

        // when
        JsonNode servers = document().path("servers");

        // then
        assertThat(servers).hasSize(1);
        assertThat(servers.get(0).path("url").asString("")).isEqualTo("http://localhost:8080");
    }

    @TestFactory
    @DisplayName("every request body carries a real example in every field")
    Stream<DynamicTest> givenPublishedDocument_whenReadingEachRequestBody_thenFindAnExampleInEveryField()
            throws Exception {
        // given
        // T110: a interface monta o corpo de "Try it out" a partir dos exemplos de cada campo.
        JsonNode document = document();

        // when
        List<Operation> withBody = operations(document).stream()
                .filter(operation -> operation.node().has("requestBody"))
                .toList();

        // then
        long expected = 0;
        for (Map<String, Object> contract : expectedContracts()) {
            expected += operationsOf(contract).stream()
                    .filter(operation -> operation.containsKey("requestBody"))
                    .count();
        }
        assertThat(withBody).as("operações com corpo de requisição").hasSize((int) expected);
        return withBody.stream().map(operation -> DynamicTest.dynamicTest(operation.label(), () -> {
            JsonNode schema = operation.node().at("/requestBody/content/application~1json/schema");
            assertThat(fieldsWithoutExample(document, schema, "body")).isEmpty();
        }));
    }

    @TestFactory
    @DisplayName("every successful body carries a real example in every field")
    Stream<DynamicTest> givenPublishedDocument_whenReadingEachSuccessfulBody_thenFindAnExampleInEveryField()
            throws Exception {
        // given
        // T110 e T180: os 200 da entrada e do /auth/me saíam como {} na interface.
        JsonNode document = document();

        // when
        List<Map.Entry<String, JsonNode>> successes = new ArrayList<>();
        operations(document).forEach(operation -> operation.node().path("responses").properties().stream()
                .filter(response -> response.getKey().startsWith("2"))
                .filter(response -> response.getValue().has("content"))
                .forEach(response -> successes.add(Map.entry(
                        operation.label() + " " + response.getKey(),
                        response.getValue().at("/content/application~1json/schema")))));

        // then
        assertThat(successes).as("respostas de sucesso com corpo").hasSize(successesWithBodyInTheContracts());
        return successes.stream().map(success -> DynamicTest.dynamicTest(
                success.getKey(),
                () -> assertThat(fieldsWithoutExample(document, success.getValue(), "body")).isEmpty()));
    }

    /** As respostas 2xx com corpo, nas operações que o documento publicado precisa conter hoje. */
    @SuppressWarnings("unchecked")
    private static int successesWithBodyInTheContracts() throws Exception {
        int count = 0;
        for (Map<String, Object> contract : expectedContracts()) {
            for (Map<String, Object> operation : operationsOf(contract)) {
                for (Map.Entry<Object, Object> response : ((Map<Object, Object>) operation.get("responses")).entrySet()) {
                    Map<String, Object> resolved = resolved(contract, (Map<String, Object>) response.getValue());
                    if (String.valueOf(response.getKey()).startsWith("2") && resolved.containsKey("content")) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    @Test
    @DisplayName("every parameter carries a real example")
    void givenPublishedDocument_whenReadingEachParameter_thenFindAnExample() throws Exception {
        // given
        JsonNode document = document();

        // when
        List<String> withoutExample = new ArrayList<>();
        operations(document).forEach(operation -> operation.node().path("parameters").forEach(parameter -> {
            if (!parameter.has("example") && !parameter.path("schema").has("example")) {
                withoutExample.add(operation.label() + " " + parameter.path("name").asString(""));
            }
        }));

        // then
        assertThat(withoutExample).isEmpty();
    }

    @Test
    @DisplayName("no example carries a value the interface would invent")
    void givenPublishedDocument_whenReadingEveryExample_thenFindNoPlaceholder() throws Exception {
        // given
        // T111: "string" e additionalProp são o que o Swagger UI escreve quando não há exemplo.
        JsonNode document = document();

        // when
        List<String> values = exampleValues(document);

        // then
        assertThat(values).as("valores de exemplo lidos").hasSizeGreaterThan(100);
        assertThat(values)
                .as("valor genérico num exemplo")
                .noneMatch(value -> PLACEHOLDERS.stream().anyMatch(value::startsWith));
    }

    @Test
    @DisplayName("uses zero or a boolean as example only where the value is real")
    void givenPublishedDocument_whenReadingZeroAndBooleanExamples_thenFindThemOnlyInTheAllowedFields()
            throws Exception {
        // given
        // T111 proíbe o 0 e o true genéricos. Numa página e na obrigação de trocar a senha, eles são o
        // dado de verdade; em qualquer outro campo, o mais provável é um exemplo esquecido.
        JsonNode document = document();

        // when
        List<String> fields = fieldsWithZeroOrBoolean(document);

        // then
        assertThat(fields).isNotEmpty().allMatch(FIELDS_WITH_ZERO_OR_BOOLEAN_EXAMPLE::contains);
    }

    @SuppressWarnings("unchecked")
    private static List<String> tagsOf(Map<String, Object> operation) {
        return (List<String>) operation.getOrDefault("tags", List.of());
    }

    @Test
    @DisplayName("describes every tag an operation uses")
    void givenPublishedDocument_whenReadingTheTagsInUse_thenFindEachDescribed() throws Exception {
        // given
        // A tag "Responsáveis" chegou com a US2 sem descrição na lista do documento.
        JsonNode document = document();
        Map<String, String> declared = new TreeMap<>();
        document.path("tags").forEach(tag -> declared.put(
                tag.path("name").asString(""), tag.path("description").asString("")));

        // when
        List<String> used = operations(document).stream()
                .flatMap(operation -> operation.node().path("tags").valueStream().map(tag -> tag.asString("")))
                .distinct()
                .toList();

        // then
        Set<String> contractTags = new TreeSet<>();
        for (Map<String, Object> contract : expectedContracts()) {
            operationsOf(contract).forEach(operation -> contractTags.addAll(tagsOf(operation)));
        }
        assertThat(used).containsExactlyInAnyOrderElementsOf(contractTags);
        assertThat(used).allSatisfy(tag -> assertThat(declared.get(tag)).as(tag).isNotBlank());
    }

    @Test
    @DisplayName("lets the interface send the session cookie to the API")
    void givenSwaggerUi_whenReadingItsConfiguration_thenSendCredentials() throws Exception {
        // given
        // FR-028, V-16: a interface roda na porta de gerenciamento e chama a API em outra origem. Sem
        // credenciais, o cookie da sessão não ia junto.

        // when
        JsonNode configuration = JsonMapper.builder().build().readTree(get("/actuator/swagger-ui/swagger-config"));

        // then
        assertThat(configuration.path("withCredentials").asBoolean(false)).isTrue();
    }

    @Test
    @DisplayName("sends the CSRF header to the API on another port of the same host, and never to another host")
    void givenSwaggerUi_whenReadingItsRequestInterceptor_thenCompareTheHostWithoutThePort() throws Exception {
        // given
        // V-16: o interceptador do springdoc só punha o X-XSRF-TOKEN quando a chamada ia para a mesma
        // origem da página, com a porta. A interface fica na 9090 e a API na 8080: toda escrita pelo
        // "Try it out" voltava 403. O cookie XSRF-TOKEN vale para o host inteiro, sem distinção de
        // porta; mandar o token a outro host continua proibido.

        // when
        String initializer = get("/actuator/swagger-ui/swagger-initializer.js");

        // then
        assertThat(initializer)
                .contains("request.headers['X-XSRF-TOKEN']")
                .contains("currentURL.protocol === requestURL.protocol && currentURL.hostname === requestURL.hostname")
                .doesNotContain("currentURL.host === requestURL.host");
    }
}
