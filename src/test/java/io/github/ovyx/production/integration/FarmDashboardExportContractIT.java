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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Contrato da exportação da granja toda, contra a aplicação em execução e PostgreSQL real (US4 da 009; S-07;
 * {@code contracts/production-api.yaml}): o arquivo com as quatro abas, e os setores iguais aos da consulta.
 */
@AutoConfigureMockMvc
@DisplayName("Farm dashboard export contract")
class FarmDashboardExportContractIT extends IntegrationTestSupport {

    private static final String EXPORT = "/api/v1/dashboard/farm/export";
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
    @DisplayName("answers the spreadsheet of the farm with the type, the name to save and the four sheets")
    void givenSectorsWithReports_whenExporting_thenAnswerTheSpreadsheetWithTheFourSheets() throws Exception {
        // given
        sectorWithTodayReport();
        String day = today.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        // when
        ResultActions response = perform(get(EXPORT).queryParam("period", "LAST_7_DAYS"));

        // then
        byte[] file = response.andExpect(status().isOk())
                .andExpect(content().contentType(SPREADSHEET))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"painel-granja-" + day + ".xlsx\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        assertThat(sheetNames(file)).containsExactly("Indicadores", "7 dias", "Classificação", "Setores");
    }

    @Test
    @DisplayName("writes in the sheet Setores the same numbers of the dashboard of the farm")
    void givenASectorWithTheReportOfToday_whenExporting_thenWriteItsRowAsTheDashboardReadsIt() throws Exception {
        // given
        UUID sectorId = sectorWithTodayReport();
        String name = fixtures.nameOf(sectorId);
        String farm = perform(get("/api/v1/dashboard/farm").queryParam("period", "TODAY"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String row = "$.sectors[?(@.sector.id == '%s')]".formatted(sectorId);
        List<Object> production = JsonPath.read(farm, row + ".production");
        List<Object> openAlerts = JsonPath.read(farm, row + ".openAlerts");

        // when
        byte[] file = perform(get(EXPORT).queryParam("period", "TODAY"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // then
        Row exported = rowsOf(file, "Setores").stream()
                .filter(candidate -> name.equals(candidate.getCellText(0)))
                .findFirst()
                .orElseThrow();
        assertThat(exported.getCellAsNumber(1)).hasValueSatisfying(value -> assertThat(value)
                .isEqualByComparingTo(new BigDecimal(production.getFirst().toString())));
        assertThat(exported.getCellText(4)).isEqualTo("Acima da meta");
        assertThat(exported.getCellAsNumber(7)).hasValueSatisfying(value -> assertThat(value)
                .isEqualByComparingTo(new BigDecimal(openAlerts.getFirst().toString())));
    }

    @Test
    @DisplayName("refuses a period that does not exist, naming the parameter, as Problem Details")
    void givenUnknownPeriod_whenExporting_thenAnswerBadRequestNamingTheParameter() throws Exception {
        // given
        String period = "MES";

        // when
        ResultActions response = perform(get(EXPORT).queryParam("period", period));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.parameter").value("period"));
    }
}
