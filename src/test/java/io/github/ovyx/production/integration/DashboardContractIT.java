package io.github.ovyx.production.integration;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ovyx.IntegrationSessions.SignedIn;
import io.github.ovyx.IntegrationSessions;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.FarmCalendar;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Contrato das operações do painel, contra a aplicação em execução e PostgreSQL real (US1 da 006; S-01 a
 * S-03; {@code contracts/production-api.yaml}). Os relatórios são de hoje e de ontem da granja, pelo
 * {@link FarmCalendar} da aplicação.
 */
@AutoConfigureMockMvc
@DisplayName("Dashboard contract")
class DashboardContractIT extends IntegrationTestSupport {

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

    private IntegrationSessions sessions;
    private SignedIn commonUser;
    private ProductionFixtures fixtures;
    private LocalDate today;

    @BeforeEach
    void setUp() throws Exception {
        sessions = new IntegrationSessions(mockMvc, caretakerRepository, passwordHasher, clock);
        commonUser = sessions.commonUser();
        fixtures = new ProductionFixtures(jdbc);
        today = calendar.today();
    }

    private ResultActions read(String path) throws Exception {
        return mockMvc.perform(get(path).cookie(commonUser.cookies()).accept(MediaType.APPLICATION_JSON));
    }

    private static String dashboardOf(Object sectorId) {
        return "/api/v1/sectors/" + sectorId + "/dashboard";
    }

    /**
     * Um setor com a A-01 (48 aves) e a B-07 (50 aves), com os relatórios completos de hoje e de ontem, com a
     * Postura Plus (R$ 2,85/kg): hoje 44 + 45 ovos e 1.344 + 1.400 g de ração; ontem 40 + 40 ovos e a mesma
     * ração.
     */
    private UUID sectorWithTwoDays() {
        UUID sectorId = fixtures.activeSector();
        UUID a01 = fixtures.activeCage(sectorId, "A", 1, 48);
        UUID b07 = fixtures.activeCage(sectorId, "B", 7, 50);
        UUID formula = fixtures.posturaPlus();
        for (LocalDate date : List.of(today, today.minusDays(1))) {
            UUID report = fixtures.report(sectorId, date, 98, true);
            fixtures.cageIn(report, a01, 48);
            fixtures.cageIn(report, b07, 50);
            boolean isToday = date.equals(today);
            fixtures.production(report, a01, isToday ? 44 : 40, 1, 2, 1, 1, 0, 0);
            fixtures.production(report, b07, isToday ? 45 : 40, 0, 1, 1, 2, 1, 0);
            fixtures.feed(report, a01, formula, 1344);
            fixtures.feed(report, b07, formula, 1400);
        }
        return sectorId;
    }

    // ---------------------------------------------------------------- GET /api/v1/dashboard

    @Test
    @DisplayName("gives the overview: the day of the farm, the part of the day, the counts and the tabs")
    void givenSectorWithReports_whenReadingTheOverview_thenAnswerTheFieldsOfTheContract() throws Exception {
        // given
        UUID sectorId = sectorWithTwoDays();

        // when
        ResultActions response = read("/api/v1/dashboard");

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.today").value(today.toString()))
                .andExpect(jsonPath("$.partOfDay").value(in(List.of("MORNING", "AFTERNOON", "EVENING"))))
                .andExpect(jsonPath("$.activeSectors").isNumber())
                .andExpect(jsonPath("$.completeToday").isNumber())
                .andExpect(jsonPath("$.sectors[*].id").value(hasItem(sectorId.toString())))
                .andExpect(jsonPath("$.sectors[*].name").value(hasItem(fixtures.nameOf(sectorId))));
    }

    @Test
    @DisplayName("refuses a format other than JSON on the overview: 406")
    void givenPlainTextAccepted_whenReadingTheOverview_thenAnswerNotAcceptable() throws Exception {
        // given
        String path = "/api/v1/dashboard";

        // when
        ResultActions response =
                mockMvc.perform(get(path).cookie(commonUser.cookies()).accept(MediaType.TEXT_PLAIN));

        // then
        response.andExpect(status().isNotAcceptable());
    }

