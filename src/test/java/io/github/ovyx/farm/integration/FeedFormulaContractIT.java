package io.github.ovyx.farm.integration;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.ovyx.IntegrationSessions;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Contrato das operações de fórmula de ração, contra a aplicação em execução e PostgreSQL real (US1 da
 * 004; S-01 a S-03, S-09; {@code contracts/feed-formulas-api.yaml}).
 *
 * <p>Cada teste age com um administrador próprio, recém-cadastrado, e com fórmulas de nome próprio,
 * para não depender da ordem nem dos outros testes do mesmo banco.
 */
@AutoConfigureMockMvc
@DisplayName("Feed formula contract")
class FeedFormulaContractIT extends IntegrationTestSupport {

    private static final String FORMULAS = "/api/v1/feed-formulas";
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String UNKNOWN_FORMULA = "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c99";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaretakerRepository caretakerRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private Clock clock;

    private IntegrationSessions sessions;
    private Cookie[] administrator;

    @BeforeEach
    void signInAsAnAdministrator() throws Exception {
        sessions = new IntegrationSessions(mockMvc, caretakerRepository, passwordHasher, clock);
        administrator = sessions.administrator().cookies();
    }

    private static String uniqueName() {
        return "Postura Plus " + UUID.randomUUID().toString().substring(0, 8);
    }

