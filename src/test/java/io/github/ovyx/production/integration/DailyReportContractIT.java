package io.github.ovyx.production.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.ovyx.IntegrationSessions;
import io.github.ovyx.IntegrationSessions.SignedIn;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Contrato das operações de relatório diário, contra a aplicação em execução e PostgreSQL real (US1 a
 * US4; S-01 a S-12; {@code contracts/production-api.yaml}).
 *
 * <p>Cada teste age com um usuário comum próprio, recém-cadastrado, e com um setor próprio: o relatório é
 * de qualquer responsável autenticado (FR-019).
 */
@AutoConfigureMockMvc
@DisplayName("Daily report contract")
class DailyReportContractIT extends IntegrationTestSupport {

    private static final String PROBLEM_JSON = "application/problem+json";

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
    private SignedIn commonUser;
    private ProductionFixtures fixtures;

    @BeforeEach
    void signInAsACommonUser() throws Exception {
        sessions = new IntegrationSessions(mockMvc, caretakerRepository, passwordHasher, clock);
        commonUser = sessions.commonUser();
        fixtures = new ProductionFixtures(jdbc);
    }

    private static String reportsOf(UUID sectorId) {
        return "/api/v1/sectors/" + sectorId + "/daily-reports";
    }

    private static String body(String date, Object birds, Object age) {
        return """
                {"collectionDate": "%s", "collectionTime": "06:42", "openingBirdCount": %s, "flockAge": %s,
                 "note": "Tarde quente."}
                """.formatted(date, birds, age);
    }

