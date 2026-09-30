package io.github.ovyx.production.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.spreadsheet.FileNames;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Contrato da exportação do painel, contra a aplicação em execução e PostgreSQL real (US2 da 007; S-05;
 * {@code contracts/production-api.yaml}): o arquivo com as cinco abas, e os indicadores iguais aos da consulta do
 * painel do mesmo setor e período.
 */
@AutoConfigureMockMvc
@DisplayName("Dashboard export contract")
class DashboardExportContractIT extends IntegrationTestSupport {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String SPREADSHEET = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

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

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.cookie(commonUser.cookies()).accept(MediaType.ALL));
    }

    /** O setor da consulta do painel: a A-01 e a B-07, com os relatórios completos de hoje e de ontem. */
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

    private static List<String> sheetNames(byte[] file) throws Exception {
        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(file))) {
            return workbook.getSheets().map(Sheet::getName).toList();
        }
    }

    private static List<Row> rowsOf(byte[] file, String name) throws Exception {
        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(file))) {
            return workbook.findSheet(name).orElseThrow().read();
        }
    }

    @Test
    @DisplayName("answers the spreadsheet of the dashboard with the type, the name to save and the five sheets")
    void givenSectorWithReports_whenExporting_thenAnswerTheSpreadsheetWithTheFiveSheets() throws Exception {
        // given
        UUID sectorId = sectorWithTwoDays();
        String day = today.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        // when
        ResultActions response = perform(
                get("/api/v1/sectors/" + sectorId + "/dashboard/export").queryParam("period", "LAST_7_DAYS"));

        // then
        byte[] file = response.andExpect(status().isOk())
                .andExpect(content().contentType(SPREADSHEET))
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"painel-" + FileNames.slug(fixtures.nameOf(sectorId)) + "-" + day + ".xlsx\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        assertThat(sheetNames(file))
                .containsExactly("Indicadores", "7 dias", "Classificação", "Alertas", "Últimos relatórios");
    }

    @Test
    @DisplayName("writes the same indicators the dashboard answers for the same sector and period")
    void givenSectorWithReports_whenExporting_thenWriteTheIndicatorsOfTheDashboard() throws Exception {
        // given
        UUID sectorId = sectorWithTwoDays();
        String dashboard = perform(get("/api/v1/sectors/" + sectorId + "/dashboard").queryParam("period", "TODAY"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // when
        byte[] file = perform(get("/api/v1/sectors/" + sectorId + "/dashboard/export").queryParam("period", "TODAY"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // then
        List<Row> indicators = rowsOf(file, "Indicadores");
        // 4 linhas de cabeçalho, 1 em branco (que o leitor pula), os títulos e os 4 indicadores.
        assertThat(indicators).hasSize(4 + 1 + 4);
        Row production = indicators.get(5);
        assertThat(production.getCellText(0)).isEqualTo("Produção");
        assertThat(production.getCellAsNumber(1)).hasValueSatisfying(value -> assertThat(value)
                .isEqualByComparingTo(new BigDecimal(JsonPath.read(dashboard, "$.indicators.production.value").toString())));
        Row cost = indicators.get(7);
        assertThat(cost.getCellText(0)).isEqualTo("Custo de ração");
        assertThat(cost.getCellAsNumber(1)).hasValueSatisfying(value -> assertThat(value)
                .isEqualByComparingTo(new BigDecimal(JsonPath.read(dashboard, "$.indicators.feedCost.value").toString())));
    }

    @Test
    @DisplayName("refuses a period that does not exist, naming the parameter, as Problem Details")
    void givenUnknownPeriod_whenExporting_thenAnswerBadRequestNamingTheParameter() throws Exception {
        // given
        UUID sectorId = sectorWithTwoDays();

        // when
        ResultActions response =
                perform(get("/api/v1/sectors/" + sectorId + "/dashboard/export").queryParam("period", "SEMANA"));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.parameter").value("period"));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11", "galpao-9"})
    @DisplayName("answers not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenExporting_thenAnswerNotFound(String sector) throws Exception {
        // given
        String path = "/api/v1/sectors/" + sector + "/dashboard/export";

        // when
        ResultActions response = perform(get(path));

        // then
        response.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("SECTOR_NOT_FOUND"));
    }
}