    private ResultActions register(String body) throws Exception {
        return mockMvc.perform(post(FORMULAS)
                .with(sessions.csrf())
                .cookie(administrator)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions update(String path, String body) throws Exception {
        return mockMvc.perform(put(path)
                .with(sessions.csrf())
                .cookie(administrator)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions postTo(String path) throws Exception {
        return mockMvc.perform(post(path).with(sessions.csrf()).cookie(administrator).accept(MediaType.APPLICATION_JSON));
    }

    /** Cadastra a Postura Plus com o nome dado e devolve o caminho dela. */
    private String registered(String name) throws Exception {
        String response = register("""
                        {"name": "%s", "pricePerKg": 2.85, "expectedIntake": 28,
                         "description": "Milho, farelo de soja e calcário"}
                        """.formatted(name))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return FORMULAS + "/" + JsonPath.read(response, "$.id");
    }

    // ---------------------------------------------------------------------------------- cadastro

    @Test
    @DisplayName("registers a formula: 201, Location and the formula, active, with the cost per bird a day")
    void givenValidFormula_whenRegistering_thenAnswerCreatedWithTheFormula() throws Exception {
        // given
        String name = uniqueName();

        // when
        ResultActions response = register("""
                {"name": "%s", "pricePerKg": 2.85, "expectedIntake": 28,
                 "description": "Milho, farelo de soja e calcário"}
                """.formatted(name));

        // then
        String id = JsonPath.read(response.andReturn().getResponse().getContentAsString(), "$.id");
        response.andExpect(status().isCreated())
                .andExpect(header().string("Location", FORMULAS + "/" + id))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.description").value("Milho, farelo de soja e calcário"))
                .andExpect(jsonPath("$.pricePerKg").value(2.85))
                .andExpect(jsonPath("$.expectedIntake").value(28))
                .andExpect(jsonPath("$.costPerBirdDay").value(0.08))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("accepts the price and the expected intake as the form typed them, and leaves the description out")
    void givenPriceAndIntakeAsText_whenRegistering_thenAcceptThem() throws Exception {
        // given
        String name = uniqueName();

        // when
        ResultActions response = register("""
                {"name": "%s", "pricePerKg": "3,10", "expectedIntake": "24"}
                """.formatted(name));

        // then
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.pricePerKg").value(3.1))
                .andExpect(jsonPath("$.expectedIntake").value(24))
                .andExpect(jsonPath("$.costPerBirdDay").value(0.074))
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    @DisplayName("refuses a blank name, a zero price and an intake above the limit at once, each with its message")
    void givenThreeInvalidFields_whenRegistering_thenAnswerBadRequestWithTheThreeFields() throws Exception {
        // given
        String body = """
                {"name": " ", "pricePerKg": "0", "expectedIntake": "300"}
                """;

        // when
        ResultActions response = register(body);

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Dados inválidos."))
                .andExpect(jsonPath("$.instance").value(FORMULAS))
                .andExpect(jsonPath("$.details.name").value("Informe o nome da fórmula."))
                .andExpect(jsonPath("$.details.pricePerKg")
                        .value("O preço deve ficar entre R$ 0,01 e R$ 1.000,00 o quilo."))
                .andExpect(jsonPath("$.details.expectedIntake")
                        .value("O consumo esperado deve ficar entre 1 e 200 gramas por ave ao dia."));
    }

    @Test
    @DisplayName("refuses a price with three decimals and a broken intake sent as numbers, in their own fields")
    void givenPriceWithThreeDecimalsAndBrokenIntakeAsNumbers_whenRegistering_thenRefuseEachInItsField()
            throws Exception {
        // given
        String body = """
                {"name": "%s", "pricePerKg": 2.855, "expectedIntake": 28.5}
                """.formatted(uniqueName());

        // when
        ResultActions response = register(body);

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.pricePerKg")
                        .value("Informe o preço em reais, com até duas casas decimais."))
                .andExpect(jsonPath("$.details.expectedIntake")
                        .value("O consumo esperado deve ser um número inteiro de gramas."));
    }

    @Test
    @DisplayName("refuses the name of an inactive formula, differing only in case and spaces")
    void givenInactiveFormulaNamed_whenRegisteringTheSameNameInOtherCase_thenAnswerConflict() throws Exception {
        // given
        String name = uniqueName();
        postTo(registered(name) + "/deactivation").andExpect(status().isOk());

        // when
        ResultActions response = register("""
                {"name": "  %s  ", "pricePerKg": "2,90", "expectedIntake": 28}
                """.formatted(name.toUpperCase()));

        // then
        response.andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FEED_FORMULA_NAME_IN_USE"))
                .andExpect(jsonPath("$.title").value("Operação recusada"))
                .andExpect(jsonPath("$.detail").value("Já existe uma fórmula com este nome."))
                .andExpect(jsonPath("$.details.name")
                        .value("Já existe uma fórmula com este nome. Se ela está inativa, reative-a."));
    }

    @Test
    @DisplayName("refuses a body that is not JSON with 415")
    void givenBodyThatIsNotJson_whenRegistering_thenAnswerUnsupportedMediaType() throws Exception {
        // given
        String name = uniqueName();

        // when
        ResultActions response = mockMvc.perform(post(FORMULAS)
                .with(sessions.csrf())
                .cookie(administrator)
                .contentType(MediaType.TEXT_PLAIN)
                .content(name));

        // then
        response.andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("REQUEST_NOT_ACCEPTABLE"));
    }

    @Test
    @DisplayName("refuses an answer format other than JSON with 406, before registering anything")
    void givenAcceptOtherThanJson_whenRegistering_thenAnswerNotAcceptable() throws Exception {
        // given
        String name = uniqueName();

        // when
        ResultActions response = mockMvc.perform(post(FORMULAS)
                .with(sessions.csrf())
                .cookie(administrator)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_XML)
                .content("""
                        {"name": "%s", "pricePerKg": 2.85, "expectedIntake": 28}
                        """.formatted(name)));

        // then
        response.andExpect(status().isNotAcceptable());
        mockMvc.perform(get(FORMULAS).cookie(administrator).queryParam("status", "ALL"))
                .andExpect(jsonPath("$[*].name", not(hasItem(name))));
    }

    // ---------------------------------------------------------------------------------- consulta

    @Test
    @DisplayName("lists the active formulas with the cost per bird a day")
    void givenRegisteredFormula_whenListing_thenFindItWithItsCost() throws Exception {
        // given
        String name = uniqueName();
        registered(name);

        // when
        ResultActions response = mockMvc.perform(get(FORMULAS).cookie(administrator));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '%s')].status".formatted(name)).value("ACTIVE"))
                .andExpect(jsonPath("$[?(@.name == '%s')].costPerBirdDay".formatted(name)).value(0.08));
    }

    @Test
    @DisplayName("moves a deactivated formula from the active list to the inactive one")
    void givenDeactivatedFormula_whenListingByStatus_thenFindItOnlyWhereItBelongs() throws Exception {
        // given
        String name = uniqueName();
        postTo(registered(name) + "/deactivation").andExpect(status().isOk());

        // when
        ResultActions active = mockMvc.perform(get(FORMULAS).cookie(administrator));
        ResultActions inactive = mockMvc.perform(get(FORMULAS).cookie(administrator).queryParam("status", "INACTIVE"));
        ResultActions all = mockMvc.perform(get(FORMULAS).cookie(administrator).queryParam("status", "ALL"));

        // then
        active.andExpect(status().isOk()).andExpect(jsonPath("$[*].name", not(hasItem(name))));
        inactive.andExpect(status().isOk()).andExpect(jsonPath("$[*].name", hasItem(name)));
        all.andExpect(status().isOk()).andExpect(jsonPath("$[*].name", hasItem(name)));
    }

    @Test
    @DisplayName("refuses an unknown status with the name of the parameter")
    void givenUnknownStatus_whenListing_thenAnswerBadRequestNamingTheParameter() throws Exception {
        // given
        String unknown = "DELETED";

        // when
        ResultActions response = mockMvc.perform(get(FORMULAS).cookie(administrator).queryParam("status", unknown));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.parameter").value("status"));
    }

    @Test
    @DisplayName("finds a formula by its identifier")
    void givenRegisteredFormula_whenFinding_thenAnswerTheFormula() throws Exception {
        // given
        String name = uniqueName();
        String path = registered(name);

        // when
        ResultActions response = mockMvc.perform(get(path).cookie(administrator));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.pricePerKg").value(2.85));
    }

    @Test
    @DisplayName("answers not found for an identifier of no formula and for a malformed one alike")
    void givenUnknownAndMalformedIdentifiers_whenFinding_thenAnswerNotFoundForBoth() throws Exception {
        // given
        String malformed = FORMULAS + "/postura-plus";

        // when
        ResultActions unknownResponse = mockMvc.perform(get(UNKNOWN_FORMULA).cookie(administrator));
        ResultActions malformedResponse = mockMvc.perform(get(malformed).cookie(administrator));

        // then
        unknownResponse.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FEED_FORMULA_NOT_FOUND"))
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.detail").value("Fórmula não encontrada."));
        malformedResponse.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("FEED_FORMULA_NOT_FOUND"));
    }