    private ResultActions open(UUID sectorId, String body) throws Exception {
        return mockMvc.perform(post(reportsOf(sectorId))
                .with(sessions.csrf())
                .cookie(commonUser.cookies())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions read(String path) throws Exception {
        return mockMvc.perform(get(path).cookie(commonUser.cookies()).accept(MediaType.APPLICATION_JSON));
    }

    /** Um relatório aberto de 24/09, com a A-01 e a B-07, pela própria API. */
    private OpenedReport openedReport(UUID sectorId) throws Exception {
        String opened = open(sectorId, body("2026-09-24", 98, 20))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new OpenedReport(
                sectorId, JsonPath.read(opened, "$.id"), JsonPath.read(opened, "$.cages[0].cageId"), JsonPath.read(opened, "$.cages[1].cageId"));
    }

    private record OpenedReport(UUID sectorId, String id, String a01, String b07) {

        String path() {
            return reportsOf(sectorId) + "/" + id;
        }

        String cage(String cageId) {
            return path() + "/cages/" + cageId;
        }
    }

    private ResultActions correct(String reportPath, String body, SignedIn who) throws Exception {
        return mockMvc.perform(put(reportPath)
                .with(sessions.csrf())
                .cookie(who.cookies())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions recordMortality(String cagePath, String body) throws Exception {
        return mockMvc.perform(put(cagePath + "/mortality")
                .with(sessions.csrf())
                .cookie(commonUser.cookies())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions confirmNoMortality(String reportPath) throws Exception {
        return mockMvc.perform(post(reportPath + "/mortality-confirmation")
                .with(sessions.csrf())
                .cookie(commonUser.cookies())
                .accept(MediaType.APPLICATION_JSON));
    }

    private ResultActions recordProduction(String cagePath, String body) throws Exception {
        return mockMvc.perform(put(cagePath + "/production")
                .with(sessions.csrf())
                .cookie(commonUser.cookies())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // ---------------------------------------------------------------------------------- abertura

    @Test
    @DisplayName("opens a report: 201, Location and the report, with who opened it and the cages pending")
    void givenValidReport_whenOpening_thenAnswerCreatedWithTheReport() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();

        // when
        ResultActions response = open(sectorId, body("2026-09-24", 98, 20));

        // then
        String id = JsonPath.read(response.andReturn().getResponse().getContentAsString(), "$.id");
        response.andExpect(status().isCreated())
                .andExpect(header().string("Location", reportsOf(sectorId) + "/" + id))
                .andExpect(jsonPath("$.sector.id").value(sectorId.toString()))
                .andExpect(jsonPath("$.sector.status").value("ACTIVE"))
                .andExpect(jsonPath("$.collectionDate").value("2026-09-24"))
                .andExpect(jsonPath("$.collectionTime").value("06:42"))
                .andExpect(jsonPath("$.openingBirdCount").value(98))
                .andExpect(jsonPath("$.flockAge").value(20))
                .andExpect(jsonPath("$.note").value("Tarde quente."))
                .andExpect(jsonPath("$.noMortalityConfirmed").value(false))
                .andExpect(jsonPath("$.openedBy.id").value(commonUser.id().toString()))
                .andExpect(jsonPath("$.openedBy.name").value(commonUser.fullName()))
                .andExpect(jsonPath("$.openedAt").isNotEmpty())
                .andExpect(jsonPath("$.lastCorrectedBy").doesNotExist())
                .andExpect(jsonPath("$.production.status").value("PENDING"))
                .andExpect(jsonPath("$.production.pendingCages").value(2))
                .andExpect(jsonPath("$.mortality.status").value("PENDING"))
                .andExpect(jsonPath("$.mortality.closingBirdCount").value(98))
                .andExpect(jsonPath("$.cages[0].code").value("A-01"))
                .andExpect(jsonPath("$.cages[0].birdCount").value(48))
                .andExpect(jsonPath("$.cages[0].production").doesNotExist())
                .andExpect(jsonPath("$.cages[1].code").value("B-07"));
    }

    @Test
    @DisplayName("accepts the quantities as typed text, as the form sends them")
    void givenQuantitiesAsText_whenOpening_thenAcceptThem() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();

        // when
        ResultActions response = open(sectorId, body("2026-09-24", "\"98\"", "\"20\""));

        // then
        response.andExpect(status().isCreated()).andExpect(jsonPath("$.openingBirdCount").value(98));
    }

    @Test
    @DisplayName("refuses a future date, no birds and 200 weeks at once: 400 with the three fields")
    void givenThreeInvalidFields_whenOpening_thenAnswerBadRequestWithTheThreeFields() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();
        String tomorrow = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(2).toString();

        // when
        ResultActions response = open(sectorId, body(tomorrow, 0, 200));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.collectionDate").value("A data da coleta não pode ser futura."))
                .andExpect(jsonPath("$.details.openingBirdCount")
                        .value("As aves do início do dia devem ficar entre 1 e 1.000.000."))
                .andExpect(jsonPath("$.details.flockAge").value("A idade do lote deve ficar entre 1 e 150 semanas."));
    }

    @Test
    @DisplayName("refuses a quantity that is not an integer in its own field, and not as an unreadable body")
    void givenDecimalQuantity_whenOpening_thenRefuseItInTheField() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();

        // when
        ResultActions response = open(sectorId, body("2026-09-24", 12.5, 20));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.openingBirdCount")
                        .value("As aves do início do dia devem ser um número inteiro."));
    }

    @Test
    @DisplayName("refuses a second report of the sector on the same date: 409 with the date")
    void givenReportOnTheDate_whenOpeningAnotherOnTheSameDate_thenAnswerConflict() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();
        open(sectorId, body("2026-09-24", 98, 20)).andExpect(status().isCreated());

        // when
        ResultActions response = open(sectorId, body("2026-09-24", 98, 20));

        // then
        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DAILY_REPORT_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.details.collectionDate")
                        .value("Já existe o relatório de 24/09/2026 neste setor."));
    }

    @Test
    @DisplayName("refuses a report in an inactive sector: 409")
    void givenInactiveSector_whenOpening_thenAnswerConflict() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();
        fixtures.deactivate(sectorId);

        // when
        ResultActions response = open(sectorId, body("2026-09-24", 98, 20));

        // then
        response.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SECTOR_INACTIVE"));
    }

    @Test
    @DisplayName("refuses a report in a sector without active cages: 409")
    void givenSectorWithoutActiveCages_whenOpening_thenAnswerConflict() throws Exception {
        // given
        UUID sectorId = fixtures.activeSector();

        // when
        ResultActions response = open(sectorId, body("2026-09-24", 98, 20));

        // then
        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SECTOR_WITHOUT_ACTIVE_CAGES"));
    }

