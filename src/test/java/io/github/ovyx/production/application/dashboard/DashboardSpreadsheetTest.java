package io.github.ovyx.production.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.application.dailyreport.ReportingSector;
import io.github.ovyx.production.domain.model.Actor;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A planilha do painel (US2 da 007; data-model §3), montada sem banco a partir do {@link SectorDashboard}: os
 * mesmos números da tela, com os textos que a tela escreve.
 */
@DisplayName("DashboardSpreadsheet")
class DashboardSpreadsheetTest {

    private static final ReportingSector SECTOR =
            new ReportingSector(UUID.fromString("3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11"), "Codornas — Galpão 1", "ACTIVE");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);
    private static final LocalTime NOW = LocalTime.of(21, 40);
    private static final BigDecimal TARGET = new BigDecimal("85.00");

    private static BigDecimal number(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static Indicators indicators() {
        return new Indicators(
                new Indicator(number("1740"), number("1700"), number("2.4"), GoodDirection.UP, 0),
                new Indicator(number("87.00"), number("85.00"), number("2.00"), GoodDirection.UP, 1),
                new Indicator(number("159.60"), number("157.25"), number("1.5"), GoodDirection.DOWN, 1),
                new Indicator(number("0.092"), null, null, GoodDirection.DOWN, 2));
    }

    /** Os 7 dias de 22 a 28/09: o dia 25 sem relatório, e o 27 sem a ração. */
    private static List<DashboardDay> trend() {
        return List.of(
                new DashboardDay(TODAY.minusDays(6), 1700, number("85.00"), number("157.25"), number("0.092")),
                new DashboardDay(TODAY.minusDays(5), 1710, number("85.50"), number("158.00"), number("0.092")),
                new DashboardDay(TODAY.minusDays(4), 1720, number("86.00"), number("158.40"), number("0.092")),
                new DashboardDay(TODAY.minusDays(3), null, null, null, null),
                new DashboardDay(TODAY.minusDays(2), 1730, number("86.50"), number("159.00"), number("0.092")),
                new DashboardDay(TODAY.minusDays(1), 1735, number("86.75"), null, null),
                new DashboardDay(TODAY, 1740, number("87.00"), number("159.60"), number("0.092")));
    }

    private static EggGrading grades() {
        return new EggGrading(
                1740,
                new EggGradeShare("standard", 1650, number("94.8")),
                List.of(
                        new EggGradeShare("small", 30, number("1.7")),
                        new EggGradeShare("jumbo", 12, number("0.7")),
                        new EggGradeShare("dirty", 20, number("1.1")),
                        new EggGradeShare("cracked", 18, number("1.0")),
                        new EggGradeShare("bloodSpot", 6, number("0.3")),
                        new EggGradeShare("abnormal", 4, number("0.2"))));
    }

    private static List<DashboardAlert> alerts() {
        return List.of(
                new DashboardAlert(
                        AlertKind.FEED_PENDING,
                        AlertTone.INFO,
                        "Ração pendente no relatório de hoje",
                        "Falta lançar a ração de 1 gaiola.",
                        new AlertTarget(UUID.randomUUID(), null, null)),
                new DashboardAlert(
                        AlertKind.WEIGHT_OUT_OF_RANGE,
                        AlertTone.WARNING,
                        "Pesagem fora da faixa na gaiola A-02",
                        "150,8 g em 28/09/2026; a faixa do setor é 155–175 g.",
                        new AlertTarget(null, UUID.randomUUID(), "A-02")));
    }

    private static List<LatestReport> latest() {
        Actor marina = new Actor(UUID.randomUUID(), "Marina Alves");
        return List.of(
                new LatestReport(
                        UUID.randomUUID(),
                        TODAY,
                        LocalTime.of(6, 30),
                        marina,
                        1740,
                        2,
                        ProductionStatus.COMPLETE,
                        FeedStatus.PENDING,
                        MortalityStatus.RECORDED),
                new LatestReport(
                        UUID.randomUUID(),
                        TODAY.minusDays(1),
                        LocalTime.of(7, 5),
                        marina,
                        1735,
                        0,
                        ProductionStatus.COMPLETE,
                        FeedStatus.COMPLETE,
                        MortalityStatus.RECORDED));
    }

    private static SectorDashboard dashboard(DashboardPeriod period, LocalDate from, LocalDate to) {
        return new SectorDashboard(
                SECTOR, period, from, to, null, indicators(), trend(), TARGET, TargetStatus.ABOVE, grades(), alerts(), 2, latest());
    }

    private static SectorDashboard lastSevenDays() {
        return dashboard(DashboardPeriod.LAST_7_DAYS, TODAY.minusDays(6), TODAY);
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

    // ---------------------------------------------------------------- arquivo e cabeçalho

    @Test
    @DisplayName("names the file after the sector and today of the farm, with five sheets")
    void givenDashboard_whenBuildingTheSpreadsheet_thenNameTheFileAndLayTheFiveSheets() {
        // given
        SectorDashboard dashboard = lastSevenDays();

        // when
        Spreadsheet spreadsheet = DashboardSpreadsheet.of(dashboard, TODAY, NOW);

        // then
        assertThat(spreadsheet.fileName()).isEqualTo("painel-codornas-galpao-1-28-09-2026.xlsx");
        assertThat(spreadsheet.sheets())
                .extracting(Sheet::name)
                .containsExactly("Indicadores", "7 dias", "Classificação", "Alertas", "Últimos relatórios");
    }

    @Test
    @DisplayName("heads each sheet with the period of seven days and the week it is compared with")
    void givenLastSevenDays_whenBuildingTheSpreadsheet_thenHeadWithBothWeeks() {
        // given
        SectorDashboard dashboard = lastSevenDays();

        // when
        Spreadsheet spreadsheet = DashboardSpreadsheet.of(dashboard, TODAY, NOW);

        // then
        assertThat(spreadsheet.sheets())
                .allSatisfy(sheet -> assertThat(sheet.heading())
                        .containsExactly(
                                "Ovyx — Painel",
                                "Setor: Codornas — Galpão 1",
                                "Período: 7 dias, de 22/09/2026 a 28/09/2026, comparado com 15/09/2026 a 21/09/2026",
                                "Gerada em 28/09/2026 às 21:40 (horário da granja)"));
    }

    @Test
    @DisplayName("heads the sheets of today with today and yesterday")
    void givenToday_whenBuildingTheSpreadsheet_thenHeadWithTodayAndYesterday() {
        // given
        SectorDashboard dashboard = dashboard(DashboardPeriod.TODAY, TODAY, TODAY);

        // when
        Spreadsheet spreadsheet = DashboardSpreadsheet.of(dashboard, TODAY, NOW);

        // then
        assertThat(spreadsheet.sheets().getFirst().heading())
                .contains("Período: hoje, 28/09/2026, comparado com 27/09/2026");
    }

    @Test
    @DisplayName("heads the sheets of yesterday with yesterday and the day before")
    void givenYesterday_whenBuildingTheSpreadsheet_thenHeadWithYesterdayAndTheDayBefore() {
        // given
        SectorDashboard dashboard = dashboard(DashboardPeriod.YESTERDAY, TODAY.minusDays(1), TODAY.minusDays(1));

        // when
        Spreadsheet spreadsheet = DashboardSpreadsheet.of(dashboard, TODAY, NOW);

        // then
        assertThat(spreadsheet.sheets().getFirst().heading())
                .contains("Período: ontem, 27/09/2026, comparado com 26/09/2026");
    }

    // ---------------------------------------------------------------- Indicadores

    @Test
    @DisplayName("writes the four indicators with the value, the previous one and the change")
    void givenIndicators_whenBuildingTheSpreadsheet_thenWriteEachWithItsFormat() {
        // given
        SectorDashboard dashboard = lastSevenDays();

        // when
        Sheet sheet = sheet(DashboardSpreadsheet.of(dashboard, TODAY, NOW), "Indicadores");

        // then
        assertThat(sheet.columns())
                .extracting(Column::title)
                .containsExactly("Indicador", "Valor", "Período anterior", "Variação", "Observação");
        assertThat(sheet.rows()).extracting(List::getFirst)
                .containsExactly(
                        Cell.text("Produção"), Cell.text("Produtividade"), Cell.text("Custo de ração"), Cell.text("Custo por ovo"));
        List<Cell> production = rowOf(sheet, "Produção");
        assertNumber(cell(sheet, production, "Valor"), "1740", CellFormat.COUNT);
        assertNumber(cell(sheet, production, "Período anterior"), "1700", CellFormat.COUNT);
        assertNumber(cell(sheet, production, "Variação"), "2.4", CellFormat.PERCENT_1);
        assertThat(cell(sheet, production, "Observação")).isEqualTo(Cell.blank());
        List<Cell> rate = rowOf(sheet, "Produtividade");
        assertNumber(cell(sheet, rate, "Valor"), "87.00", CellFormat.PERCENT_2);
        assertNumber(cell(sheet, rate, "Variação"), "2.00", CellFormat.POINTS);
        assertThat(cell(sheet, rate, "Observação")).isEqualTo(Cell.text("1 dia com a produção pendente"));
        List<Cell> cost = rowOf(sheet, "Custo de ração");
        assertNumber(cell(sheet, cost, "Valor"), "159.60", CellFormat.MONEY);
        assertNumber(cell(sheet, cost, "Período anterior"), "157.25", CellFormat.MONEY);
        assertNumber(cell(sheet, cost, "Variação"), "1.5", CellFormat.PERCENT_1);
        assertThat(cell(sheet, cost, "Observação")).isEqualTo(Cell.text("1 dia sem ração completa"));
    }

    @Test
    @DisplayName("leaves the missing previous value blank and says there is no comparison")
    void givenIndicatorWithoutPreviousValue_whenBuildingTheSpreadsheet_thenSayNoComparison() {
        // given
        SectorDashboard dashboard = lastSevenDays();

        // when
        Sheet sheet = sheet(DashboardSpreadsheet.of(dashboard, TODAY, NOW), "Indicadores");

        // then
        List<Cell> costPerEgg = rowOf(sheet, "Custo por ovo");
        assertNumber(cell(sheet, costPerEgg, "Valor"), "0.092", CellFormat.MONEY_3);
        assertThat(cell(sheet, costPerEgg, "Período anterior")).isEqualTo(Cell.blank());
        assertThat(cell(sheet, costPerEgg, "Variação")).isEqualTo(Cell.text("sem comparação"));
        assertThat(cell(sheet, costPerEgg, "Observação")).isEqualTo(Cell.text("2 dias sem ração completa"));
    }

    @Test
    @DisplayName("leaves an indicator without data blank, and not zero")
    void givenIndicatorWithoutData_whenBuildingTheSpreadsheet_thenLeaveItBlank() {
        // given
        Indicators empty = new Indicators(
                new Indicator(null, number("1700"), null, GoodDirection.UP, 0),
                new Indicator(null, null, null, GoodDirection.UP, 0),
                new Indicator(null, null, null, GoodDirection.DOWN, 0),
                new Indicator(null, null, null, GoodDirection.DOWN, 0));
        SectorDashboard dashboard = new SectorDashboard(
                SECTOR, DashboardPeriod.TODAY, TODAY, TODAY, null, empty, trend(), TARGET, null, null, List.of(), 0, List.of());

        // when
        Sheet sheet = sheet(DashboardSpreadsheet.of(dashboard, TODAY, NOW), "Indicadores");

        // then
        List<Cell> production = rowOf(sheet, "Produção");
        assertThat(cell(sheet, production, "Valor")).isEqualTo(Cell.blank());
        assertNumber(cell(sheet, production, "Período anterior"), "1700", CellFormat.COUNT);
        assertThat(cell(sheet, production, "Variação")).isEqualTo(Cell.text("sem comparação"));
    }

    // ---------------------------------------------------------------- 7 dias

    @Test
    @DisplayName("writes the seven days with the target, leaving blank the day without report and the cost without feed")
    void givenTrend_whenBuildingTheSpreadsheet_thenWriteTheSevenDays() {
        // given
        SectorDashboard dashboard = lastSevenDays();

        // when
        Sheet sheet = sheet(DashboardSpreadsheet.of(dashboard, TODAY, NOW), "7 dias");

        // then
        assertThat(sheet.columns())
                .extracting(Column::title)
                .containsExactly("Data", "Produção", "Produtividade", "Meta", "Custo de ração", "Custo por ovo");
        assertThat(sheet.rows()).hasSize(7);
        List<Cell> first = sheet.rows().getFirst();
        assertThat(cell(sheet, first, "Data")).isEqualTo(Cell.date(TODAY.minusDays(6)));
        assertNumber(cell(sheet, first, "Produção"), "1700", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Produtividade"), "85.00", CellFormat.PERCENT_2);
        assertNumber(cell(sheet, first, "Meta"), "85.00", CellFormat.PERCENT_2);
        assertNumber(cell(sheet, first, "Custo de ração"), "157.25", CellFormat.MONEY);
        assertNumber(cell(sheet, first, "Custo por ovo"), "0.092", CellFormat.MONEY_3);
        List<Cell> withoutReport = sheet.rows().get(3);
        assertThat(List.of(
                        cell(sheet, withoutReport, "Produção"),
                        cell(sheet, withoutReport, "Produtividade"),
                        cell(sheet, withoutReport, "Custo de ração"),
                        cell(sheet, withoutReport, "Custo por ovo")))
                .allMatch(Cell.Blank.class::isInstance);
        List<Cell> withoutFeed = sheet.rows().get(5);
        assertNumber(cell(sheet, withoutFeed, "Produção"), "1735", CellFormat.COUNT);
        assertThat(cell(sheet, withoutFeed, "Custo de ração")).isEqualTo(Cell.blank());
    }

    // ---------------------------------------------------------------- Classificação

    @Test
    @DisplayName("writes the grading of the period with the standard eggs, each grade and the collected ones")
    void givenGrading_whenBuildingTheSpreadsheet_thenWriteEachGradeAndTheTotal() {
        // given
        SectorDashboard dashboard = lastSevenDays();

        // when
        Sheet sheet = sheet(DashboardSpreadsheet.of(dashboard, TODAY, NOW), "Classificação");

        // then
        assertThat(sheet.columns()).extracting(Column::title).containsExactly("Classe", "Quantidade", "% dos coletados");
        assertThat(sheet.rows()).extracting(List::getFirst)
                .containsExactly(
                        Cell.text("Padrão"),
                        Cell.text("Pequenos"),
                        Cell.text("Jumbo"),
                        Cell.text("Sujos"),
                        Cell.text("Trincados"),
                        Cell.text("Com sangue"),
                        Cell.text("Anormais"));
        List<Cell> standard = rowOf(sheet, "Padrão");
        assertNumber(cell(sheet, standard, "Quantidade"), "1650", CellFormat.COUNT);
        assertNumber(cell(sheet, standard, "% dos coletados"), "94.8", CellFormat.PERCENT_1);
        assertThat(sheet.totals().getFirst()).isEqualTo(Cell.text("Coletados"));
        assertNumber(cell(sheet, sheet.totals(), "Quantidade"), "1740", CellFormat.COUNT);
        assertNumber(cell(sheet, sheet.totals(), "% dos coletados"), "100.0", CellFormat.PERCENT_1);
    }

    @Test
    @DisplayName("says no egg was collected when the period has no grading")
    void givenNoGrading_whenBuildingTheSpreadsheet_thenWriteTheNotice() {
        // given
        SectorDashboard dashboard = new SectorDashboard(
                SECTOR, DashboardPeriod.TODAY, TODAY, TODAY, null, indicators(), trend(), TARGET, null, null, List.of(), 0, List.of());

        // when
        Sheet sheet = sheet(DashboardSpreadsheet.of(dashboard, TODAY, NOW), "Classificação");

        // then
        assertThat(sheet.emptyNotice()).isEqualTo("Nenhum ovo coletado no período");
    }

    // ---------------------------------------------------------------- Alertas e últimos relatórios

    @Test
    @DisplayName("writes the open alerts in the order of the dashboard, with the tone in words")
    void givenAlerts_whenBuildingTheSpreadsheet_thenWriteEachAlert() {
        // given
        SectorDashboard dashboard = lastSevenDays();

        // when
        Sheet sheet = sheet(DashboardSpreadsheet.of(dashboard, TODAY, NOW), "Alertas");

        // then
        assertThat(sheet.columns()).extracting(Column::title).containsExactly("Tipo", "Alerta", "Detalhe");
        assertThat(sheet.rows())
                .containsExactly(
                        List.of(
                                Cell.text("Informação"),
                                Cell.text("Ração pendente no relatório de hoje"),
                                Cell.text("Falta lançar a ração de 1 gaiola.")),
                        List.of(
                                Cell.text("Atenção"),
                                Cell.text("Pesagem fora da faixa na gaiola A-02"),
                                Cell.text("150,8 g em 28/09/2026; a faixa do setor é 155–175 g.")));
    }

    @Test
    @DisplayName("says there is no open alert when the dashboard has none")
    void givenNoAlert_whenBuildingTheSpreadsheet_thenWriteTheNotice() {
        // given
        SectorDashboard dashboard = new SectorDashboard(
                SECTOR, DashboardPeriod.TODAY, TODAY, TODAY, null, indicators(), trend(), TARGET, null, grades(), List.of(), 0, List.of());

        // when
        Spreadsheet spreadsheet = DashboardSpreadsheet.of(dashboard, TODAY, NOW);

        // then
        assertThat(sheet(spreadsheet, "Alertas").emptyNotice()).isEqualTo("Nenhum alerta aberto");
        assertThat(sheet(spreadsheet, "Últimos relatórios").emptyNotice()).isEqualTo("Nenhum relatório no setor");
    }

    @Test
    @DisplayName("writes the latest reports with the situation the screen shows")
    void givenLatestReports_whenBuildingTheSpreadsheet_thenWriteEachWithItsSituation() {
        // given
        SectorDashboard dashboard = lastSevenDays();

        // when
        Sheet sheet = sheet(DashboardSpreadsheet.of(dashboard, TODAY, NOW), "Últimos relatórios");

        // then
        assertThat(sheet.columns())
                .extracting(Column::title)
                .containsExactly("Data", "Hora", "Aberto por", "Ovos coletados", "Aves removidas", "Situação");
        List<Cell> first = sheet.rows().getFirst();
        assertThat(cell(sheet, first, "Data")).isEqualTo(Cell.date(TODAY));
        assertThat(cell(sheet, first, "Hora")).isEqualTo(Cell.time(LocalTime.of(6, 30)));
        assertThat(cell(sheet, first, "Aberto por")).isEqualTo(Cell.text("Marina Alves"));
        assertNumber(cell(sheet, first, "Ovos coletados"), "1740", CellFormat.COUNT);
        assertNumber(cell(sheet, first, "Aves removidas"), "2", CellFormat.COUNT);
        assertThat(cell(sheet, first, "Situação")).isEqualTo(Cell.text("Ração pendente"));
        assertThat(cell(sheet, sheet.rows().get(1), "Situação")).isEqualTo(Cell.text("Completo"));
    }

    // ---------------------------------------------------------------- mutação (T051)

    @Test
    @DisplayName("says the production is pending before the feed, as the screen does")
    void givenLatestReportWithTheProductionAndTheFeedPending_whenBuildingTheSpreadsheet_thenSayTheProductionFirst() {
        // given
        LatestReport pending = new LatestReport(
                UUID.randomUUID(),
                TODAY,
                LocalTime.of(6, 30),
                new Actor(UUID.randomUUID(), "Marina Alves"),
                800,
                0,
                ProductionStatus.PENDING,
                FeedStatus.PENDING,
                MortalityStatus.PENDING);
        SectorDashboard dashboard = new SectorDashboard(
                SECTOR, DashboardPeriod.TODAY, TODAY, TODAY, null, indicators(), trend(), TARGET, null, grades(), List.of(), 0, List.of(pending));

        // when
        Sheet sheet = sheet(DashboardSpreadsheet.of(dashboard, TODAY, NOW), "Últimos relatórios");

        // then
        assertThat(cell(sheet, sheet.rows().getFirst(), "Situação")).isEqualTo(Cell.text("Produção pendente"));
    }

    // ---------------------------------------------------------------- meta do setor (008)

    @Test
    @DisplayName("writes the target of the sector in every day, and the low laying alert with it (008)")
    void givenSectorWithATargetOf72_whenBuildingTheSpreadsheet_thenWriteItInEveryDayAndInTheAlert() {
        // given
        DashboardAlert lowLaying = new DashboardAlert(
                AlertKind.LOW_LAYING,
                AlertTone.WARNING,
                "Baixa postura na gaiola C-03",
                "70,0% nos últimos 3 relatórios, abaixo da meta de 72% do setor; o setor fez 85,0% hoje.",
                new AlertTarget(null, UUID.randomUUID(), "C-03"));
        SectorDashboard dashboard = new SectorDashboard(
                SECTOR,
                DashboardPeriod.LAST_7_DAYS,
                TODAY.minusDays(6),
                TODAY,
                null,
                indicators(),
                trend(),
                new LayingRateTarget(new BigDecimal("72")).value(),
                TargetStatus.ABOVE,
                grades(),
                List.of(lowLaying),
                1,
                latest());

        // when
        Spreadsheet spreadsheet = DashboardSpreadsheet.of(dashboard, TODAY, NOW);

        // then
        Sheet days = sheet(spreadsheet, "7 dias");
        assertThat(days.rows()).hasSize(7);
        days.rows().forEach(row -> assertNumber(cell(days, row, "Meta"), "72.00", CellFormat.PERCENT_2));
        assertThat(sheet(spreadsheet, "Alertas").rows())
                .containsExactly(List.of(
                        Cell.text("Atenção"),
                        Cell.text("Baixa postura na gaiola C-03"),
                        Cell.text("70,0% nos últimos 3 relatórios, abaixo da meta de 72% do setor; o setor fez 85,0% hoje.")));
    }
}
