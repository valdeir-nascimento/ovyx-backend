package io.github.ovyx.farm.integration;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aUniqueSector;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.ovyx.IntegrationSessions.SignedIn;
import io.github.ovyx.IntegrationSessions;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
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

/**
 * As operações de pesagem nos códigos e nos corpos do contrato {@code farm-api.yaml} da 005 (FR-003,
 * FR-004, FR-009 a FR-013, FR-017; R-010).
 */
@AutoConfigureMockMvc
@DisplayName("Weighing contract")
class WeighingContractIT extends IntegrationTestSupport {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaretakerRepository caretakerRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private SectorRepository sectors;

    @Autowired
    private Clock clock;

    private IntegrationSessions sessions;
    private SignedIn commonUser;
    private Sector sector;
    private String cagePath;

    @BeforeEach
    void registerASectorAndSignInAsACommonUser() throws Exception {
        sessions = new IntegrationSessions(mockMvc, caretakerRepository, passwordHasher, clock);
        commonUser = sessions.commonUser();
        sector = aUniqueSector().withCage("A", 1, 48).withCage("B", 7, 50).build();
        sectors.save(sector);
        cagePath = "/api/v1/sectors/" + sector.id() + "/cages/" + sector.cages().get(0).id();
    }

    private ResultActions record(String path, String body) throws Exception {
        return mockMvc.perform(post(path + "/weighings")
                .with(sessions.csrf())
                .cookie(commonUser.cookies())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions overviewOf(String path) throws Exception {
        return mockMvc.perform(get(path + "/weighings").cookie(commonUser.cookies()).accept(MediaType.APPLICATION_JSON));
    }

    // ---------------------------------------------------------------------------------- registro

    @Test
    @DisplayName("records a weighing: 201, Location and the weighing, with who recorded it")
    void givenValidWeighing_whenRecording_thenAnswerCreatedWithTheWeighing() throws Exception {
        // given
        String body = """
                {"weighedOn": "2026-09-24", "averageWeight": "161,4"}
                """;

        // when
        ResultActions response = record(cagePath, body);

        // then
        String id = JsonPath.read(response.andReturn().getResponse().getContentAsString(), "$.id");
        response.andExpect(status().isCreated())
                .andExpect(header().string("Location", cagePath + "/weighings/" + id))
                .andExpect(jsonPath("$.weighedOn").value("2026-09-24"))
                .andExpect(jsonPath("$.averageWeight").value(161.4))
                .andExpect(jsonPath("$.recordedBy.id").value(commonUser.id().toString()))
                .andExpect(jsonPath("$.recordedBy.name").value(commonUser.fullName()))
                .andExpect(jsonPath("$.recordedAt").isNotEmpty())
                .andExpect(jsonPath("$.lastCorrectedBy").doesNotExist());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"161.4", "\"161.4\"", "\"161,4\""})
    @DisplayName("accepts the weight as a number or as the form typed it, and returns it as a number")
    void givenWeightAsNumberOrText_whenRecording_thenAcceptIt(String weight) throws Exception {
        // given
        String body = """
                {"weighedOn": "2026-09-17", "averageWeight": %s}
                """.formatted(weight);

        // when
        ResultActions response = record(cagePath, body);

        // then
        response.andExpect(status().isCreated()).andExpect(jsonPath("$.averageWeight").value(161.4));
    }

    @Test
    @DisplayName("refuses a future day and a missing weight with 400, both at once")
    void givenFutureDayAndMissingWeight_whenRecording_thenAnswerBadRequestWithBothFields() throws Exception {
        // given
        String body = """
                {"weighedOn": "2999-01-01"}
                """;

        // when
        ResultActions response = record(cagePath, body);

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.weighedOn").value("A data da pesagem não pode ser futura."))
                .andExpect(jsonPath("$.details.averageWeight").value("Informe o peso médio em gramas."));
    }

    @Test
    @DisplayName("refuses a second weighing on the same day with 409, in the day field")
    void givenWeighingOnTheDay_whenRecordingTheSameDay_thenAnswerConflict() throws Exception {
        // given
        record(cagePath, """
                {"weighedOn": "2026-09-24", "averageWeight": 161}
                """).andExpect(status().isCreated());

        // when
        ResultActions response = record(cagePath, """
                {"weighedOn": "2026-09-24", "averageWeight": 158}
                """);

        // then
        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WEIGHING_DATE_IN_USE"))
                .andExpect(jsonPath("$.details.weighedOn")
                        .value("A gaiola já tem pesagem em 24/09/2026. Corrija a pesagem desse dia."));
    }

    @Test
    @DisplayName("refuses a weighing of an inactive cage with 409")
    void givenInactiveCage_whenRecording_thenAnswerConflict() throws Exception {
        // given
        Sector withInactiveCage = aUniqueSector().withInactiveCage("A", 1, 48).build();
        sectors.save(withInactiveCage);
        String path = "/api/v1/sectors/" + withInactiveCage.id() + "/cages/" + withInactiveCage.cages().get(0).id();

        // when
        ResultActions response = record(path, """
                {"weighedOn": "2026-09-24", "averageWeight": 161}
                """);

        // then
        response.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CAGE_INACTIVE"));
    }

    @Test
    @DisplayName("refuses an unknown sector with 404")
    void givenUnknownSector_whenRecording_thenAnswerNotFound() throws Exception {
        // given
        String path = "/api/v1/sectors/" + UUID.randomUUID() + "/cages/" + sector.cages().get(0).id();

        // when
        ResultActions response = record(path, """
                {"weighedOn": "2026-09-24", "averageWeight": 161}
                """);

        // then
        response.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SECTOR_NOT_FOUND"));
    }

    @Test
    @DisplayName("refuses a cage of another sector with 404")
    void givenCageOfAnotherSector_whenRecording_thenAnswerNotFound() throws Exception {
        // given
        Sector other = aUniqueSector().withCage("C", 1, 48).build();
        sectors.save(other);
        String path = "/api/v1/sectors/" + other.id() + "/cages/" + sector.cages().get(0).id();

        // when
        ResultActions response = record(path, """
                {"weighedOn": "2026-09-24", "averageWeight": 161}
                """);

        // then
        response.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CAGE_NOT_FOUND"));
    }

    @Test
    @DisplayName("refuses a body that is not JSON with 415")
    void givenBodyThatIsNotJson_whenRecording_thenAnswerUnsupportedMediaType() throws Exception {
        // given
        String body = "161,4 em 24/09";

        // when
        ResultActions response = mockMvc.perform(post(cagePath + "/weighings")
                .with(sessions.csrf())
                .cookie(commonUser.cookies())
                .contentType(MediaType.TEXT_PLAIN)
                .content(body));

        // then
        response.andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("REQUEST_NOT_ACCEPTABLE"));
    }