    @Test
    @DisplayName("answers 404 for a sector that does not exist, and for a malformed identifier")
    void givenUnknownOrMalformedSector_whenOpening_thenAnswerNotFound() throws Exception {
        // given
        String malformed = "/api/v1/sectors/galpao-9/daily-reports";

        // when
        ResultActions unknown = open(UUID.randomUUID(), body("2026-09-24", 98, 20));
        ResultActions torto = mockMvc.perform(post(malformed)
                .with(sessions.csrf())
                .cookie(commonUser.cookies())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("2026-09-24", 98, 20)));

        // then
        unknown.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SECTOR_NOT_FOUND"));
        torto.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SECTOR_NOT_FOUND"));
    }

    @Test
    @DisplayName("answers 415 for a body that is not JSON")
    void givenBodyThatIsNotJson_whenOpening_thenAnswerUnsupportedMediaType() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();

        // when
        ResultActions response = mockMvc.perform(post(reportsOf(sectorId))
                .with(sessions.csrf())
                .cookie(commonUser.cookies())
                .contentType(MediaType.TEXT_PLAIN)
                .content("relatório"));

        // then
        response.andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("answers 406 when JSON is not acceptable")
    void givenClientNotAcceptingJson_whenOpening_thenAnswerNotAcceptable() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();

        // when
        ResultActions response = mockMvc.perform(post(reportsOf(sectorId))
                .with(sessions.csrf())
                .cookie(commonUser.cookies())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_PLAIN)
                .content(body("2026-09-24", 98, 20)));

        // then
        response.andExpect(status().isNotAcceptable());
    }

    // ---------------------------------------------------------------------------------- produção

    @Test
    @DisplayName("records the production of a cage: 200 with the cage, and the grades left blank as zero")
    void givenValidProduction_whenRecording_thenAnswerTheCageWithTheProduction() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());

        // when
        ResultActions response = recordProduction(
                report.cage(report.b07()), """
                {"eggs": 45, "jumbo": 1, "dirty": 1, "cracked": 2, "bloodSpot": 1}
                """);

        // then
        response.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cageId").value(report.b07()))
                .andExpect(jsonPath("$.code").value("B-07"))
                .andExpect(jsonPath("$.battery").value("B"))
                .andExpect(jsonPath("$.number").value(7))
                .andExpect(jsonPath("$.birdCount").value(50))
                .andExpect(jsonPath("$.production.eggs").value(45))
                .andExpect(jsonPath("$.production.small").value(0))
                .andExpect(jsonPath("$.production.jumbo").value(1))
                .andExpect(jsonPath("$.production.dirty").value(1))
                .andExpect(jsonPath("$.production.cracked").value(2))
                .andExpect(jsonPath("$.production.bloodSpot").value(1))
                .andExpect(jsonPath("$.production.abnormal").value(0))
                .andExpect(jsonPath("$.mortality").doesNotExist());
    }

    @Test
    @DisplayName("accepts the quantities as typed text, and a blank grade as zero")
    void givenQuantitiesAsText_whenRecording_thenAcceptThem() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());

        // when
        ResultActions response = recordProduction(report.cage(report.a01()), """
                {"eggs": "44", "small": "", "cracked": "1"}
                """);

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.production.eggs").value(44))
                .andExpect(jsonPath("$.production.small").value(0))
                .andExpect(jsonPath("$.production.cracked").value(1));
    }

    @Test
    @DisplayName("refuses grades above the eggs: 400 in the eggs field, with both numbers")
    void givenGradesAboveTheEggs_whenRecording_thenAnswerBadRequestInTheEggsField() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());

        // when
        ResultActions response = recordProduction(report.cage(report.b07()), """
                {"eggs": 30, "small": 10, "jumbo": 10, "dirty": 10, "cracked": 4}
                """);

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.eggs").value("As classificações somam 34, mais que os 30 ovos coletados."));
    }

    @Test
    @DisplayName("refuses blank eggs, decimal cracked and negative dirty at once: 400 with the three fields")
    void givenThreeInvalidFields_whenRecording_thenAnswerBadRequestWithTheThreeFields() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());

        // when
        ResultActions response = recordProduction(report.cage(report.b07()), """
                {"eggs": "", "cracked": 2.5, "dirty": -1}
                """);

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.eggs").value("Informe os ovos coletados."))
                .andExpect(jsonPath("$.details.cracked").value("A quantidade de trincados deve ser um número inteiro."))
                .andExpect(jsonPath("$.details.dirty").value("A quantidade de sujos deve ficar entre 0 e 1.000."));
    }

    @Test
    @DisplayName("refuses the production of the report of an inactive sector: 409")
    void givenInactiveSector_whenRecording_thenAnswerConflict() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();
        OpenedReport report = openedReport(sectorId);
        fixtures.deactivate(sectorId);

        // when
        ResultActions response = recordProduction(report.cage(report.b07()), "{\"eggs\": 45}");

        // then
        response.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SECTOR_INACTIVE"));
    }

    @Test
    @DisplayName("answers 404 for a cage out of the report, a malformed cage and a report that does not exist")
    void givenUnknownCageOrReport_whenRecording_thenAnswerNotFound() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());

        // when
        ResultActions outOfTheReport = recordProduction(report.cage(UUID.randomUUID().toString()), "{\"eggs\": 45}");
        ResultActions malformed = recordProduction(report.cage("B-07"), "{\"eggs\": 45}");
        ResultActions noReport = recordProduction(
                reportsOf(report.sectorId()) + "/" + UUID.randomUUID() + "/cages/" + report.b07(), "{\"eggs\": 45}");

        // then
        outOfTheReport.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CAGE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Gaiola não encontrada neste relatório."));
        malformed.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CAGE_NOT_FOUND"));
        noReport.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("DAILY_REPORT_NOT_FOUND"));
    }

    @Test
    @DisplayName("answers 415 for a production that is not JSON")
    void givenBodyThatIsNotJson_whenRecording_thenAnswerUnsupportedMediaType() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());

        // when
        ResultActions response = mockMvc.perform(put(report.cage(report.b07()) + "/production")
                .with(sessions.csrf())
                .cookie(commonUser.cookies())
                .contentType(MediaType.TEXT_PLAIN)
                .content("45 ovos"));

        // then
        response.andExpect(status().isUnsupportedMediaType());
    }

    // ---------------------------------------------------------------------------------- correção

    @Test
    @DisplayName("corrects the general data: 200 with the report, who opened it and who corrected it")
    void givenValidCorrection_whenCorrecting_thenAnswerTheReportWithTheAuthors() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());

        // when
        ResultActions response = correct(report.path(), body("2026-09-24", 96, 21), commonUser);

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(report.id()))
                .andExpect(jsonPath("$.openingBirdCount").value(96))
                .andExpect(jsonPath("$.flockAge").value(21))
                .andExpect(jsonPath("$.openedBy.name").value(commonUser.fullName()))
                .andExpect(jsonPath("$.lastCorrectedBy.id").value(commonUser.id().toString()))
                .andExpect(jsonPath("$.lastCorrectedAt").isNotEmpty())
                .andExpect(jsonPath("$.cages[1].code").value("B-07"));
    }

    @Test
    @DisplayName("keeps both names when one person opens the report and another corrects it")
    void givenReportOpenedByAUser_whenAnAdministratorCorrectsIt_thenKeepBothNames() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());
        SignedIn administrator = sessions.administrator();
        correct(report.path(), body("2026-09-24", 98, 21), administrator).andExpect(status().isOk());

        // when
        ResultActions response = read(report.path());

        // then
        response.andExpect(jsonPath("$.openedBy.name").value(commonUser.fullName()))
                .andExpect(jsonPath("$.lastCorrectedBy.name").value(administrator.fullName()));
    }

    @Test
    @DisplayName("refuses opening birds below the birds already removed: 400 in the opening birds field")
    void givenTwoBirdsRemoved_whenCorrectingTheOpeningBirdsToOne_thenAnswerBadRequest() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());
        recordMortality(report.cage(report.b07()), "{\"deaths\": 1, \"culls\": 1}").andExpect(status().isOk());

        // when
        ResultActions response = correct(report.path(), body("2026-09-24", 1, 20), commonUser);

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.openingBirdCount").value(
                        "O relatório já tem 2 aves removidas; as aves do início do dia não podem ficar abaixo disso."));
    }

    @Test
    @DisplayName("refuses the date of another report of the sector: 409 with the date")
    void givenDateOfAnotherReport_whenCorrecting_thenAnswerConflict() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();
        open(sectorId, body("2026-09-23", 98, 20)).andExpect(status().isCreated());
        OpenedReport report = openedReport(sectorId);

        // when
        ResultActions response = correct(report.path(), body("2026-09-23", 98, 20), commonUser);

        // then
        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DAILY_REPORT_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.details.collectionDate").value("Já existe o relatório de 23/09/2026 neste setor."));
    }

    @Test
    @DisplayName("refuses the correction in an inactive sector (409) and of a report that does not exist (404)")
    void givenInactiveSectorOrNoReport_whenCorrecting_thenAnswerConflictOrNotFound() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();
        OpenedReport report = openedReport(sectorId);
        ResultActions noReport = correct(reportsOf(sectorId) + "/" + UUID.randomUUID(), body("2026-09-24", 98, 20), commonUser);
        fixtures.deactivate(sectorId);

        // when
        ResultActions inactive = correct(report.path(), body("2026-09-24", 98, 21), commonUser);

        // then
        noReport.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("DAILY_REPORT_NOT_FOUND"));
        inactive.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SECTOR_INACTIVE"));
    }

    // ---------------------------------------------------------------------------------- mortalidade

    @Test
    @DisplayName("records the mortality of a cage: 200 with the cage, the deaths, the culls and the note")
    void givenValidMortality_whenRecording_thenAnswerTheCageWithTheMortality() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());

        // when
        ResultActions response = recordMortality(report.cage(report.b07()), """
                {"deaths": 1, "culls": "1", "note": "Prostração e penas eriçadas."}
                """);

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B-07"))
                .andExpect(jsonPath("$.birdCount").value(50))
                .andExpect(jsonPath("$.mortality.deaths").value(1))
                .andExpect(jsonPath("$.mortality.culls").value(1))
                .andExpect(jsonPath("$.mortality.note").value("Prostração e penas eriçadas."))
                .andExpect(jsonPath("$.production").doesNotExist());
    }

    @Test
    @DisplayName("refuses deaths and culls above the birds of the cage: 400 in the deaths field")
    void givenRemovalsAboveTheBirdsOfTheCage_whenRecording_thenAnswerBadRequest() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());

        // when
        ResultActions response = recordMortality(report.cage(report.b07()), "{\"deaths\": 40, \"culls\": 15}");

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.deaths").value("A gaiola tem 50 aves; mortes e descartes somam 55."));
    }

    @Test
    @DisplayName("refuses removals of the day above the birds of the start of the day: 400 in the deaths field")
    void givenRemovalsOfTheDayAboveTheOpeningBirds_whenRecording_thenAnswerBadRequest() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();
        String opened = open(sectorId, body("2026-09-24", 3, 20)).andReturn().getResponse().getContentAsString();
        OpenedReport report = new OpenedReport(
                sectorId, JsonPath.read(opened, "$.id"), JsonPath.read(opened, "$.cages[0].cageId"), JsonPath.read(opened, "$.cages[1].cageId"));
        recordMortality(report.cage(report.a01()), "{\"deaths\": 2}").andExpect(status().isOk());

        // when
        ResultActions response = recordMortality(report.cage(report.b07()), "{\"deaths\": 2}");

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.deaths")
                        .value("Mortes e descartes do dia somariam 4, mais que as 3 aves do início do dia."));
    }

    @Test
    @DisplayName("refuses negative deaths and decimal culls at once: 400 with both fields")
    void givenInvalidDeathsAndCulls_whenRecording_thenAnswerBadRequestWithBothFields() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());

        // when
        ResultActions response = recordMortality(report.cage(report.b07()), "{\"deaths\": -1, \"culls\": 1.5}");

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.deaths").value("As mortes devem ficar entre 0 e 1.000."))
                .andExpect(jsonPath("$.details.culls").value("Os descartes devem ser um número inteiro."));
    }

    @Test
    @DisplayName("answers 404 for the mortality of a cage out of the report, and 409 in an inactive sector")
    void givenCageOutOfTheReportOrInactiveSector_whenRecording_thenAnswerNotFoundOrConflict() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();
        OpenedReport report = openedReport(sectorId);
        ResultActions outOfTheReport = recordMortality(report.cage(UUID.randomUUID().toString()), "{\"deaths\": 1}");
        fixtures.deactivate(sectorId);

        // when
        ResultActions inactive = recordMortality(report.cage(report.b07()), "{\"deaths\": 1}");

        // then
        outOfTheReport.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CAGE_NOT_FOUND"));
        inactive.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SECTOR_INACTIVE"));
    }

    @Test
    @DisplayName("keeps the birds of the cage in the farm after the mortality (FR-015)")
    void givenMortalityRecorded_whenReadingTheCageInTheFarm_thenFindTheSameBirds() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();
        OpenedReport report = openedReport(sectorId);
        recordMortality(report.cage(report.b07()), "{\"deaths\": 2, \"culls\": 1}").andExpect(status().isOk());

        // when
        ResultActions cage = read("/api/v1/sectors/" + sectorId + "/cages/" + report.b07());

        // then
        cage.andExpect(status().isOk()).andExpect(jsonPath("$.birdCount").value(50));
    }

    @Test
    @DisplayName("confirms the day without occurrence: 200 with the report and the mortality recorded")
    void givenReportWithoutOccurrence_whenConfirming_thenAnswerTheReport() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());

        // when
        ResultActions response = confirmNoMortality(report.path());

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(report.id()))
                .andExpect(jsonPath("$.noMortalityConfirmed").value(true))
                .andExpect(jsonPath("$.mortality.status").value("RECORDED"))
                .andExpect(jsonPath("$.mortality.closingBirdCount").value(98));
    }

    @Test
    @DisplayName("refuses to confirm a day with a death recorded: 409, and 404 for a report that does not exist")
    void givenOccurrenceOrNoReport_whenConfirming_thenAnswerConflictOrNotFound() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());
        recordMortality(report.cage(report.b07()), "{\"deaths\": 1}").andExpect(status().isOk());

        // when
        ResultActions recorded = confirmNoMortality(report.path());
        ResultActions noReport = confirmNoMortality(reportsOf(report.sectorId()) + "/" + UUID.randomUUID());

        // then
        recorded.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MORTALITY_ALREADY_RECORDED"))
                .andExpect(jsonPath("$.detail").value("O relatório já tem mortes ou descartes lançados."));
        noReport.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("DAILY_REPORT_NOT_FOUND"));
    }

    // ---------------------------------------------------------------------------------- detalhe e gaiola

    @Test
    @DisplayName("details the report with the cages and the totals of the day")
    void givenOneCageRecorded_whenReadingTheReport_thenAnswerTheCagesAndTheTotals() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());
        recordProduction(report.cage(report.b07()), "{\"eggs\": 45, \"cracked\": 2}").andExpect(status().isOk());

        // when
        ResultActions response = read(report.path());

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(report.id()))
                .andExpect(jsonPath("$.lastCorrectedBy.name").value(commonUser.fullName()))
                .andExpect(jsonPath("$.lastCorrectedAt").isNotEmpty())
                .andExpect(jsonPath("$.cages[0].production").doesNotExist())
                .andExpect(jsonPath("$.cages[1].production.eggs").value(45))
                .andExpect(jsonPath("$.production.status").value("PENDING"))
                .andExpect(jsonPath("$.production.pendingCages").value(1))
                .andExpect(jsonPath("$.production.collectedEggs").value(45))
                .andExpect(jsonPath("$.production.standardEggs").value(43))
                .andExpect(jsonPath("$.production.unsellableEggs").value(2))
                .andExpect(jsonPath("$.production.layingRate").value(45.92));
    }

    @Test
    @DisplayName("answers 404 for a report that does not exist, a malformed one and one of another sector")
    void givenUnknownMalformedOrForeignReport_whenReading_thenAnswerNotFound() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());
        UUID otherSector = fixtures.sectorWithTwoCages();

        // when
        ResultActions unknown = read(reportsOf(report.sectorId()) + "/" + UUID.randomUUID());
        ResultActions malformed = read(reportsOf(report.sectorId()) + "/24-09-2026");
        ResultActions foreign = read(reportsOf(otherSector) + "/" + report.id());

        // then
        unknown.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("DAILY_REPORT_NOT_FOUND"));
        malformed.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("DAILY_REPORT_NOT_FOUND"));
        foreign.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("DAILY_REPORT_NOT_FOUND"));
    }

    @Test
    @DisplayName("reads one cage of the report, with the production recorded")
    void givenRecordedCage_whenReadingIt_thenAnswerTheCage() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());
        recordProduction(report.cage(report.b07()), "{\"eggs\": 45}").andExpect(status().isOk());

        // when
        ResultActions response = read(report.cage(report.b07()));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B-07"))
                .andExpect(jsonPath("$.production.eggs").value(45));
    }

    @Test
    @DisplayName("answers 404 for a cage out of the report")
    void givenCageOutOfTheReport_whenReadingIt_thenAnswerNotFound() throws Exception {
        // given
        OpenedReport report = openedReport(fixtures.sectorWithTwoCages());

        // when
        ResultActions response = read(report.cage(UUID.randomUUID().toString()));

        // then
        response.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("CAGE_NOT_FOUND"));
    }

    // ---------------------------------------------------------------------------------- lista

    @Test
    @DisplayName("lists the reports of the sector with the sector")
    void givenTwoReports_whenListing_thenAnswerThePageWithTheSector() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();
        open(sectorId, body("2026-09-23", 98, 20)).andExpect(status().isCreated());
        open(sectorId, body("2026-09-24", 98, 20)).andExpect(status().isCreated());

        // when
        ResultActions response = read(reportsOf(sectorId));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.sector.name").value(fixtures.nameOf(sectorId)))
                .andExpect(jsonPath("$.content[0].collectionDate").value("2026-09-24"))
                .andExpect(jsonPath("$.content[0].collectionTime").value("06:42"))
                .andExpect(jsonPath("$.content[0].openedByName").value(commonUser.fullName()))
                .andExpect(jsonPath("$.content[0].productionStatus").value("PENDING"))
                .andExpect(jsonPath("$.content[0].pendingCages").value(2))
                .andExpect(jsonPath("$.content[1].collectionDate").value("2026-09-23"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("filters the list by the date of the collection")
    void givenTwoReports_whenFilteringByDate_thenAnswerOnlyThatDay() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();
        open(sectorId, body("2026-09-23", 98, 20)).andExpect(status().isCreated());
        open(sectorId, body("2026-09-24", 98, 20)).andExpect(status().isCreated());

        // when
        ResultActions response = read(reportsOf(sectorId) + "?collectionDate=2026-09-23");

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].collectionDate").value("2026-09-23"));
    }

    @Test
    @DisplayName("refuses a malformed date and a page size above 100: 400")
    void givenMalformedDateOrLargePage_whenListing_thenAnswerBadRequest() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();

        // when
        ResultActions malformedDate = read(reportsOf(sectorId) + "?collectionDate=24/09/2026");
        ResultActions largePage = read(reportsOf(sectorId) + "?size=101");

        // then
        malformedDate.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.parameter").value("collectionDate"));
        largePage.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.size").value("O tamanho da página deve estar entre 1 e 100."));
    }

    // ---------------------------------------------------------------------------------- sugestão

    @Test
    @DisplayName("suggests the birds of the active cages and no age for the first report")
    void givenSectorWithoutReports_whenAskingForTheSuggestion_thenSuggestTheBirdsOfTheCagesAndNoAge()
            throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();

        // when
        ResultActions response = read(reportsOf(sectorId) + "/suggestion");

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBirdCount").value(98))
                .andExpect(jsonPath("$.flockAge").doesNotExist())
                .andExpect(jsonPath("$.collectionDate").isNotEmpty())
                .andExpect(jsonPath("$.collectionTime").isNotEmpty());
    }

    @Test
    @DisplayName("suggests the closing birds and the age of the latest report")
    void givenReport_whenAskingForTheSuggestion_thenSuggestItsClosingBirdsAndAge() throws Exception {
        // given
        UUID sectorId = fixtures.sectorWithTwoCages();
        open(sectorId, body(LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString(), 96, 21))
                .andExpect(status().isCreated());

        // when
        ResultActions response = read(reportsOf(sectorId) + "/suggestion");

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBirdCount").value(96))
                .andExpect(jsonPath("$.flockAge").value(21));
    }
}
