package io.github.ovyx.farm.integration;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aUniqueSector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ovyx.IntegrationSessions;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorTestDataBuilder;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.spreadsheet.FileNames;
import io.github.ovyx.shared.domain.FixedClock;
import jakarta.servlet.http.Cookie;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.util.List;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
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
 * Contrato da exportação das gaiolas, contra a aplicação em execução e PostgreSQL real (US3 da 007; S-06;
 * {@code contracts/farm-api.yaml}): o arquivo com os três cabeçalhos, todas as gaiolas dos filtros, e as recusas
 * em {@code application/problem+json}.
 */
@AutoConfigureMockMvc
@DisplayName("Cage export contract")
class CageExportContractIT extends IntegrationTestSupport {

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
    private SectorRepository repository;

    private Cookie[] commonUser;

    @BeforeEach
    void setUp() throws Exception {
        commonUser = new IntegrationSessions(mockMvc, caretakerRepository, passwordHasher, clock).commonUser().cookies();
    }

    /** Um setor com a faixa de 155 a 175 g, 30 gaiolas ativas na bateria A e a B-07 inativa. */
    private Sector sector() {
        SectorTestDataBuilder builder =
                aUniqueSector().withReferenceWeight(155, 175).withInactiveCage("B", 7, 30);
        for (int number = 1; number <= 30; number++) {
            builder.withCage("A", number, 50);
        }
        Sector sector = builder.withClock(FixedClock.at("2026-09-25T13:10:42Z")).build();
        repository.save(sector);
        return sector;
    }

    private ResultActions export(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.cookie(commonUser).accept(MediaType.ALL));
    }

    private static List<Row> rowsOf(byte[] file) throws Exception {
        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(file))) {
            return workbook.findSheet("Gaiolas").orElseThrow().read();
        }
    }

    @Test
    @DisplayName("answers the spreadsheet of every active cage, beyond one page, with the three headers")
    void givenSectorWithCages_whenExporting_thenAnswerEveryActiveCage() throws Exception {
        // given
        Sector sector = sector();

        // when
        ResultActions response = export(get("/api/v1/sectors/" + sector.id() + "/cages/export"));

        // then
        byte[] file = response.andExpect(status().isOk())
                .andExpect(content().contentType(SPREADSHEET))
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"gaiolas-" + FileNames.slug(sector.name().value()) + ".xlsx\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        List<Row> rows = rowsOf(file);
        // 6 linhas de cabeçalho, 1 em branco (que o leitor pula), os títulos e as 30 gaiolas ativas.
        assertThat(rows).hasSize(6 + 1 + 30);
        assertThat(rows.get(3).getCellText(0)).isEqualTo("Faixa de peso: 155 a 175 g");
        assertThat(rows.get(7).getCellText(0)).isEqualTo("A-01");
    }

    @Test
    @DisplayName("keeps the filters of the list: the battery and the status")
    void givenFilters_whenExporting_thenAnswerOnlyTheCagesOfTheFilters() throws Exception {
        // given
        Sector sector = sector();

        // when
        byte[] file = export(get("/api/v1/sectors/" + sector.id() + "/cages/export")
                        .queryParam("battery", "b")
                        .queryParam("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // then
        List<Row> rows = rowsOf(file);
        assertThat(rows.get(2).getCellText(0)).isEqualTo("Filtros: bateria B, só as inativas");
        assertThat(rows.getLast().getCellText(0)).isEqualTo("B-07");
        assertThat(rows.getLast().getCellText(4)).isEqualTo("Inativa");
    }

    @Test
    @DisplayName("refuses a status that does not exist, naming the parameter, as Problem Details")
    void givenUnknownStatus_whenExporting_thenAnswerBadRequestNamingTheParameter() throws Exception {
        // given
        Sector sector = sector();

        // when
        ResultActions response =
                export(get("/api/v1/sectors/" + sector.id() + "/cages/export").queryParam("status", "QUASE"));

        // then
        response.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.parameter").value("status"));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11", "galpao-9"})
    @DisplayName("answers not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenExporting_thenAnswerNotFound(String sector) throws Exception {
        // given
        String path = "/api/v1/sectors/" + sector + "/cages/export";

        // when
        ResultActions response = export(get(path));

        // then
        response.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("SECTOR_NOT_FOUND"));
    }
}
