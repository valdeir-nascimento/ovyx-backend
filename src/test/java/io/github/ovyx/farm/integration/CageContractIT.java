package io.github.ovyx.farm.integration;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Contrato das operações de gaiola, contra a aplicação em execução e PostgreSQL real (US2; S-03 a
 * S-05; {@code contracts/farm-api.yaml}).
 *
 * <p>Cada teste age com um administrador próprio e num setor próprio, recém-cadastrado.
 */
@AutoConfigureMockMvc
@DisplayName("Cage contract")
class CageContractIT extends IntegrationTestSupport {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String UNKNOWN_ID = "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44";

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
    private String sectorPath;
    private String cagesPath;

    @BeforeEach
    void registerASectorAsAnAdministrator() throws Exception {
        sessions = new IntegrationSessions(mockMvc, caretakerRepository, passwordHasher, clock);
        administrator = sessions.administrator().cookies();
        sectorPath = "/api/v1/sectors/" + idOf(send(post("/api/v1/sectors"), """
                {"name": "Gaiolas %s"}
                """.formatted(UUID.randomUUID()))
                .andExpect(status().isCreated()));
        cagesPath = sectorPath + "/cages";
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(sessions.csrf())
                .cookie(administrator)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String idOf(ResultActions response) throws Exception {
        return JsonPath.read(response.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private ResultActions registerCage(String body) throws Exception {
        return send(post(cagesPath), body);
    }

    /** Cadastra uma gaiola e devolve o caminho dela. */
    private String registered(String battery, int number, int birdCount) throws Exception {
        return cagesPath + "/" + idOf(registerCage("""
                {"battery": "%s", "number": %d, "birdCount": %d}
                """.formatted(battery, number, birdCount))
                .andExpect(status().isCreated()));
    }

    // ---------------------------------------------------------------------------------- cadastro

    @Test
    @DisplayName("registers a cage: 201, Location and the cage with its code in capitals")
    void givenBatteryInLowerCase_whenRegistering_thenAnswerCreatedWithTheCodeInCapitals() throws Exception {
        // given
        String body = """
                {"battery": "b", "number": 7, "birdCount": 50}
                """;

        // when
        ResultActions response = registerCage(body);

        // then
        String id = idOf(response);
        response.andExpect(status().isCreated())
                .andExpect(header().string("Location", cagesPath + "/" + id))
                .andExpect(jsonPath("$.code").value("B-07"))
                .andExpect(jsonPath("$.battery").value("B"))
                .andExpect(jsonPath("$.number").value(7))
                .andExpect(jsonPath("$.birdCount").value(50))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.sectorId").value(sectorPath.substring(sectorPath.lastIndexOf('/') + 1)))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("adds the cage to the totals and to the batteries of the sector")
    void givenTwoCages_whenFindingTheSector_thenFindThemInItsTotalsAndBatteries() throws Exception {
        // given
        registered("B", 7, 50);
        registered("A", 1, 48);

        // when
        ResultActions response = mockMvc.perform(get(sectorPath).cookie(administrator));

        // then
        response.andExpect(jsonPath("$.activeCageCount").value(2))
                .andExpect(jsonPath("$.birdCount").value(98))
                .andExpect(jsonPath("$.batteries[0]").value("A"))
                .andExpect(jsonPath("$.batteries[1]").value("B"));
    }

    @Test
    @DisplayName("refuses an empty battery, number zero and negative birds at once, with the contract's messages")
    void givenThreeInvalidFields_whenRegistering_thenAnswerBadRequestWithTheThreeFields() throws Exception {
        // given
        String body = """
                {"battery": "", "number": 0, "birdCount": -1}
                """;

        // when
        ResultActions response = registerCage(body);

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.battery").value("Informe a bateria, com até 3 letras ou dígitos."))
                .andExpect(jsonPath("$.details.number").value("O número da gaiola deve ficar entre 1 e 999."))
                .andExpect(jsonPath("$.details.birdCount").value("A quantidade de aves deve ficar entre 0 e 1.000."));
    }

    @ParameterizedTest(name = "birdCount {0}")
    @ValueSource(strings = {"12.5", "\"cinquenta\""})
    @DisplayName("refuses birds that are not an integer next to the field, with the other fields, and not as an unreadable body")
    void givenBirdsThatAreNotAnInteger_whenRegistering_thenAnswerTheFieldWithTheOtherViolations(String birdCount)
            throws Exception {
        // given
        String body = """
                {"battery": "ABCD", "number": 7, "birdCount": %s}
                """.formatted(birdCount);

        // when
        ResultActions response = registerCage(body);

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.birdCount").value("A quantidade de aves deve ser um número inteiro."))
                .andExpect(jsonPath("$.details.battery").exists());
    }

    @Test
    @DisplayName("refuses the battery and number of another active cage of the sector")
    void givenActiveCageB07_whenRegisteringB07_thenAnswerConflict() throws Exception {
        // given
        registered("B", 7, 50);

        // when
        ResultActions response = registerCage("""
                {"battery": "b", "number": 7, "birdCount": 48}
                """);

        // then
        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAGE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.title").value("Operação recusada"))
                .andExpect(jsonPath("$.detail").value("Já existe uma gaiola ativa B-07 neste setor."))
                .andExpect(jsonPath("$.details.battery").value("Já existe uma gaiola ativa B-07 neste setor."))
                .andExpect(jsonPath("$.details.number").value("Já existe uma gaiola ativa B-07 neste setor."));
    }

    @Test
    @DisplayName("answers sector not found when registering a cage in a sector that does not exist")
    void givenUnknownSector_whenRegisteringACage_thenAnswerNotFound() throws Exception {
        // given
        String unknownCages = "/api/v1/sectors/" + UNKNOWN_ID + "/cages";

        // when
        ResultActions response = send(post(unknownCages), """
                {"battery": "B", "number": 7, "birdCount": 50}
                """);

        // then
        response.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SECTOR_NOT_FOUND"));
    }

    @Test
    @DisplayName("refuses a body that is not JSON with 415")
    void givenBodyThatIsNotJson_whenRegistering_thenAnswerUnsupportedMediaType() throws Exception {
        // given
        String body = "B-07";

        // when
        ResultActions response = mockMvc.perform(post(cagesPath)
                .with(sessions.csrf())
                .cookie(administrator)
                .contentType(MediaType.TEXT_PLAIN)
                .content(body));

        // then
        response.andExpect(status().isUnsupportedMediaType());
    }

    // ---------------------------------------------------------------------------------- consulta

    @Test
    @DisplayName("searches the cages by a piece of the code, in pages")
    void givenCages_whenSearchingByCode_thenAnswerThePageOfTheMatchingOnes() throws Exception {
        // given
        registered("A", 7, 50);
        registered("B", 7, 50);
        registered("B", 8, 50);

        // when
        ResultActions response = mockMvc.perform(get(cagesPath).cookie(administrator).queryParam("code", "07"));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].code").value("A-07"))
                .andExpect(jsonPath("$.content[1].code").value("B-07"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("refuses a page size above 100 with the message of the 001")
    void givenPageSizeAbove100_whenSearching_thenAnswerBadRequest() throws Exception {
        // given
        String size = "101";

        // when
        ResultActions response = mockMvc.perform(get(cagesPath).cookie(administrator).queryParam("size", size));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.size").value("O tamanho da página deve estar entre 1 e 100."));
    }

    @Test
    @DisplayName("answers sector not found when searching the cages of a sector that does not exist")
    void givenUnknownSector_whenSearching_thenAnswerNotFound() throws Exception {
        // given
        String unknownCages = "/api/v1/sectors/" + UNKNOWN_ID + "/cages";

        // when
        ResultActions response = mockMvc.perform(get(unknownCages).cookie(administrator));

        // then
        response.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SECTOR_NOT_FOUND"));
    }

    @Test
    @DisplayName("finds a cage of the sector")
    void givenCage_whenFinding_thenAnswerIt() throws Exception {
        // given
        String path = registered("C", 120, 48);

        // when
        ResultActions response = mockMvc.perform(get(path).cookie(administrator));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("C-120"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("answers cage not found for a cage that does not exist, a malformed one, or one of another sector")
    void givenUnknownMalformedAndForeignCages_whenFinding_thenAnswerCageNotFound() throws Exception {
        // given
        String foreign = registered("B", 7, 50);
        String otherSector = "/api/v1/sectors/" + idOf(send(post("/api/v1/sectors"), """
                {"name": "Outro %s"}
                """.formatted(UUID.randomUUID())));
        String foreignThroughOtherSector = otherSector + foreign.substring(foreign.indexOf("/cages"));

        // when
        ResultActions unknownResponse = mockMvc.perform(get(cagesPath + "/" + UNKNOWN_ID).cookie(administrator));
        ResultActions malformedResponse = mockMvc.perform(get(cagesPath + "/b-07").cookie(administrator));
        ResultActions foreignResponse = mockMvc.perform(get(foreignThroughOtherSector).cookie(administrator));

        // then
        unknownResponse.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CAGE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Gaiola não encontrada."));
        malformedResponse.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CAGE_NOT_FOUND"));
        foreignResponse.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CAGE_NOT_FOUND"));
    }

    // ---------------------------------------------------------------------------------- edição

    @Test
    @DisplayName("corrects the birds of a cage, and the totals of the sector follow")
    void givenCageWith50Birds_whenCorrectingTo48_thenAnswerTheCageAndUpdateTheTotals() throws Exception {
        // given
        String path = registered("B", 7, 50);

        // when
        ResultActions response = send(put(path), """
                {"battery": "B", "number": 7, "birdCount": 48}
                """);

        // then
        response.andExpect(status().isOk()).andExpect(jsonPath("$.birdCount").value(48));
        mockMvc.perform(get(sectorPath).cookie(administrator)).andExpect(jsonPath("$.birdCount").value(48));
    }

    @Test
    @DisplayName("refuses on update the code of another active cage")
    void givenTwoCages_whenUpdatingOneToTheCodeOfTheOther_thenAnswerConflict() throws Exception {
        // given
        registered("A", 12, 40);
        String path = registered("B", 7, 50);

        // when
        ResultActions response = send(put(path), """
                {"battery": "A", "number": 12, "birdCount": 50}
                """);

        // then
        response.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CAGE_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("refuses invalid fields on update, and answers not found for a cage that does not exist")
    void givenInvalidFieldsAndUnknownCage_whenUpdating_thenAnswerBadRequestAndNotFound() throws Exception {
        // given
        String path = registered("B", 7, 50);

        // when
        ResultActions invalid = send(put(path), """
                {"battery": "B", "number": 1000, "birdCount": 50}
                """);
        ResultActions unknown = send(put(cagesPath + "/" + UNKNOWN_ID), """
                {"battery": "B", "number": 7, "birdCount": 50}
                """);

        // then
        invalid.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.number").exists());
        unknown.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CAGE_NOT_FOUND"));
    }

    // ---------------------------------------------------------------------- inativação e reativação

    private ResultActions postTo(String path) throws Exception {
        return mockMvc.perform(post(path).with(sessions.csrf()).cookie(administrator).accept(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("deactivates a cage, which leaves the totals and stays consultable among the inactive (S-06)")
    void givenActiveCage_whenDeactivating_thenAnswerItInactiveAndTakeItOutOfTheTotals() throws Exception {
        // given
        String path = registered("A", 2, 48);

        // when
        ResultActions response = postTo(path + "/deactivation");

        // then
        response.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        mockMvc.perform(get(sectorPath).cookie(administrator)).andExpect(jsonPath("$.birdCount").value(0));
        mockMvc.perform(get(cagesPath).cookie(administrator).queryParam("status", "INACTIVE"))
                .andExpect(jsonPath("$.content[0].code").value("A-02"));
    }

    @Test
    @DisplayName("accepts a new cage with the code of an inactive one, and then refuses to reactivate the old one")
    void givenInactiveA02AndANewA02_whenReactivatingTheOldOne_thenAnswerConflict() throws Exception {
        // given
        String old = registered("A", 2, 48);
        postTo(old + "/deactivation").andExpect(status().isOk());
        registered("A", 2, 50);

        // when
        ResultActions response = postTo(old + "/reactivation");

        // then
        response.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CAGE_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("refuses a new cage and a cage reactivated alone in an inactive sector (S-07)")
    void givenInactiveSector_whenRegisteringOrReactivatingACage_thenAnswerConflictWithTheSectorInactive() throws Exception {
        // given
        String cage = registered("A", 1, 50);
        postTo(sectorPath + "/deactivation").andExpect(status().isOk());

        // when
        ResultActions registration = registerCage("""
                {"battery": "C", "number": 1, "birdCount": 50}
                """);
        ResultActions reactivation = postTo(cage + "/reactivation");

        // then
        registration.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SECTOR_INACTIVE"))
                .andExpect(jsonPath("$.detail").value("O setor está inativo. Reative o setor antes de mexer nas gaiolas dele."));
        reactivation.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SECTOR_INACTIVE"));
    }

    @Test
    @DisplayName("answers cage not found when deactivating or reactivating a cage that does not exist")
    void givenUnknownCage_whenDeactivatingOrReactivating_thenAnswerNotFound() throws Exception {
        // given
        String unknown = cagesPath + "/" + UNKNOWN_ID;

        // when
        ResultActions deactivation = postTo(unknown + "/deactivation");
        ResultActions reactivation = postTo(unknown + "/reactivation");

        // then
        deactivation.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CAGE_NOT_FOUND"));
        reactivation.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CAGE_NOT_FOUND"));
    }

    @Test
    @DisplayName("refuses to update a cage of an inactive sector, with the sector inactive as the reason")
    void givenInactiveSector_whenUpdatingACage_thenAnswerConflictWithTheSectorInactive() throws Exception {
        // given
        String cage = registered("A", 1, 50);
        postTo(sectorPath + "/deactivation").andExpect(status().isOk());

        // when
        ResultActions response = send(put(cage), """
                {"battery": "A", "number": 1, "birdCount": 40}
                """);

        // then
        response.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SECTOR_INACTIVE"));
    }
}