    // ---------------------------------------------------------------- GET /api/v1/sectors/{id}/dashboard

    @Test
    @DisplayName("gives the dashboard of today, with the indicators in the scales of the contract")
    void givenReportsOfTodayAndYesterday_whenReadingToday_thenAnswerTheIndicatorsAndTheTrend() throws Exception {
        // given
        UUID sectorId = sectorWithTwoDays();

        // when
        ResultActions response = read(dashboardOf(sectorId) + "?period=TODAY");

        // then
        response.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sector.id").value(sectorId.toString()))
                .andExpect(jsonPath("$.sector.status").value("ACTIVE"))
                .andExpect(jsonPath("$.period").value("TODAY"))
                .andExpect(jsonPath("$.from").value(today.toString()))
                .andExpect(jsonPath("$.to").value(today.toString()))
                .andExpect(jsonPath("$.todayReport.productionStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.todayReport.feedStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.todayReport.mortalityStatus").value("RECORDED"))
                .andExpect(jsonPath("$.indicators.production.value").value(89))
                .andExpect(jsonPath("$.indicators.production.previous").value(80))
                .andExpect(jsonPath("$.indicators.production.change").value(11.3))
                .andExpect(jsonPath("$.indicators.production.goodDirection").value("UP"))
                .andExpect(jsonPath("$.indicators.production.incompleteDays").value(0))
                .andExpect(jsonPath("$.indicators.layingRate.value").value(90.82))
                .andExpect(jsonPath("$.indicators.layingRate.previous").value(81.63))
                .andExpect(jsonPath("$.indicators.layingRate.change").value(9.19))
                .andExpect(jsonPath("$.indicators.feedCost.value").value(7.82))
                .andExpect(jsonPath("$.indicators.feedCost.goodDirection").value("DOWN"))
                .andExpect(jsonPath("$.indicators.costPerEgg.value").value(0.088))
                .andExpect(jsonPath("$.indicators.costPerEgg.previous").value(0.098))
                .andExpect(jsonPath("$.trend", hasSize(7)))
                .andExpect(jsonPath("$.trend[6].date").value(today.toString()))
                .andExpect(jsonPath("$.trend[6].production").value(89))
                .andExpect(jsonPath("$.trend[6].costPerEgg").value(0.088))
                .andExpect(jsonPath("$.trend[0].production").doesNotExist());
    }

    @Test
    @DisplayName("gives the target of productivity and the grading of the eggs of the period (US2)")
    void givenGradedReports_whenReadingToday_thenAnswerTheTargetAndTheGrading() throws Exception {
        // given
        UUID sectorId = sectorWithTwoDays();

        // when
        ResultActions response = read(dashboardOf(sectorId) + "?period=TODAY");

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.target").value(85.00))
                .andExpect(jsonPath("$.targetStatus").value("ABOVE"))
                .andExpect(jsonPath("$.grades.collected").value(89))
                .andExpect(jsonPath("$.grades.standard.grade").value("standard"))
                .andExpect(jsonPath("$.grades.standard.count").value(79))
                .andExpect(jsonPath("$.grades.standard.percent").value(88.8))
                .andExpect(jsonPath("$.grades.shares[*].grade")
                        .value(org.hamcrest.Matchers.contains("small", "jumbo", "dirty", "cracked", "bloodSpot", "abnormal")))
                .andExpect(jsonPath("$.grades.shares[3].count").value(3))
                .andExpect(jsonPath("$.grades.shares[3].percent").value(3.4));
    }

