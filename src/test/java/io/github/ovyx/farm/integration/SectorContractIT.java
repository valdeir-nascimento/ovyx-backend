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
 * Contrato das operações de setor, contra a aplicação em execução e PostgreSQL real (US1; S-01, S-02;
 * {@code contracts/farm-api.yaml}).
 *
 * <p>Cada teste age com um administrador próprio, recém-cadastrado, e com setores de nome próprio,
 * para não depender da ordem nem dos outros testes do mesmo banco.
 */
@AutoConfigureMockMvc
@DisplayName("Sector contract")
class SectorContractIT extends IntegrationTestSupport {

    private static final String SECTORS = "/api/v1/sectors";
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String UNKNOWN_SECTOR = "/api/v1/sectors/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaretakerRepository caretakerRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private Clock clock;

    private FarmSessions sessions;
    private Cookie[] administrator;

    @BeforeEach
    void signInAsAnAdministrator() throws Exception {
        sessions = new FarmSessions(mockMvc, caretakerRepository, passwordHasher, clock);
        administrator = sessions.administrator();
    }

    private static String uniqueName() {
        return "Codornas — Galpão " + UUID.randomUUID().toString().substring(0, 8);
    }

    private ResultActions register(String body) throws Exception {
        return mockMvc.perform(post(SECTORS)
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

    /** Cadastra um setor e devolve o caminho dele. */
    private String registered(String name) throws Exception {
        String response = register("""
                        {"name": "%s", "description": "Codornas japonesas em postura, baterias A e B"}
                        """.formatted(name))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return SECTORS + "/" + JsonPath.read(response, "$.id");
    }

    // ---------------------------------------------------------------------------------- cadastro

    @Test
    @DisplayName("registers a sector: 201, Location and the sector, active, with zero cages and zero birds")
    void givenValidSector_whenRegistering_thenAnswerCreatedWithTheSector() throws Exception {
        // given
        String name = uniqueName();

        // when
        ResultActions response = register("""
                {"name": "%s", "description": "Codornas japonesas em postura, baterias A e B"}
                """.formatted(name));

        // then
        String id = JsonPath.read(response.andReturn().getResponse().getContentAsString(), "$.id");
        response.andExpect(status().isCreated())
                .andExpect(header().string("Location", SECTORS + "/" + id))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.description").value("Codornas japonesas em postura, baterias A e B"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.activeCageCount").value(0))
                .andExpect(jsonPath("$.birdCount").value(0))
                .andExpect(jsonPath("$.batteries").isEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("registers a sector without description, and leaves the description out of the answer")
    void givenSectorWithoutDescription_whenRegistering_thenAnswerWithoutTheDescription() throws Exception {
        // given
        String name = uniqueName();

        // when
        ResultActions response = register("""
                {"name": "%s"}
                """.formatted(name));

        // then
        response.andExpect(status().isCreated()).andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    @DisplayName("refuses a short name and a long description at once, each with its message")
    void givenShortNameAndLongDescription_whenRegistering_thenAnswerBadRequestWithBothFields() throws Exception {
        // given
        String body = """
                {"name": "A", "description": "%s"}
                """.formatted("d".repeat(501));

        // when
        ResultActions response = register(body);

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Dados inválidos."))
                .andExpect(jsonPath("$.instance").value(SECTORS))
                .andExpect(jsonPath("$.details.name").value("O nome do setor deve ter ao menos 2 caracteres."))
                .andExpect(jsonPath("$.details.description")
                        .value("A descrição deve ter no máximo 500 caracteres."));
    }

    @Test
    @DisplayName("refuses the name of another active sector, differing only in case and spaces")
    void givenActiveSectorNamed_whenRegisteringTheSameNameInOtherCase_thenAnswerConflict() throws Exception {
        // given
        String name = uniqueName();
        registered(name);

        // when
        ResultActions response = register("""
                {"name": "  %s  "}
                """.formatted(name.toUpperCase()));

        // then
        response.andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("SECTOR_NAME_IN_USE"))
                .andExpect(jsonPath("$.title").value("Operação recusada"))
                .andExpect(jsonPath("$.detail").value("Já existe um setor ativo com este nome."))
                .andExpect(jsonPath("$.details.name").value("Já existe um setor ativo com este nome."));
    }

    @Test
    @DisplayName("refuses a body that is not JSON with 415")
    void givenBodyThatIsNotJson_whenRegistering_thenAnswerUnsupportedMediaType() throws Exception {
        // given
        String name = uniqueName();

        // when
        ResultActions response = mockMvc.perform(post(SECTORS)
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
        ResultActions response = mockMvc.perform(post(SECTORS)
                .with(sessions.csrf())
                .cookie(administrator)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_XML)
                .content("""
                        {"name": "%s"}
                        """.formatted(name)));

        // then
        response.andExpect(status().isNotAcceptable());
        mockMvc.perform(get(SECTORS).cookie(administrator).queryParam("status", "ALL"))
                .andExpect(jsonPath("$[*].name", not(hasItem(name))));
    }

    // ---------------------------------------------------------------------------------- consulta

    @Test
    @DisplayName("lists the active sectors with their totals")
    void givenRegisteredSector_whenListing_thenFindItWithItsTotals() throws Exception {
        // given
        String name = uniqueName();
        registered(name);

        // when
        ResultActions response = mockMvc.perform(get(SECTORS).cookie(administrator));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '%s')].status".formatted(name)).value("ACTIVE"))
                .andExpect(jsonPath("$[?(@.name == '%s')].activeCageCount".formatted(name)).value(0))
                .andExpect(jsonPath("$[?(@.name == '%s')].birdCount".formatted(name)).value(0));
    }

