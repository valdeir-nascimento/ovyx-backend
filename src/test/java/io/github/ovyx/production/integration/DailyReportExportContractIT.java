package io.github.ovyx.production.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ovyx.IntegrationSessions;
import io.github.ovyx.IntegrationSessions.SignedIn;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.spreadsheet.FileNames;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
 * Contrato da exportação dos relatórios de um intervalo, contra a aplicação em execução e PostgreSQL real (US1 da
 * 007; S-01 a S-04; {@code contracts/production-api.yaml}): o arquivo com os três cabeçalhos, lido de volta, e as
 * recusas em {@code application/problem+json}, e não como planilha.
 */
@AutoConfigureMockMvc
@DisplayName("Daily report export contract")
class DailyReportExportContractIT extends IntegrationTestSupport {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String SPREADSHEET = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final LocalDate FIRST = LocalDate.of(2026, 9, 1);
    private static final LocalDate SECOND = LocalDate.of(2026, 9, 2);

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

    private SignedIn commonUser;
    private ProductionFixtures fixtures;

    @BeforeEach
    void setUp() throws Exception {
        commonUser = new IntegrationSessions(mockMvc, caretakerRepository, passwordHasher, clock).commonUser();
        fixtures = new ProductionFixtures(jdbc);
    }

    private static String exportOf(Object sectorId) {
        return "/api/v1/sectors/" + sectorId + "/daily-reports/export";
    }

