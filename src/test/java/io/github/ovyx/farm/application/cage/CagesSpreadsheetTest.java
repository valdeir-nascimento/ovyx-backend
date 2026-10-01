package io.github.ovyx.farm.application.cage;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.application.sector.SectorDetail;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.domain.valueobject.ReferenceWeight;
import io.github.ovyx.shared.application.spreadsheet.Cell;
import io.github.ovyx.shared.application.spreadsheet.CellFormat;
import io.github.ovyx.shared.application.spreadsheet.Column;
import io.github.ovyx.shared.application.spreadsheet.Sheet;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A planilha das gaiolas (US3 da 007; data-model §4), montada sem banco: o cabeçalho com os filtros, a faixa e
 * os totais do setor, e uma linha por gaiola com a última pesagem diante da faixa.
 */
@DisplayName("CagesSpreadsheet")
class CagesSpreadsheetTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);
    private static final LocalTime NOW = LocalTime.of(21, 40);
    private static final SectorId SECTOR_ID = SectorId.generate();

    private static SectorDetail sector(ReferenceWeight range) {
        return new SectorDetail(
                SECTOR_ID,
                "Codornas — Galpão 1",
                null,
                Status.ACTIVE,
                118,
                5900,
                List.of("A", "B"),
                Instant.parse("2026-09-01T10:00:00Z"),
                Instant.parse("2026-09-01T10:00:00Z"),
                range, new BigDecimal("85.0"));
    }

    private static CageSummary cage(String battery, int number, Status status, String weight, LocalDate weighedOn) {
        return new CageSummary(
                CageId.generate(),
                SECTOR_ID,
                battery + "-" + String.format("%02d", number),
                battery,
                number,
                50,
                status,
                weight == null ? null : new CageLastWeighing(weighedOn, new BigDecimal(weight)));
    }

    private static List<CageSummary> cages() {
        return List.of(
                cage("A", 1, Status.ACTIVE, "161.4", TODAY.minusDays(4)),
                cage("A", 2, Status.ACTIVE, "150.8", TODAY),
                cage("C", 3, Status.ACTIVE, null, null),
                cage("D", 1, Status.INACTIVE, "175.0", TODAY.minusDays(10)));
    }

    private static Spreadsheet spreadsheet(ReferenceWeight range, String code, String battery, StatusFilter status, List<CageSummary> cages) {
        return CagesSpreadsheet.of(sector(range), code, battery, status, cages, TODAY, NOW);
    }

    private static Cell cell(Sheet sheet, List<Cell> row, String title) {
        List<String> titles = sheet.columns().stream().map(Column::title).toList();
        assertThat(titles).contains(title);
        return row.get(titles.indexOf(title));
    }

    private static void assertNumber(Cell cell, String value, CellFormat format) {
        assertThat(cell).isInstanceOf(Cell.Number.class);
        assertThat(((Cell.Number) cell).value()).isEqualByComparingTo(value);
        assertThat(((Cell.Number) cell).format()).isEqualTo(format);
    }

    @Test
    @DisplayName("names the file after the sector, with one sheet of cages")
    void givenCages_whenBuildingTheSpreadsheet_thenNameTheFileAfterTheSector() {
        // given
        List<CageSummary> cages = cages();

        // when
        Spreadsheet spreadsheet = spreadsheet(new ReferenceWeight(155, 175), null, null, StatusFilter.ALL, cages);

        // then
        assertThat(spreadsheet.fileName()).isEqualTo("gaiolas-codornas-galpao-1.xlsx");
        assertThat(spreadsheet.sheets()).extracting(Sheet::name).containsExactly("Gaiolas");
    }

    @Test
    @DisplayName("heads the sheet with the filters, the range and the totals of the sector")
    void givenFilters_whenBuildingTheSpreadsheet_thenSayThemInTheHeading() {
        // given
        List<CageSummary> cages = cages();

        // when
        Spreadsheet spreadsheet = spreadsheet(new ReferenceWeight(155, 175), "B-0", "B", StatusFilter.ACTIVE, cages);

        // then
        assertThat(spreadsheet.sheets().getFirst().heading())
                .containsExactly(
                        "Ovyx — Gaiolas",
                        "Setor: Codornas — Galpão 1",
                        "Filtros: busca \"B-0\", bateria B, só as ativas",
                        "Faixa de peso: 155 a 175 g",
                        "Gaiolas ativas: 118. Aves: 5.900",
                        "Gerada em 28/09/2026 às 21:40 (horário da granja)");
    }

    @Test
    @DisplayName("says no filter but the status, and the sector without range")
    void givenNoFilterAndNoRange_whenBuildingTheSpreadsheet_thenSayNoneAndNoRange() {
        // given
        List<CageSummary> cages = cages();

        // when
        List<String> heading = spreadsheet(null, null, null, StatusFilter.ACTIVE, cages).sheets().getFirst().heading();
        List<String> all = spreadsheet(null, null, null, StatusFilter.ALL, cages).sheets().getFirst().heading();
        List<String> inactive = spreadsheet(null, null, null, StatusFilter.INACTIVE, cages).sheets().getFirst().heading();

        // then
        assertThat(heading).contains("Filtros: nenhum, só as ativas", "Faixa de peso: sem faixa definida");
        assertThat(all).contains("Filtros: nenhum, todas as situações");
        assertThat(inactive).contains("Filtros: nenhum, só as inativas");
    }

    @Test
    @DisplayName("writes one row per cage, with the last weighing and where it stands in the range")
    void givenCages_whenBuildingTheSpreadsheet_thenWriteEachCage() {
        // given
        List<CageSummary> cages = cages();

        // when
        Sheet sheet = spreadsheet(new ReferenceWeight(155, 175), null, null, StatusFilter.ALL, cages).sheets().getFirst();

        // then
        assertThat(sheet.columns())
                .extracting(Column::title)
                .containsExactly("Gaiola", "Bateria", "Número", "Aves", "Situação", "Peso médio (g)", "Data da pesagem", "Faixa");
        assertThat(sheet.rows()).hasSize(4);
        List<Cell> a01 = sheet.rows().getFirst();
        assertThat(cell(sheet, a01, "Gaiola")).isEqualTo(Cell.text("A-01"));
        assertThat(cell(sheet, a01, "Bateria")).isEqualTo(Cell.text("A"));
        assertNumber(cell(sheet, a01, "Número"), "1", CellFormat.COUNT);
        assertNumber(cell(sheet, a01, "Aves"), "50", CellFormat.COUNT);
        assertThat(cell(sheet, a01, "Situação")).isEqualTo(Cell.text("Ativa"));
        assertNumber(cell(sheet, a01, "Peso médio (g)"), "161.4", CellFormat.GRAMS);
        assertThat(cell(sheet, a01, "Data da pesagem")).isEqualTo(Cell.date(TODAY.minusDays(4)));
        assertThat(cell(sheet, a01, "Faixa")).isEqualTo(Cell.text("Dentro da faixa"));
        assertThat(cell(sheet, sheet.rows().get(1), "Faixa")).isEqualTo(Cell.text("Fora da faixa"));
        assertThat(cell(sheet, sheet.rows().get(3), "Situação")).isEqualTo(Cell.text("Inativa"));
    }

    @Test
    @DisplayName("leaves the weight, the day and the range blank for a cage never weighed")
    void givenCageNeverWeighed_whenBuildingTheSpreadsheet_thenLeaveItsWeighingBlank() {
        // given
        List<CageSummary> cages = cages();

        // when
        Sheet sheet = spreadsheet(new ReferenceWeight(155, 175), null, null, StatusFilter.ALL, cages).sheets().getFirst();

        // then
        List<Cell> c03 = sheet.rows().get(2);
        assertThat(cell(sheet, c03, "Peso médio (g)")).isEqualTo(Cell.blank());
        assertThat(cell(sheet, c03, "Data da pesagem")).isEqualTo(Cell.blank());
        assertThat(cell(sheet, c03, "Faixa")).isEqualTo(Cell.blank());
    }

    @Test
    @DisplayName("says there is no range for a weighed cage of a sector without range")
    void givenSectorWithoutRange_whenBuildingTheSpreadsheet_thenSayNoRangeForTheWeighedCages() {
        // given
        List<CageSummary> cages = cages();

        // when
        Sheet sheet = spreadsheet(null, null, null, StatusFilter.ALL, cages).sheets().getFirst();

        // then
        assertThat(cell(sheet, sheet.rows().getFirst(), "Faixa")).isEqualTo(Cell.text("Sem faixa definida"));
    }

    @Test
    @DisplayName("says no cage matches the filters when there is none")
    void givenNoCage_whenBuildingTheSpreadsheet_thenWriteTheNotice() {
        // given
        List<CageSummary> cages = List.of();

        // when
        Sheet sheet = spreadsheet(null, "Z", null, StatusFilter.ACTIVE, cages).sheets().getFirst();

        // then
        assertThat(sheet.emptyNotice()).isEqualTo("Nenhuma gaiola com esses filtros");
    }
}