    @Test
    @DisplayName("gives the pending entries of today as alerts, with the report as the target (US3)")
    void givenFeedPendingToday_whenReadingTheDashboard_thenAnswerTheAlert() throws Exception {
        // given
        UUID sectorId = fixtures.activeSector();
        UUID a01 = fixtures.activeCage(sectorId, "A", 1, 48);
        UUID b07 = fixtures.activeCage(sectorId, "B", 7, 50);
        UUID formula = fixtures.posturaPlus();
        UUID report = fixtures.report(sectorId, today, 98, true);
        fixtures.cageIn(report, a01, 48);
        fixtures.cageIn(report, b07, 50);
        fixtures.production(report, a01, 44, 0, 0, 0, 0, 0, 0);
        fixtures.production(report, b07, 45, 0, 0, 0, 0, 0, 0);
        fixtures.feed(report, a01, formula, 1344);

        // when
        ResultActions response = read(dashboardOf(sectorId));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.openAlerts").value(1))
                .andExpect(jsonPath("$.alerts[0].kind").value("FEED_PENDING"))
                .andExpect(jsonPath("$.alerts[0].tone").value("INFO"))
                .andExpect(jsonPath("$.alerts[0].title").value("Ração pendente no relatório de hoje"))
                .andExpect(jsonPath("$.alerts[0].detail").value("Lance a ração de 1 gaiola para calcular o custo por ovo."))
                .andExpect(jsonPath("$.alerts[0].target.reportId").value(report.toString()))
                .andExpect(jsonPath("$.alerts[0].target.cageId").doesNotExist());
    }

    @Test
    @DisplayName("gives an empty list of alerts, and not nothing, when nothing stands out (US3)")
    void givenCompleteReports_whenReadingTheDashboard_thenAnswerNoAlert() throws Exception {
        // given
        UUID sectorId = sectorWithTwoDays();

        // when
        ResultActions response = read(dashboardOf(sectorId));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.openAlerts").value(0))
                .andExpect(jsonPath("$.alerts", hasSize(0)));
    }