    @Test
    @DisplayName("leaves an active sector out of the inactive list, and keeps it in the list of all")
    void givenActiveSector_whenListingByStatus_thenFindItOnlyWhereItBelongs() throws Exception {
        // given
        String name = uniqueName();
        registered(name);

        // when
        ResultActions inactive = mockMvc.perform(get(SECTORS).cookie(administrator).queryParam("status", "INACTIVE"));
        ResultActions all = mockMvc.perform(get(SECTORS).cookie(administrator).queryParam("status", "ALL"));

        // then
        inactive.andExpect(status().isOk()).andExpect(jsonPath("$[*].name", not(hasItem(name))));
        all.andExpect(status().isOk()).andExpect(jsonPath("$[*].name", hasItem(name)));
    }

    @Test
    @DisplayName("refuses an unknown status with the name of the parameter")
    void givenUnknownStatus_whenListing_thenAnswerBadRequestNamingTheParameter() throws Exception {
        // given
        String unknown = "DELETED";

        // when
        ResultActions response = mockMvc.perform(get(SECTORS).cookie(administrator).queryParam("status", unknown));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.parameter").value("status"));
    }

    @Test
    @DisplayName("finds a sector by its identifier")
    void givenRegisteredSector_whenFinding_thenAnswerTheSector() throws Exception {
        // given
        String name = uniqueName();
        String path = registered(name);

        // when
        ResultActions response = mockMvc.perform(get(path).cookie(administrator));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.activeCageCount").value(0));
    }