    private ResultActions export(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.cookie(commonUser.cookies()).accept(MediaType.ALL));
    }

    /**
     * Um setor com a A-01 (48 aves) e a B-07 (50 aves). Em 01/09, tudo lançado com a Postura Plus: 44 + 45 ovos e
     * 1.344 + 1.400 g de ração. Em 02/09, só a produção da A-01 (40 ovos), sem ração. Em 03/09, fora do intervalo,
     * outro relatório.
     */
    private UUID sectorWithTwoReports() {
        UUID sectorId = fixtures.activeSector();
        UUID a01 = fixtures.activeCage(sectorId, "A", 1, 48);
        UUID b07 = fixtures.activeCage(sectorId, "B", 7, 50);
        UUID formula = fixtures.posturaPlus();
        UUID first = fixtures.report(sectorId, FIRST, 98, true);
        fixtures.cageIn(first, a01, 48);
        fixtures.cageIn(first, b07, 50);
        fixtures.production(first, a01, 44, 1, 2, 1, 1, 0, 0);
        fixtures.production(first, b07, 45, 0, 1, 1, 2, 1, 0);
        fixtures.feed(first, a01, formula, 1344);
        fixtures.feed(first, b07, formula, 1400);
        UUID second = fixtures.report(sectorId, SECOND, 98, true);
        fixtures.cageIn(second, a01, 48);
        fixtures.cageIn(second, b07, 50);
        fixtures.production(second, a01, 40, 0, 0, 0, 0, 0, 0);
        UUID outside = fixtures.report(sectorId, SECOND.plusDays(1), 98, true);
        fixtures.cageIn(outside, a01, 48);
        return sectorId;
    }

    /** As abas da planilha, cada uma com as linhas pelo índice a partir de zero. */
    private static Map<String, List<Row>> sheetsOf(byte[] file) throws Exception {
        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(file))) {
            return workbook.getSheets().collect(Collectors.toMap(Sheet::getName, sheet -> {
                try {
                    return sheet.read();
                } catch (java.io.IOException exception) {
                    throw new java.io.UncheckedIOException(exception);
                }
            }));
        }
    }

    // ---------------------------------------------------------------- o arquivo

    @Test
    @DisplayName("answers the spreadsheet of the interval with the type, the name to save and no cache")
    void givenReportsInTheInterval_whenExporting_thenAnswerTheSpreadsheetWithTheThreeHeaders() throws Exception {
        // given
        UUID sectorId = sectorWithTwoReports();
        String slug = FileNames.slug(fixtures.nameOf(sectorId));

        // when
        ResultActions response =
                export(get(exportOf(sectorId)).queryParam("from", "2026-09-01").queryParam("to", "2026-09-02"));

        // then
        response.andExpect(status().isOk())
                .andExpect(content().contentType(SPREADSHEET))
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"relatorios-" + slug + "-01-09-2026-a-02-09-2026.xlsx\""))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    @DisplayName("writes the two reports of the interval, the totals and every cage of each one")
    void givenReportsInTheInterval_whenExporting_thenWriteTheReportsTheTotalsAndTheCages() throws Exception {
        // given
        UUID sectorId = sectorWithTwoReports();

        // when
        byte[] file = export(get(exportOf(sectorId)).queryParam("from", "2026-09-01").queryParam("to", "2026-09-02"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // then
        Map<String, List<Row>> sheets = sheetsOf(file);
        assertThat(sheets).containsOnlyKeys("Relatórios", "Gaiolas");
        List<Row> reports = sheets.get("Relatórios");
        // 4 linhas de cabeçalho, 1 em branco (que o leitor pula), os títulos, 2 relatórios e os totais.
        assertThat(reports).hasSize(4 + 1 + 2 + 1);
        Row titles = reports.get(4);
        assertThat(titles.getCellText(0)).isEqualTo("Data");
        assertThat(reports.get(5).getCellAsNumber(5)).hasValueSatisfying(eggs -> assertThat(eggs).isEqualByComparingTo("89"));
        assertThat(reports.get(6).getCellAsNumber(5)).hasValueSatisfying(eggs -> assertThat(eggs).isEqualByComparingTo("40"));
        Row totals = reports.get(7);
        assertThat(totals.getCellText(0)).isEqualTo("Total");
        assertThat(totals.getCellAsNumber(5)).hasValueSatisfying(eggs -> assertThat(eggs).isEqualByComparingTo("129"));
        assertThat(totals.getCellText(22)).isEqualTo("1 dia fora do custo");
        // 4 linhas de cabeçalho, os títulos e 2 gaiolas em cada um dos 2 relatórios.
        assertThat(sheets.get("Gaiolas")).hasSize(4 + 1 + 4);
    }

    @Test
    @DisplayName("writes the notice of no report for an interval without reports")
    void givenIntervalWithoutReports_whenExporting_thenWriteTheNotice() throws Exception {
        // given
        UUID sectorId = sectorWithTwoReports();

        // when
        byte[] file = export(get(exportOf(sectorId)).queryParam("from", "2026-08-01").queryParam("to", "2026-08-31"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // then
        List<Row> reports = sheetsOf(file).get("Relatórios");
        assertThat(reports.getLast().getCellText(0)).isEqualTo("Nenhum relatório de 01/08/2026 a 31/08/2026");
    }

    // ---------------------------------------------------------------- as recusas

    @Test
    @DisplayName("refuses without the dates, each one at once, as Problem Details and not as a spreadsheet")
    void givenNoDates_whenExporting_thenAnswerBadRequestWithBothDates() throws Exception {
        // given
        UUID sectorId = sectorWithTwoReports();

        // when
        ResultActions response = export(get(exportOf(sectorId)));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.from").value("Informe a data inicial."))
                .andExpect(jsonPath("$.details.to").value("Informe a data final."));
    }

    @Test
    @DisplayName("refuses the last day before the first one")
    void givenLastDayBeforeTheFirst_whenExporting_thenAnswerBadRequestOnTheLastDay() throws Exception {
        // given
        UUID sectorId = sectorWithTwoReports();

        // when
        ResultActions response =
                export(get(exportOf(sectorId)).queryParam("from", "2026-09-28").queryParam("to", "2026-09-01"));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.to").value("A data final deve ser igual ou posterior à inicial."));
    }

    @Test
    @DisplayName("refuses a date out of the format YYYY-MM-DD, naming the parameter")
    void givenDateOutOfTheFormat_whenExporting_thenAnswerBadRequestNamingTheParameter() throws Exception {
        // given
        UUID sectorId = sectorWithTwoReports();

        // when
        ResultActions response =
                export(get(exportOf(sectorId)).queryParam("from", "2026-13-01").queryParam("to", "2026-09-28"));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.parameter").value("from"));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11", "galpao-9"})
    @DisplayName("answers not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenExporting_thenAnswerNotFound(String sector) throws Exception {
        // given
        String path = exportOf(sector);

        // when
        ResultActions response = export(get(path).queryParam("from", "2026-09-01").queryParam("to", "2026-09-28"));

        // then
        response.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("SECTOR_NOT_FOUND"));
    }
}