    @Test
    @DisplayName("gives no value, and not zero, to a day without a report, and reads today when no period is asked")
    void givenNoReportToday_whenReadingWithoutPeriod_thenAnswerTodayWithoutValues() throws Exception {
        // given
        UUID sectorId = fixtures.activeSector();
        UUID a01 = fixtures.activeCage(sectorId, "A", 1, 48);
        UUID report = fixtures.report(sectorId, today.minusDays(1), 48, true);
        fixtures.cageIn(report, a01, 48);
        fixtures.production(report, a01, 44, 0, 0, 0, 0, 0, 0);

        // when
        ResultActions response = read(dashboardOf(sectorId));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("TODAY"))
                .andExpect(jsonPath("$.todayReport").doesNotExist())
                .andExpect(jsonPath("$.indicators.production.value").doesNotExist())
                .andExpect(jsonPath("$.indicators.production.previous").value(44))
                .andExpect(jsonPath("$.indicators.production.change").doesNotExist())
                .andExpect(jsonPath("$.indicators.feedCost.previous").doesNotExist())
                .andExpect(jsonPath("$.indicators.feedCost.incompleteDays").value(0));
    }

    @Test
    @DisplayName("gives the 7 days up to today against the 7 before")
    void givenReports_whenReadingSevenDays_thenAnswerTheWindow() throws Exception {
        // given
        UUID sectorId = sectorWithTwoDays();

        // when
        ResultActions response = read(dashboardOf(sectorId) + "?period=LAST_7_DAYS");

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("LAST_7_DAYS"))
                .andExpect(jsonPath("$.from").value(today.minusDays(6).toString()))
                .andExpect(jsonPath("$.to").value(today.toString()))
                .andExpect(jsonPath("$.indicators.production.value").value(169))
                .andExpect(jsonPath("$.indicators.production.previous").doesNotExist());
    }

    @Test
    @DisplayName("refuses a period that does not exist: 400 with the parameter")
    void givenUnknownPeriod_whenReading_thenAnswerValidationFailedWithTheParameter() throws Exception {
        // given
        UUID sectorId = fixtures.activeSector();

        // when
        ResultActions response = read(dashboardOf(sectorId) + "?period=SEMANA");

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("Valor inválido para o parâmetro 'period'."))
                .andExpect(jsonPath("$.details.parameter").value("period"));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"4e6a8c0e-2b4d-4f6a-8c0e-2b4d6f8a0c44", "galpao-9"})
    @DisplayName("refuses a sector that does not exist or is malformed: 404")
    void givenUnknownSector_whenReading_thenAnswerSectorNotFound(String sectorId) throws Exception {
        // given
        String path = dashboardOf(sectorId);

        // when
        ResultActions response = read(path);

        // then
        response.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("SECTOR_NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value(path));
    }

    @Test
    @DisplayName("refuses a format other than JSON on the dashboard of a sector: 406")
    void givenPlainTextAccepted_whenReadingTheDashboard_thenAnswerNotAcceptable() throws Exception {
        // given
        UUID sectorId = fixtures.activeSector();

        // when
        ResultActions response = mockMvc.perform(
                get(dashboardOf(sectorId)).cookie(commonUser.cookies()).accept(MediaType.TEXT_PLAIN));

        // then
        response.andExpect(status().isNotAcceptable());
    }

    @Test
    @DisplayName("gives the latest reports of the sector, newest first (US4)")
    void givenReportsOfTodayAndYesterday_whenReadingTheDashboard_thenAnswerTheLatestReports() throws Exception {
        // given
        UUID sectorId = sectorWithTwoDays();

        // when
        ResultActions response = read(dashboardOf(sectorId) + "?period=LAST_7_DAYS");

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.latestReports", hasSize(2)))
                .andExpect(jsonPath("$.latestReports[0].collectionDate").value(today.toString()))
                .andExpect(jsonPath("$.latestReports[0].collectionTime").value("06:30"))
                .andExpect(jsonPath("$.latestReports[0].openedBy.name").value("Marina Alves"))
                .andExpect(jsonPath("$.latestReports[0].collectedEggs").value(89))
                .andExpect(jsonPath("$.latestReports[0].removedBirds").value(0))
                .andExpect(jsonPath("$.latestReports[0].productionStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.latestReports[0].feedStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.latestReports[0].mortalityStatus").value("RECORDED"))
                .andExpect(jsonPath("$.latestReports[1].collectionDate").value(today.minusDays(1).toString()));
    }

    // ---------------------------------------------------------------- meta do setor (008)

    /**
     * Um setor com a A-01 (100 aves) e os relatórios de hoje - 2 até hoje, com 75, 74 e 76 ovos: a gaiola fica
     * em 75,0% nos últimos 3 relatórios, e o setor em 76,0% hoje.
     */
    private UUID sectorAt75Percent(String target) {
        UUID sectorId = fixtures.activeSector();
        fixtures.layingRateTarget(sectorId, target);
        UUID a01 = fixtures.activeCage(sectorId, "A", 1, 100);
        int[] eggs = {75, 74, 76};
        for (int back = 2; back >= 0; back--) {
            UUID report = fixtures.report(sectorId, today.minusDays(back), 100, true);
            fixtures.cageIn(report, a01, 100);
            fixtures.production(report, a01, eggs[2 - back], 0, 0, 0, 0, 0, 0);
        }
        return sectorId;
    }

    @Test
    @DisplayName("uses the target of each sector in the chart and in the low laying alert (008)")
    void givenTwoSectorsWithTheSameReportsAndDifferentTargets_whenReadingToday_thenUseTheTargetOfEach()
            throws Exception {
        // given
        UUID at72 = sectorAt75Percent("72");
        UUID at85 = sectorAt75Percent("85");

        // when
        ResultActions low = read(dashboardOf(at72) + "?period=TODAY");
        ResultActions high = read(dashboardOf(at85) + "?period=TODAY");

        // then
        low.andExpect(status().isOk())
                .andExpect(jsonPath("$.target").value(72.00))
                .andExpect(jsonPath("$.targetStatus").value("ABOVE"))
                .andExpect(jsonPath("$.alerts[?(@.kind == 'LOW_LAYING')]").isEmpty());
        high.andExpect(status().isOk())
                .andExpect(jsonPath("$.target").value(85.00))
                .andExpect(jsonPath("$.targetStatus").value("BELOW"))
                .andExpect(jsonPath("$.alerts[?(@.kind == 'LOW_LAYING')].detail")
                        .value("75,0% nos últimos 3 relatórios, abaixo da meta de 85% do setor; o setor fez 76,0% hoje."));
    }
}
