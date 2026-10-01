package io.github.ovyx.production.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import io.github.ovyx.shared.application.spreadsheet.Cell;
import io.github.ovyx.shared.application.spreadsheet.CellFormat;
import io.github.ovyx.shared.application.spreadsheet.Column;
import io.github.ovyx.shared.application.spreadsheet.Sheet;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A planilha da granja toda (US4 da 009), montada sem banco a partir do {@link FarmDashboard}: os mesmos números da
 * tela, nos formatos das planilhas da 007.
 */
@DisplayName("FarmDashboardSpreadsheet")
class FarmDashboardSpreadsheetTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);
    private static final LocalTime NOW = LocalTime.of(21, 40);

    private static BigDecimal number(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static Indicators indicators() {
        return new Indicators(
                new Indicator(number("2900"), number("2860"), number("1.4"), GoodDirection.UP, 0),
                new Indicator(number("90.63"), number("89.38"), number("1.25"), GoodDirection.UP, 0),
                new Indicator(number("159.60"), number("277.65"), number("-42.5"), GoodDirection.DOWN, 1),
                new Indicator(number("0.092"), number("0.097"), number("-5.2"), GoodDirection.DOWN, 1));
    }

    /** Os 7 dias de 22 a 28/09: o dia 25 sem relatório, e o 28 com os dois setores. */
    private static List<FarmDay> trend() {
        List<FarmDay> days = new ArrayList<>();
        for (int back = 6; back >= 0; back--) {
            LocalDate date = TODAY.minusDays(back);
            days.add(back == 3
                    ? new FarmDay(date, null, null, null, null, null, 0)
                    : new FarmDay(date, 2900, number("90.63"), number("280.00"), number("0.097"), number("80.13"), 2));
        }
        return days;
    }

    private static List<FarmSectorRow> rows() {
        return List.of(
                new FarmSectorRow(
                        new DashboardSector(UUID.randomUUID(), "Codornas — Galpão 1"),
                        1740,
                        number("87.00"),
                        number("85.00"),
                        TargetStatus.ABOVE,
                        number("0.092"),
                        new TodayReport(
                                UUID.randomUUID(), ProductionStatus.COMPLETE, FeedStatus.COMPLETE, MortalityStatus.RECORDED),
                        3),
                new FarmSectorRow(
                        new DashboardSector(UUID.randomUUID(), "Codornas — Galpão 4"),
                        null,
                        null,
                        number("85.00"),
                        null,
                        null,
                        null,
                        0),
                new FarmSectorRow(
                        new DashboardSector(UUID.randomUUID(), "Poedeiras — Galpão 2"),
                        840,
                        number("70.00"),
                        number("72.00"),
                        TargetStatus.BELOW,
                        number("0.143"),
                        new TodayReport(
                                UUID.randomUUID(), ProductionStatus.COMPLETE, FeedStatus.PENDING, MortalityStatus.RECORDED),
                        1));
    }

    private static FarmDashboard farm() {
        return new FarmDashboard(
                DashboardPeriod.LAST_7_DAYS,
                TODAY.minusDays(6),
                TODAY,
                3,
                2,
                indicators(),
                trend(),
                number("80.58"),
                TargetStatus.ABOVE,
                new EggGrading(
                        2900,
                        new EggGradeShare("standard", 2852, number("98.3")),
                        List.of(new EggGradeShare("small", 48, number("1.7")))),
                rows());
    }

    private static Sheet sheet(Spreadsheet spreadsheet, String name) {
        return spreadsheet.sheets().stream().filter(sheet -> sheet.name().equals(name)).findFirst().orElseThrow();
    }

    private static List<Cell> rowOf(Sheet sheet, String first) {
        return sheet.rows().stream()
                .filter(row -> row.getFirst().equals(Cell.text(first)))
                .findFirst()
                .orElseThrow();
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
    @DisplayName("names the file after today of the farm, with four sheets and the heading of the whole farm")
    void givenFarm_whenBuildingTheSpreadsheet_thenNameTheFileAndLayTheFourSheets() {
        // given
        FarmDashboard farm = farm();

        // when
        Spreadsheet spreadsheet = FarmDashboardSpreadsheet.of(farm, TODAY, NOW);

        // then
        assertThat(spreadsheet.fileName()).isEqualTo("painel-granja-28-09-2026.xlsx");
        assertThat(spreadsheet.sheets())
                .extracting(Sheet::name)
                .containsExactly("Indicadores", "7 dias", "Classificação", "Setores");
        assertThat(sheet(spreadsheet, "Indicadores").heading())
                .containsExactly(
                        "Ovyx — Painel da granja toda",
                        "Setores ativos: 3, 2 com relatório no período",
                        "Período: 7 dias, de 22/09/2026 a 28/09/2026, comparado com 15/09/2026 a 21/09/2026",
                        "Gerada em 28/09/2026 às 21:40 (horário da granja)");
    }

    @Test
    @DisplayName("writes the indicators of the farm, with the pending ones counted in reports")
    void givenFarm_whenBuildingTheIndicators_thenWriteThemWithTheReportsLeftOut() {
        // given
        FarmDashboard farm = farm();

        // when
        Sheet sheet = sheet(FarmDashboardSpreadsheet.of(farm, TODAY, NOW), "Indicadores");

        // then
        List<Cell> production = rowOf(sheet, "Produção");
        assertNumber(cell(sheet, production, "Valor"), "2900", CellFormat.COUNT);
        assertNumber(cell(sheet, production, "Variação"), "1.4", CellFormat.PERCENT_1);
        assertNumber(cell(sheet, rowOf(sheet, "Produtividade"), "Valor"), "90.63", CellFormat.PERCENT_2);
        assertThat(cell(sheet, rowOf(sheet, "Custo de ração"), "Observação"))
                .isEqualTo(Cell.text("1 relatório sem ração completa"));
    }

    @Test
    @DisplayName("writes the seven days with the target of each day and the sectors reporting, blank without report")
    void givenFarm_whenBuildingTheSevenDays_thenWriteTheTargetOfEachDayAndTheSectors() {
        // given
        FarmDashboard farm = farm();

        // when
        Sheet sheet = sheet(FarmDashboardSpreadsheet.of(farm, TODAY, NOW), "7 dias");

        // then
        assertThat(sheet.columns())
                .extracting(Column::title)
                .containsExactly(
                        "Data",
                        "Produção",
                        "Produtividade",
                        "Meta",
                        "Custo de ração",
                        "Custo por ovo",
                        "Setores com relatório");
        assertThat(sheet.rows()).hasSize(7);
        List<Cell> last = sheet.rows().getLast();
        assertThat(cell(sheet, last, "Data")).isEqualTo(Cell.date(TODAY));
        assertNumber(cell(sheet, last, "Meta"), "80.13", CellFormat.PERCENT_2);
        assertNumber(cell(sheet, last, "Setores com relatório"), "2", CellFormat.COUNT);
        List<Cell> withoutReport = sheet.rows().get(3);
        assertThat(cell(sheet, withoutReport, "Produção")).isEqualTo(Cell.blank());
        assertThat(cell(sheet, withoutReport, "Meta")).isEqualTo(Cell.blank());
        assertNumber(cell(sheet, withoutReport, "Setores com relatório"), "0", CellFormat.COUNT);
    }

    @Test
    @DisplayName("writes one row per sector, with the status, the report of today and blanks for the one without report")
    void givenFarm_whenBuildingTheSectors_thenWriteOneRowPerSector() {
        // given
        FarmDashboard farm = farm();

        // when
        Sheet sheet = sheet(FarmDashboardSpreadsheet.of(farm, TODAY, NOW), "Setores");

        // then
        assertThat(sheet.columns())
                .extracting(Column::title)
                .containsExactly(
                        "Setor",
                        "Produção",
                        "Produtividade",
                        "Meta",
                        "Situação",
                        "Custo por ovo",
                        "Relatório de hoje",
                        "Alertas");
        List<Cell> first = rowOf(sheet, "Codornas — Galpão 1");
        assertNumber(cell(sheet, first, "Produção"), "1740", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Produtividade"), "87.00", CellFormat.PERCENT_2);
        assertNumber(cell(sheet, first, "Meta"), "85.00", CellFormat.PERCENT_2);
        assertThat(cell(sheet, first, "Situação")).isEqualTo(Cell.text("Acima da meta"));
        assertNumber(cell(sheet, first, "Custo por ovo"), "0.092", CellFormat.MONEY_3);
        assertThat(cell(sheet, first, "Relatório de hoje")).isEqualTo(Cell.text("Completo"));
        assertNumber(cell(sheet, first, "Alertas"), "3", CellFormat.COUNT);
        List<Cell> withoutReport = rowOf(sheet, "Codornas — Galpão 4");
        assertThat(cell(sheet, withoutReport, "Produção")).isEqualTo(Cell.blank());
        assertThat(cell(sheet, withoutReport, "Situação")).isEqualTo(Cell.blank());
        assertThat(cell(sheet, withoutReport, "Relatório de hoje")).isEqualTo(Cell.text("Não aberto"));
        List<Cell> below = rowOf(sheet, "Poedeiras — Galpão 2");
        assertThat(cell(sheet, below, "Situação")).isEqualTo(Cell.text("Abaixo da meta"));
        assertThat(cell(sheet, below, "Relatório de hoje")).isEqualTo(Cell.text("Ração pendente"));
    }

    @Test
    @DisplayName("writes the grading of the farm as the one of a sector")
    void givenFarm_whenBuildingTheGrading_thenWriteTheStandardAndEachClass() {
        // given
        FarmDashboard farm = farm();

        // when
        Sheet sheet = sheet(FarmDashboardSpreadsheet.of(farm, TODAY, NOW), "Classificação");

        // then
        assertNumber(cell(sheet, rowOf(sheet, "Padrão"), "Quantidade"), "2852", CellFormat.COUNT);
        assertNumber(cell(sheet, rowOf(sheet, "Pequenos"), "% dos coletados"), "1.7", CellFormat.PERCENT_1);
    }
}
