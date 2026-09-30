package io.github.ovyx.production.application.dailyreport;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.shared.application.spreadsheet.Cell;
import io.github.ovyx.shared.application.spreadsheet.CellFormat;
import io.github.ovyx.shared.application.spreadsheet.Column;
import io.github.ovyx.shared.application.spreadsheet.Sheet;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A planilha dos relatórios de um intervalo (data-model §2 da 007), montada sem banco a partir dos detalhes dos
 * relatórios, com os totais do próprio {@link DailyReportTotals}.
 *
 * <p>Dois relatórios: o de 01/09 completo, com 87 ovos de 100 aves e a ração de R$ 7,84; e o de 02/09 com uma
 * gaiola sem produção e outra sem ração, e a observação "=SOMA(A1:A9)".
 */
@DisplayName("DailyReportsSpreadsheet")
class DailyReportsSpreadsheetTest {

    private static final ReportingSector SECTOR =
            new ReportingSector(UUID.fromString("3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11"), "Codornas — Galpão 1", "ACTIVE");
    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 28);
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);
    private static final LocalTime NOW = LocalTime.of(21, 40);
    private static final BigDecimal PRICE = new BigDecimal("2.85");

    // ---------------------------------------------------------------- montagem dos relatórios

    private static ReportCageDetail cage(String battery, int number, CageProduction production, CageMortality mortality, Integer consumption) {
        CageFeed feed = consumption == null ? null : CageFeed.of(UUID.randomUUID(), "Postura Plus", PRICE, 28, consumption, 50);
        return new ReportCageDetail(
                UUID.randomUUID(), ReportCageDetail.codeOf(battery, number), battery, number, 50, production, mortality, feed);
    }

    private static DailyReportDetail report(
            LocalDate date, LocalTime time, int birds, String note, boolean noMortality, String openedBy, List<ReportCageDetail> cages) {
        ProductionTotals production = DailyReportTotals.production(birds, cages);
        return new DailyReportDetail(
                UUID.randomUUID(),
                SECTOR,
                date,
                time,
                birds,
                20,
                note,
                noMortality,
                new Actor(UUID.randomUUID(), openedBy),
                Instant.parse("2026-09-01T09:31:40Z"),
                null,
                null,
                production,
                DailyReportTotals.mortality(birds, noMortality, cages),
                DailyReportTotals.feed(cages, production.collectedEggs()),
                cages);
    }

    /** 01/09: completo, 87 ovos de 100 aves, 1 morte, 2.750 g de ração a R$ 2,85/kg. */
    private static DailyReportDetail complete() {
        return report(
                FROM,
                LocalTime.of(6, 30),
                100,
                null,
                false,
                "Marina Alves",
                List.of(
                        cage("A", 1, new CageProduction(44, 1, 0, 1, 0, 0, 0), new CageMortality(1, 0, "Bicada"), 1400),
                        cage("A", 2, new CageProduction(43, 0, 1, 0, 1, 0, 0), null, 1350)));
    }

    /** 02/09: a A-02 sem produção e a A-01 sem ração; mortalidade confirmada sem ocorrência. */
    private static DailyReportDetail pending() {
        return report(
                LocalDate.of(2026, 9, 2),
                LocalTime.of(7, 5),
                99,
                "=SOMA(A1:A9)",
                true,
                "Administrador do Sistema",
                List.of(
                        cage("A", 1, new CageProduction(40, 0, 0, 0, 0, 0, 0), null, null),
                        cage("A", 2, null, null, 1300)));
    }

    private static Spreadsheet spreadsheet(List<DailyReportDetail> reports) {
        return DailyReportsSpreadsheet.of(SECTOR, FROM, TO, reports, TODAY, NOW);
    }

    private static Sheet sheet(Spreadsheet spreadsheet, String name) {
        return spreadsheet.sheets().stream().filter(sheet -> sheet.name().equals(name)).findFirst().orElseThrow();
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

    // ---------------------------------------------------------------- arquivo e cabeçalho

    @Test
    @DisplayName("names the file after the sector and the interval")
    void givenInterval_whenBuildingTheSpreadsheet_thenNameTheFileAfterTheSectorAndTheDates() {
        // given
        List<DailyReportDetail> reports = List.of(complete());

        // when
        Spreadsheet spreadsheet = spreadsheet(reports);

        // then
        assertThat(spreadsheet.fileName()).isEqualTo("relatorios-codornas-galpao-1-01-09-2026-a-28-09-2026.xlsx");
        assertThat(spreadsheet.sheets()).extracting(Sheet::name).containsExactly("Relatórios", "Gaiolas");
    }

    @Test
    @DisplayName("heads both sheets with the sector, the interval and the time of the farm")
    void givenInterval_whenBuildingTheSpreadsheet_thenHeadBothSheets() {
        // given
        List<DailyReportDetail> reports = List.of(complete());

        // when
        Spreadsheet spreadsheet = spreadsheet(reports);

        // then
        assertThat(spreadsheet.sheets())
                .allSatisfy(sheet -> assertThat(sheet.heading())
                        .containsExactly(
                                "Ovyx — Relatórios diários",
                                "Setor: Codornas — Galpão 1",
                                "Período: 01/09/2026 a 28/09/2026",
                                "Gerada em 28/09/2026 às 21:40 (horário da granja)"));
    }

    // ---------------------------------------------------------------- aba Relatórios

    @Test
    @DisplayName("lays the columns of the reports in the order of the data model")
    void givenReports_whenBuildingTheSpreadsheet_thenLayTheColumnsInOrder() {
        // given
        List<DailyReportDetail> reports = List.of(complete());

        // when
        Sheet sheet = sheet(spreadsheet(reports), "Relatórios");

        // then
        assertThat(sheet.columns())
                .extracting(Column::title)
                .containsExactly(
                        "Data",
                        "Hora",
                        "Aberto por",
                        "Idade do lote (semanas)",
                        "Aves no início do dia",
                        "Ovos coletados",
                        "Produtividade",
                        "Ovos padrão",
                        "Pequenos",
                        "Jumbo",
                        "Sujos",
                        "Trincados",
                        "Com sangue",
                        "Anormais",
                        "Mortes",
                        "Descartes",
                        "Aves removidas",
                        "Saldo de aves",
                        "Consumo de ração (kg)",
                        "Custo de ração",
                        "Custo por ovo",
                        "Produção",
                        "Ração",
                        "Mortalidade",
                        "Observação");
    }

    @Test
    @DisplayName("writes one row per report, the oldest first, with the numbers of the report")
    void givenTwoReportsOutOfOrder_whenBuildingTheSpreadsheet_thenWriteTheOldestFirst() {
        // given
        List<DailyReportDetail> reports = List.of(pending(), complete());

        // when
        Sheet sheet = sheet(spreadsheet(reports), "Relatórios");

        // then
        assertThat(sheet.rows()).hasSize(2);
        List<Cell> first = sheet.rows().getFirst();
        assertThat(cell(sheet, first, "Data")).isEqualTo(Cell.date(FROM));
        assertThat(cell(sheet, first, "Hora")).isEqualTo(Cell.time(LocalTime.of(6, 30)));
        assertThat(cell(sheet, first, "Aberto por")).isEqualTo(Cell.text("Marina Alves"));
        assertNumber(cell(sheet, first, "Idade do lote (semanas)"), "20", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Aves no início do dia"), "100", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Ovos coletados"), "87", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Produtividade"), "87.00", CellFormat.PERCENT_2);
        assertNumber(cell(sheet, first, "Ovos padrão"), "83", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Pequenos"), "1", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Jumbo"), "1", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Sujos"), "1", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Trincados"), "1", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Com sangue"), "0", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Anormais"), "0", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Mortes"), "1", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Descartes"), "0", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Aves removidas"), "1", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Saldo de aves"), "99", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Consumo de ração (kg)"), "2.8", CellFormat.KILOGRAMS);
        assertNumber(cell(sheet, first, "Custo de ração"), "7.84", CellFormat.MONEY);
        assertNumber(cell(sheet, first, "Custo por ovo"), "0.090", CellFormat.MONEY_3);
        assertThat(cell(sheet, first, "Produção")).isEqualTo(Cell.text("Completa"));
        assertThat(cell(sheet, first, "Ração")).isEqualTo(Cell.text("Completa"));
        assertThat(cell(sheet, first, "Mortalidade")).isEqualTo(Cell.text("Lançada"));
        assertThat(cell(sheet, first, "Observação")).isEqualTo(Cell.blank());
    }

    @Test
    @DisplayName("leaves the costs blank on a report with the feed pending, and says what is pending")
    void givenReportWithTheFeedAndTheProductionPending_whenBuildingTheSpreadsheet_thenLeaveTheCostsBlank() {
        // given
        List<DailyReportDetail> reports = List.of(complete(), pending());

        // when
        Sheet sheet = sheet(spreadsheet(reports), "Relatórios");

        // then
        List<Cell> second = sheet.rows().get(1);
        assertNumber(cell(sheet, second, "Ovos coletados"), "40", CellFormat.COUNT);
        assertNumber(cell(sheet, second, "Produtividade"), "40.40", CellFormat.PERCENT_2);
        assertNumber(cell(sheet, second, "Consumo de ração (kg)"), "1.3", CellFormat.KILOGRAMS);
        assertThat(cell(sheet, second, "Custo de ração")).isEqualTo(Cell.blank());
        assertThat(cell(sheet, second, "Custo por ovo")).isEqualTo(Cell.blank());
        assertThat(cell(sheet, second, "Produção")).isEqualTo(Cell.text("Pendente: 1 gaiola"));
        assertThat(cell(sheet, second, "Ração")).isEqualTo(Cell.text("Pendente: 1 gaiola"));
        assertThat(cell(sheet, second, "Mortalidade")).isEqualTo(Cell.text("Lançada"));
        assertThat(cell(sheet, second, "Observação")).isEqualTo(Cell.text("=SOMA(A1:A9)"));
    }

    @Test
    @DisplayName("says the pending cages in the plural, and the mortality not recorded as pending")
    void givenReportWithThreeCagesPending_whenBuildingTheSpreadsheet_thenSayThreeCages() {
        // given
        DailyReportDetail empty = report(
                FROM,
                LocalTime.of(6, 30),
                150,
                null,
                false,
                "Marina Alves",
                List.of(cage("A", 1, null, null, null), cage("A", 2, null, null, null), cage("A", 3, null, null, null)));

        // when
        Sheet sheet = sheet(spreadsheet(List.of(empty)), "Relatórios");

        // then
        List<Cell> row = sheet.rows().getFirst();
        assertThat(cell(sheet, row, "Produção")).isEqualTo(Cell.text("Pendente: 3 gaiolas"));
        assertThat(cell(sheet, row, "Ração")).isEqualTo(Cell.text("Pendente: 3 gaiolas"));
        assertThat(cell(sheet, row, "Mortalidade")).isEqualTo(Cell.text("Pendente"));
    }

    @Test
    @DisplayName("totals the interval with the sums and the rules of a period")
    void givenTwoReports_whenBuildingTheSpreadsheet_thenTotalTheInterval() {
        // given
        List<DailyReportDetail> reports = List.of(complete(), pending());

        // when
        Sheet sheet = sheet(spreadsheet(reports), "Relatórios");

        // then
        List<Cell> totals = sheet.totals();
        assertThat(cell(sheet, totals, "Data")).isEqualTo(Cell.text("Total"));
        assertThat(cell(sheet, totals, "Hora")).isEqualTo(Cell.blank());
        assertNumber(cell(sheet, totals, "Aves no início do dia"), "199", CellFormat.COUNT);
        assertNumber(cell(sheet, totals, "Ovos coletados"), "127", CellFormat.COUNT);
        assertNumber(cell(sheet, totals, "Produtividade"), "63.82", CellFormat.PERCENT_2);
        assertNumber(cell(sheet, totals, "Ovos padrão"), "123", CellFormat.COUNT);
        assertNumber(cell(sheet, totals, "Pequenos"), "1", CellFormat.COUNT);
        assertNumber(cell(sheet, totals, "Trincados"), "1", CellFormat.COUNT);
        assertNumber(cell(sheet, totals, "Mortes"), "1", CellFormat.COUNT);
        assertNumber(cell(sheet, totals, "Descartes"), "0", CellFormat.COUNT);
        assertNumber(cell(sheet, totals, "Aves removidas"), "1", CellFormat.COUNT);
        assertThat(cell(sheet, totals, "Saldo de aves")).isEqualTo(Cell.blank());
        assertNumber(cell(sheet, totals, "Consumo de ração (kg)"), "4.1", CellFormat.KILOGRAMS);
        assertNumber(cell(sheet, totals, "Custo de ração"), "7.84", CellFormat.MONEY);
        assertNumber(cell(sheet, totals, "Custo por ovo"), "0.090", CellFormat.MONEY_3);
        assertThat(cell(sheet, totals, "Ração")).isEqualTo(Cell.text("1 dia fora do custo"));
        assertThat(cell(sheet, totals, "Observação")).isEqualTo(Cell.blank());
    }

    @Test
    @DisplayName("says two days left out of the cost in the plural")
    void givenTwoReportsWithTheFeedPending_whenBuildingTheSpreadsheet_thenSayTwoDays() {
        // given
        DailyReportDetail other = report(
                LocalDate.of(2026, 9, 3),
                LocalTime.of(6, 50),
                99,
                null,
                true,
                "Marina Alves",
                List.of(cage("A", 1, new CageProduction(41, 0, 0, 0, 0, 0, 0), null, null)));

        // when
        Sheet sheet = sheet(spreadsheet(List.of(complete(), pending(), other)), "Relatórios");

        // then
        assertThat(cell(sheet, sheet.totals(), "Ração")).isEqualTo(Cell.text("2 dias fora do custo"));
    }

    @Test
    @DisplayName("leaves the note of the totals blank when every day counts in the cost")
    void givenOnlyCompleteReports_whenBuildingTheSpreadsheet_thenSayNothingAboutDaysLeftOut() {
        // given
        List<DailyReportDetail> reports = List.of(complete());

        // when
        Sheet sheet = sheet(spreadsheet(reports), "Relatórios");

        // then
        assertThat(cell(sheet, sheet.totals(), "Ração")).isEqualTo(Cell.blank());
    }

    // ---------------------------------------------------------------- aba Gaiolas

    @Test
    @DisplayName("writes one row per cage of each report, with the entries of the cage")
    void givenTwoReports_whenBuildingTheSpreadsheet_thenWriteEachCageOfEachReport() {
        // given
        List<DailyReportDetail> reports = List.of(complete(), pending());

        // when
        Sheet sheet = sheet(spreadsheet(reports), "Gaiolas");

        // then
        assertThat(sheet.columns())
                .extracting(Column::title)
                .containsExactly(
                        "Data",
                        "Gaiola",
                        "Bateria",
                        "Número",
                        "Aves",
                        "Ovos coletados",
                        "Pequenos",
                        "Jumbo",
                        "Sujos",
                        "Trincados",
                        "Com sangue",
                        "Anormais",
                        "Mortes",
                        "Descartes",
                        "Observação da mortalidade",
                        "Fórmula",
                        "Consumo (g)",
                        "Custo");
        assertThat(sheet.rows()).hasSize(4);
        List<Cell> first = sheet.rows().getFirst();
        assertThat(cell(sheet, first, "Data")).isEqualTo(Cell.date(FROM));
        assertThat(cell(sheet, first, "Gaiola")).isEqualTo(Cell.text("A-01"));
        assertThat(cell(sheet, first, "Bateria")).isEqualTo(Cell.text("A"));
        assertNumber(cell(sheet, first, "Número"), "1", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Aves"), "50", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Ovos coletados"), "44", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Pequenos"), "1", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Mortes"), "1", CellFormat.COUNT);
        assertThat(cell(sheet, first, "Observação da mortalidade")).isEqualTo(Cell.text("Bicada"));
        assertThat(cell(sheet, first, "Fórmula")).isEqualTo(Cell.text("Postura Plus"));
        assertNumber(cell(sheet, first, "Consumo (g)"), "1400", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Custo"), "3.99", CellFormat.MONEY);
    }

    @Test
    @DisplayName("leaves blank the entries a cage does not have")
    void givenCageWithoutEntries_whenBuildingTheSpreadsheet_thenLeaveTheirCellsBlank() {
        // given
        List<DailyReportDetail> reports = List.of(pending());

        // when
        Sheet sheet = sheet(spreadsheet(reports), "Gaiolas");

        // then
        List<Cell> withoutFeed = sheet.rows().get(0);
        assertThat(cell(sheet, withoutFeed, "Fórmula")).isEqualTo(Cell.blank());
        assertThat(cell(sheet, withoutFeed, "Consumo (g)")).isEqualTo(Cell.blank());
        assertThat(cell(sheet, withoutFeed, "Custo")).isEqualTo(Cell.blank());
        assertThat(cell(sheet, withoutFeed, "Mortes")).isEqualTo(Cell.blank());
        List<Cell> withoutProduction = sheet.rows().get(1);
        List<Cell> production = new ArrayList<>();
        for (String title : List.of("Ovos coletados", "Pequenos", "Jumbo", "Sujos", "Trincados", "Com sangue", "Anormais")) {
            production.add(cell(sheet, withoutProduction, title));
        }
        assertThat(production).allMatch(Cell.Blank.class::isInstance);
        assertNumber(cell(sheet, withoutProduction, "Consumo (g)"), "1300", CellFormat.COUNT);
    }

    // ---------------------------------------------------------------- sem relatório

    @Test
    @DisplayName("writes the notice of no report on both sheets of an empty interval")
    void givenNoReport_whenBuildingTheSpreadsheet_thenWriteTheNoticeOnBothSheets() {
        // given
        List<DailyReportDetail> reports = List.of();

        // when
        Spreadsheet spreadsheet = DailyReportsSpreadsheet.of(
                SECTOR, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), reports, TODAY, NOW);

        // then
        assertThat(spreadsheet.sheets())
                .allSatisfy(sheet -> {
                    assertThat(sheet.rows()).isEmpty();
                    assertThat(sheet.totals()).isEmpty();
                    assertThat(sheet.emptyNotice()).isEqualTo("Nenhum relatório de 01/08/2026 a 31/08/2026");
                });
    }

    @Test
    @DisplayName("writes the notice of no cage when the reports of the interval have none")
    void givenReportsWithoutCages_whenBuildingTheSpreadsheet_thenWriteTheNoticeOfNoCage() {
        // given
        DailyReportDetail withoutCages = report(FROM, LocalTime.of(6, 30), 100, null, true, "Marina Alves", List.of());

        // when
        Sheet sheet = sheet(spreadsheet(List.of(withoutCages)), "Gaiolas");

        // then
        assertThat(sheet.rows()).isEmpty();
        assertThat(sheet.emptyNotice()).isEqualTo("Nenhuma gaiola nos relatórios de 01/09/2026 a 28/09/2026");
    }
}