    // ---------------------------------------------------------------------------------- edição

    @Test
    @DisplayName("updates the fields, and the new price shows in the cost per bird a day")
    void givenRegisteredFormula_whenUpdating_thenAnswerTheNewData() throws Exception {
        // given
        String path = registered(uniqueName());
        String newName = uniqueName() + " II";

        // when
        ResultActions response = update(path, """
                {"name": "%s", "pricePerKg": "3,10", "expectedIntake": 28}
                """.formatted(newName));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.pricePerKg").value(3.1))
                .andExpect(jsonPath("$.costPerBirdDay").value(0.087))
                .andExpect(jsonPath("$.description").doesNotExist());
        mockMvc.perform(get(path).cookie(administrator)).andExpect(jsonPath("$.name").value(newName));
    }

    @Test
    @DisplayName("updates an inactive formula, which stays inactive")
    void givenInactiveFormula_whenUpdating_thenKeepItInactive() throws Exception {
        // given
        String name = uniqueName();
        String path = registered(name);
        postTo(path + "/deactivation").andExpect(status().isOk());

        // when
        ResultActions response = update(path, """
                {"name": "%s", "pricePerKg": "2,95", "expectedIntake": 28}
                """.formatted(name));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.pricePerKg").value(2.95));
    }

    @Test
    @DisplayName("refuses a missing price and intake on update, with the messages the contract publishes")
    void givenMissingPriceAndIntake_whenUpdating_thenAnswerBadRequest() throws Exception {
        // given
        String path = registered(uniqueName());

        // when
        ResultActions response = update(path, """
                {"name": "Postura"}
                """);

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.pricePerKg").value("Informe o preço por quilo."))
                .andExpect(jsonPath("$.details.expectedIntake").value("Informe o consumo esperado."));
    }

    @Test
    @DisplayName("answers not found when updating a formula that does not exist")
    void givenUnknownFormula_whenUpdating_thenAnswerNotFound() throws Exception {
        // given
        String body = """
                {"name": "%s", "pricePerKg": 2.85, "expectedIntake": 28}
                """.formatted(uniqueName());

        // when
        ResultActions response = update(UNKNOWN_FORMULA, body);

        // then
        response.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("FEED_FORMULA_NOT_FOUND"));
    }

    @Test
    @DisplayName("refuses on update the name of another formula")
    void givenNameOfAnotherFormula_whenUpdating_thenAnswerConflict() throws Exception {
        // given
        String taken = uniqueName();
        registered(taken);
        String path = registered(uniqueName());

        // when
        ResultActions response = update(path, """
                {"name": "%s", "pricePerKg": 2.85, "expectedIntake": 28}
                """.formatted(taken.toLowerCase()));

        // then
        response.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("FEED_FORMULA_NAME_IN_USE"));
    }

    // ---------------------------------------------------------------------- inativação e reativação

    @Test
    @DisplayName("deactivates a formula: 200 with the formula inactive and its data")
    void givenActiveFormula_whenDeactivating_thenAnswerTheFormulaInactive() throws Exception {
        // given
        String path = registered(uniqueName());

        // when
        ResultActions response = postTo(path + "/deactivation");

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.pricePerKg").value(2.85));
    }

    @Test
    @DisplayName("reactivates a formula: 200 with the formula active")
    void givenInactiveFormula_whenReactivating_thenAnswerTheFormulaActive() throws Exception {
        // given
        String path = registered(uniqueName());
        postTo(path + "/deactivation").andExpect(status().isOk());

        // when
        ResultActions response = postTo(path + "/reactivation");

        // then
        response.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("answers not found when deactivating or reactivating a formula that does not exist")
    void givenUnknownFormula_whenDeactivatingOrReactivating_thenAnswerNotFound() throws Exception {
        // given
        String unknown = UNKNOWN_FORMULA;

        // when
        ResultActions deactivation = postTo(unknown + "/deactivation");
        ResultActions reactivation = postTo(unknown + "/reactivation");

        // then
        deactivation.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("FEED_FORMULA_NOT_FOUND"));
        reactivation.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("FEED_FORMULA_NOT_FOUND"));
    }
}