    @Test
    @DisplayName("answers not found for an identifier of no sector and for a malformed one alike")
    void givenUnknownAndMalformedIdentifiers_whenFinding_thenAnswerNotFoundForBoth() throws Exception {
        // given
        String malformed = SECTORS + "/galpao-9";

        // when
        ResultActions unknownResponse = mockMvc.perform(get(UNKNOWN_SECTOR).cookie(administrator));
        ResultActions malformedResponse = mockMvc.perform(get(malformed).cookie(administrator));

        // then
        unknownResponse.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("SECTOR_NOT_FOUND"))
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.detail").value("Setor não encontrado."));
        malformedResponse.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SECTOR_NOT_FOUND"));
    }

    // ---------------------------------------------------------------------------------- edição

    @Test
    @DisplayName("updates the name and the description")
    void givenRegisteredSector_whenUpdating_thenAnswerTheNewData() throws Exception {
        // given
        String path = registered(uniqueName());
        String newName = uniqueName() + " (norte)";

        // when
        ResultActions response = update(path, """
                {"name": "%s", "description": "Baterias A a D, ala norte"}
                """.formatted(newName));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.description").value("Baterias A a D, ala norte"));
        mockMvc.perform(get(path).cookie(administrator)).andExpect(jsonPath("$.name").value(newName));
    }

    @Test
    @DisplayName("refuses a blank name on update, with the message the contract publishes")
    void givenBlankName_whenUpdating_thenAnswerBadRequest() throws Exception {
        // given
        String path = registered(uniqueName());

        // when
        ResultActions response = update(path, """
                {"name": "   "}
                """);

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.name").value("Informe o nome do setor."));
    }

    @Test
    @DisplayName("answers not found when updating a sector that does not exist")
    void givenUnknownSector_whenUpdating_thenAnswerNotFound() throws Exception {
        // given
        String body = """
                {"name": "%s"}
                """.formatted(uniqueName());

        // when
        ResultActions response = update(UNKNOWN_SECTOR, body);

        // then
        response.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SECTOR_NOT_FOUND"));
    }

    @Test
    @DisplayName("refuses on update the name of another active sector")
    void givenNameOfAnotherActiveSector_whenUpdating_thenAnswerConflict() throws Exception {
        // given
        String taken = uniqueName();
        registered(taken);
        String path = registered(uniqueName());

        // when
        ResultActions response = update(path, """
                {"name": "%s"}
                """.formatted(taken.toLowerCase()));

        // then
        response.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SECTOR_NAME_IN_USE"));
    }

    // ---------------------------------------------------------------------- inativação e reativação

    private ResultActions postTo(String path) throws Exception {
        return mockMvc.perform(post(path).with(sessions.csrf()).cookie(administrator).accept(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("deactivates a sector: 200 with the sector inactive and without active cages")
    void givenActiveSector_whenDeactivating_thenAnswerTheSectorInactive() throws Exception {
        // given
        String path = registered(uniqueName());

        // when
        ResultActions response = postTo(path + "/deactivation");

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"))
                .andExpect(jsonPath("$.activeCageCount").value(0));
    }

    @Test
    @DisplayName("reactivates a sector: 200 with the sector active")
    void givenInactiveSector_whenReactivating_thenAnswerTheSectorActive() throws Exception {
        // given
        String path = registered(uniqueName());
        postTo(path + "/deactivation").andExpect(status().isOk());

        // when
        ResultActions response = postTo(path + "/reactivation");

        // then
        response.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("refuses to reactivate a sector whose name another active sector took (S-08)")
    void givenNameTakenWhileInactive_whenReactivating_thenAnswerConflict() throws Exception {
        // given
        String name = uniqueName();
        String path = registered(name);
        postTo(path + "/deactivation").andExpect(status().isOk());
        registered(name);

        // when
        ResultActions response = postTo(path + "/reactivation");

        // then
        response.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SECTOR_NAME_IN_USE"));
    }

    @Test
    @DisplayName("answers not found when deactivating or reactivating a sector that does not exist")
    void givenUnknownSector_whenDeactivatingOrReactivating_thenAnswerNotFound() throws Exception {
        // given
        String unknown = UNKNOWN_SECTOR;

        // when
        ResultActions deactivation = postTo(unknown + "/deactivation");
        ResultActions reactivation = postTo(unknown + "/reactivation");

        // then
        deactivation.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SECTOR_NOT_FOUND"));
        reactivation.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SECTOR_NOT_FOUND"));
    }
}
