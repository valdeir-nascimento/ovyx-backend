package io.github.ovyx.production.integration;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ovyx.IntegrationSessions;
import io.github.ovyx.IntegrationSessions.SignedIn;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.FarmCalendar;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.LocalDate;
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
 * Contrato do painel da granja toda, contra a aplicação em execução e PostgreSQL real (US1 da 009; S-02, S-08;
 * {@code contracts/production-api.yaml}).
 *
 * <p>O banco é dividido com os outros testes, que também cadastram setores com relatórios de hoje: aqui se confere a
 * forma e as escalas da resposta, e as somas exatas ficam no {@code FarmDashboardTest}.
 */
@AutoConfigureMockMvc
@DisplayName("Farm dashboard contract")
class FarmDashboardContractIT extends IntegrationTestSupport {

    private static final String FARM = "/api/v1/dashboard/farm";
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
    private FarmCalendar calendar;

    @Autowired
    private JdbcTemplate jdbc;

    private SignedIn commonUser;
    private ProductionFixtures fixtures;
    private LocalDate today;

    @BeforeEach
    void setUp() throws Exception {
        commonUser = new IntegrationSessions(mockMvc, caretakerRepository, passwordHasher, clock).commonUser();
        fixtures = new ProductionFixtures(jdbc);
        today = calendar.today();
    }

    private ResultActions read(String path, String period) throws Exception {
        return mockMvc.perform(
                get(path).queryParam("period", period).cookie(commonUser.cookies()).accept(MediaType.APPLICATION_JSON));
    }

    /** Um setor com a A-01 (48 aves) e o relatório de hoje completo, com 44 ovos e a Postura Plus. */
    private UUID sectorWithTodayReport() {
        UUID sectorId = fixtures.activeSector();
        UUID a01 = fixtures.activeCage(sectorId, "A", 1, 48);
        UUID report = fixtures.report(sectorId, today, 48, true);
        fixtures.cageIn(report, a01, 48);
        fixtures.production(report, a01, 44, 0, 0, 0, 0, 0, 0);
        fixtures.feed(report, a01, fixtures.posturaPlus(), 1344);
        return sectorId;
    }

    @Test
    @DisplayName("gives the farm of today, with the counts, the indicators in the scales of the contract and the 7 days")
    void givenSectorsWithReportsAndOneWithout_whenReadingToday_thenAnswerTheFieldsOfTheContract() throws Exception {
        // given
        sectorWithTodayReport();
        sectorWithTodayReport();
        fixtures.activeSector();

        // when
        ResultActions response = read(FARM, "TODAY");

        // then
        response.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.period").value("TODAY"))
                .andExpect(jsonPath("$.from").value(today.toString()))
                .andExpect(jsonPath("$.to").value(today.toString()))
                .andExpect(jsonPath("$.activeSectors").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.reportingSectors").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.indicators.production.goodDirection").value("UP"))
                .andExpect(jsonPath("$.indicators.layingRate.value").isNumber())
                .andExpect(jsonPath("$.indicators.costPerEgg.goodDirection").value("DOWN"))
                .andExpect(jsonPath("$.indicators.feedCost.incompleteDays").isNumber())
                .andExpect(jsonPath("$.trend", hasSize(7)))
                .andExpect(jsonPath("$.trend[6].date").value(today.toString()))
                .andExpect(jsonPath("$.trend[6].reportingSectors").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.sectors").isArray());
    }

    @Test
    @DisplayName("refuses a period that does not exist, naming the parameter, as Problem Details")
    void givenUnknownPeriod_whenReadingTheFarm_thenAnswerBadRequestNamingTheParameter() throws Exception {
        // given
        String period = "MES";

        // when
        ResultActions response = read(FARM, period);

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.parameter").value("period"));
    }

    @Test
    @DisplayName("refuses a format other than JSON: 406")
    void givenPlainTextAccepted_whenReadingTheFarm_thenAnswerNotAcceptable() throws Exception {
        // given
        String path = FARM;

        // when
        ResultActions response =
                mockMvc.perform(get(path).cookie(commonUser.cookies()).accept(MediaType.TEXT_PLAIN));

        // then
        response.andExpect(status().isNotAcceptable());
    }

    // ---------------------------------------------------------------- comparacao dos setores (US2)

    @Test
    @DisplayName("lists each active sector with the fields of the contract and the open alerts of its own dashboard")
    void givenASectorWithTheReportOfToday_whenReadingTheFarm_thenListItWithTheAlertsOfItsDashboard() throws Exception {
        // given
        UUID sectorId = sectorWithTodayReport();
        UUID withoutReport = fixtures.activeSector();
        String sector = mockMvc.perform(get("/api/v1/sectors/" + sectorId + "/dashboard")
                        .queryParam("period", "TODAY")
                        .cookie(commonUser.cookies())
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        int openAlerts = JsonPath.read(sector, "$.openAlerts");

        // when
        ResultActions response = read(FARM, "TODAY");

        // then
        String row = "$.sectors[?(@.sector.id == '%s')]";
        response.andExpect(status().isOk())
                .andExpect(jsonPath(row.formatted(sectorId) + ".sector.name").value(fixtures.nameOf(sectorId)))
                .andExpect(jsonPath(row.formatted(sectorId) + ".production").value(44))
                .andExpect(jsonPath(row.formatted(sectorId) + ".layingRate").value(91.67))
                .andExpect(jsonPath(row.formatted(sectorId) + ".target").value(85.00))
                .andExpect(jsonPath(row.formatted(sectorId) + ".targetStatus").value("ABOVE"))
                .andExpect(jsonPath(row.formatted(sectorId) + ".todayReport.productionStatus").value("COMPLETE"))
                .andExpect(jsonPath(row.formatted(sectorId) + ".openAlerts").value(openAlerts))
                .andExpect(jsonPath(row.formatted(withoutReport) + ".target").value(85.00))
                .andExpect(jsonPath(row.formatted(withoutReport) + ".production").isEmpty())
                .andExpect(jsonPath(row.formatted(withoutReport) + ".todayReport").isEmpty());
    }
}