    @Test
    @DisplayName("refuses an answer format other than JSON with 406, before recording anything")
    void givenAcceptOtherThanJson_whenRecording_thenAnswerNotAcceptable() throws Exception {
        // given
        String body = """
                {"weighedOn": "2026-09-10", "averageWeight": 156}
                """;

        // when
        ResultActions response = mockMvc.perform(post(cagePath + "/weighings")
                .with(sessions.csrf())
                .cookie(commonUser.cookies())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_XML)
                .content(body));

        // then
        response.andExpect(status().isNotAcceptable());
        overviewOf(cagePath).andExpect(jsonPath("$.history").isEmpty());
    }

    // ---------------------------------------------------------------------------------- acompanhamento

    @Test
    @DisplayName("answers the overview of the cage: the cage, the sector, the latest and the history with the changes")
    void givenTwoWeighings_whenAskingTheOverview_thenAnswerTheCageTheLatestAndTheHistory() throws Exception {
        // given
        record(cagePath, """
                {"weighedOn": "2026-09-17", "averageWeight": 158}
                """).andExpect(status().isCreated());
        record(cagePath, """
                {"weighedOn": "2026-09-24", "averageWeight": "161,4"}
                """).andExpect(status().isCreated());

        // when
        ResultActions response = overviewOf(cagePath);

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.cage.code").value("A-01"))
                .andExpect(jsonPath("$.cage.battery").value("A"))
                .andExpect(jsonPath("$.cage.birdCount").value(48))
                .andExpect(jsonPath("$.cage.status").value("ACTIVE"))
                .andExpect(jsonPath("$.sector.id").value(sector.id().toString()))
                .andExpect(jsonPath("$.sector.name").value(sector.name().value()))
                .andExpect(jsonPath("$.latest.weighedOn").value("2026-09-24"))
                .andExpect(jsonPath("$.latest.averageWeight").value(161.4))
                .andExpect(jsonPath("$.history[0].weighedOn").value("2026-09-24"))
                .andExpect(jsonPath("$.history[0].change").value(3.4))
                .andExpect(jsonPath("$.history[0].recordedBy.name").value(commonUser.fullName()))
                .andExpect(jsonPath("$.history[1].weighedOn").value("2026-09-17"))
                .andExpect(jsonPath("$.history[1].change").doesNotExist());
    }

    @Test
    @DisplayName("answers the overview of a cage without weighings, with no latest and an empty history")
    void givenNoWeighing_whenAskingTheOverview_thenAnswerNoLatestAndAnEmptyHistory() throws Exception {
        // given
        String b07 = "/api/v1/sectors/" + sector.id() + "/cages/" + sector.cages().get(1).id();

        // when
        ResultActions response = overviewOf(b07);

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.cage.code").value("B-07"))
                .andExpect(jsonPath("$.latest").doesNotExist())
                .andExpect(jsonPath("$.history").isEmpty());
    }

    @Test
    @DisplayName("answers 404 for a cage that does not exist")
    void givenUnknownCage_whenAskingTheOverview_thenAnswerNotFound() throws Exception {
        // given
        String path = "/api/v1/sectors/" + sector.id() + "/cages/" + UUID.randomUUID();

        // when
        ResultActions response = overviewOf(path);

        // then
        response.andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("CAGE_NOT_FOUND"));
    }

    // ---------------------------------------------------------------- acompanhamento (US3)

    @Test
    @DisplayName("answers the overview of the contract example: five weekly weighings within the range of the sector")
    void givenFiveWeeklyWeighingsAndTheRange_whenAskingTheOverview_thenAnswerTheContractExample() throws Exception {
        // given
        Sector withRange = aUniqueSector().withReferenceWeight(155, 175).withCage("A", 1, 48).build();
        sectors.save(withRange);
        String path = "/api/v1/sectors/" + withRange.id() + "/cages/" + withRange.cages().get(0).id();
        String[][] weekly = {
            {"2026-08-27", "150"}, {"2026-09-03", "153"}, {"2026-09-10", "156"}, {"2026-09-17", "158"},
            {"2026-09-24", "161"}
        };
        for (String[] weighing : weekly) {
            record(path, """
                    {"weighedOn": "%s", "averageWeight": %s}
                    """.formatted(weighing[0], weighing[1])).andExpect(status().isCreated());
        }

        // when
        ResultActions response = overviewOf(path);

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.sector.referenceWeight.minimum").value(155))
                .andExpect(jsonPath("$.sector.referenceWeight.maximum").value(175))
                .andExpect(jsonPath("$.latest.averageWeight").value(161.0))
                .andExpect(jsonPath("$.fourWeekChange.change").value(11.0))
                .andExpect(jsonPath("$.fourWeekChange.since").value("2026-08-27"))
                .andExpect(jsonPath("$.rangeStatus").value("WITHIN"))
                .andExpect(jsonPath("$.chart.length()").value(5))
                .andExpect(jsonPath("$.chart[0].weighedOn").value("2026-08-27"))
                .andExpect(jsonPath("$.chart[4].averageWeight").value(161.0))
                .andExpect(jsonPath("$.history.length()").value(5));
    }

    @Test
    @DisplayName("answers an empty chart and no situation for a cage never weighed")
    void givenNoWeighing_whenAskingTheOverview_thenAnswerAnEmptyChartAndNoSituation() throws Exception {
        // given
        String b07 = "/api/v1/sectors/" + sector.id() + "/cages/" + sector.cages().get(1).id();

        // when
        ResultActions response = overviewOf(b07);

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.chart").isEmpty())
                .andExpect(jsonPath("$.rangeStatus").doesNotExist())
                .andExpect(jsonPath("$.fourWeekChange").doesNotExist());
    }

    // ---------------------------------------------------------------- correção e exclusão (US4)

    private String recorded(String path, String day, String weight) throws Exception {
        String response = record(path, """
                {"weighedOn": "%s", "averageWeight": %s}
                """.formatted(day, weight))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return path + "/weighings/" + JsonPath.read(response, "$.id");
    }

    private ResultActions correct(String weighing, String body) throws Exception {
        return mockMvc.perform(put(weighing)
                .with(sessions.csrf())
                .cookie(commonUser.cookies())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions voidOf(String weighing) throws Exception {
        return mockMvc.perform(post(weighing + "/voiding").with(sessions.csrf()).cookie(commonUser.cookies()));
    }

    @Test
    @DisplayName("finds a weighing: 200 with who recorded it; 404 for another cage")
    void givenWeighing_whenFindingIt_thenAnswerItAndNotThroughAnotherCage() throws Exception {
        // given
        String weighing = recorded(cagePath, "2026-09-24", "161.4");
        String b07 = "/api/v1/sectors/" + sector.id() + "/cages/" + sector.cages().get(1).id();

        // when
        ResultActions found = mockMvc.perform(get(weighing).cookie(commonUser.cookies()));
        ResultActions elsewhere = mockMvc.perform(
                get(b07 + weighing.substring(weighing.indexOf("/weighings"))).cookie(commonUser.cookies()));

        // then
        found.andExpect(status().isOk())
                .andExpect(jsonPath("$.averageWeight").value(161.4))
                .andExpect(jsonPath("$.recordedBy.name").value(commonUser.fullName()));
        elsewhere.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("WEIGHING_NOT_FOUND"));
    }

    @Test
    @DisplayName("corrects a weighing: 200 with who corrected it")
    void givenWeighing_whenCorrecting_thenAnswerTheCorrection() throws Exception {
        // given
        String weighing = recorded(cagePath, "2026-09-24", "116");

        // when
        ResultActions response = correct(weighing, """
                {"weighedOn": "2026-09-24", "averageWeight": 161}
                """);

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.averageWeight").value(161.0))
                .andExpect(jsonPath("$.lastCorrectedBy.name").value(commonUser.fullName()))
                .andExpect(jsonPath("$.lastCorrectedAt").isNotEmpty());
    }

    @Test
    @DisplayName("refuses a correction to the day of another weighing with 409, and an invalid one with 400")
    void givenTwoWeighings_whenCorrectingBadly_thenAnswerConflictAndBadRequest() throws Exception {
        // given
        recorded(cagePath, "2026-09-17", "158");
        String weighing = recorded(cagePath, "2026-09-24", "161");

        // when
        ResultActions taken = correct(weighing, """
                {"weighedOn": "2026-09-17", "averageWeight": 161}
                """);
        ResultActions invalid = correct(weighing, """
                {"weighedOn": "2026-09-24", "averageWeight": 10001}
                """);

        // then
        taken.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("WEIGHING_DATE_IN_USE"));
        invalid.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.averageWeight").value("O peso médio deve ficar entre 1 e 10.000 gramas."));
    }

    @Test
    @DisplayName("voids a weighing: 204, again 204; the voided one answers 404 and its day is free again")
    void givenWeighing_whenVoiding_thenHideItAndFreeItsDay() throws Exception {
        // given
        String weighing = recorded(cagePath, "2026-09-23", "999");

        // when
        ResultActions voided = voidOf(weighing);
        ResultActions again = voidOf(weighing);

        // then
        voided.andExpect(status().isNoContent());
        again.andExpect(status().isNoContent());
        mockMvc.perform(get(weighing).cookie(commonUser.cookies()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WEIGHING_NOT_FOUND"));
        correct(weighing, """
                {"weighedOn": "2026-09-23", "averageWeight": 160}
                """).andExpect(status().isNotFound());
        overviewOf(cagePath).andExpect(jsonPath("$.history").isEmpty());
        record(cagePath, """
                {"weighedOn": "2026-09-23", "averageWeight": 160}
                """).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("refuses to void a weighing of an inactive cage with 409")
    void givenWeighingOfACageLaterDeactivated_whenVoiding_thenAnswerConflict() throws Exception {
        // given
        String weighing = recorded(cagePath, "2026-09-24", "161");
        Sector reloaded = sectors.findById(sector.id()).orElseThrow();
        reloaded.deactivateCage(sector.cages().get(0).id(), clock);
        sectors.save(reloaded);

        // when
        ResultActions response = voidOf(weighing);

        // then
        response.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CAGE_INACTIVE"));
    }
}
