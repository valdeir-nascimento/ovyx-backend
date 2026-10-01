package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.application.dashboard.DashboardSheets.PendingUnit;
import io.github.ovyx.shared.application.spreadsheet.Cell;
import io.github.ovyx.shared.application.spreadsheet.CellFormat;
import io.github.ovyx.shared.application.spreadsheet.Column;
import io.github.ovyx.shared.application.spreadsheet.FileNames;
import io.github.ovyx.shared.application.spreadsheet.Sheet;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetHeading;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A planilha do painel de um setor num periodo (US2 da 007; data-model §3), montada sem banco a partir do
 * {@link SectorDashboard}: os mesmos numeros da tela, e os textos que a tela escreve (a nota dos dias
 * incompletos, a situacao dos ultimos relatorios, "sem comparacao").
 *
 * <p>Cinco abas, cada uma com o cabecalho do setor, do periodo com a comparacao e de quando foi gerada:
 * "Indicadores", "7 dias", "Classificacao", "Alertas" e "Ultimos relatorios".
 */
public final class DashboardSpreadsheet {

    private static final List<Column> TREND_COLUMNS = List.of(
            new Column("Data", 12),
            new Column("Produção", 12),
            new Column("Produtividade", 13),
            new Column("Meta", 10),
            new Column("Custo de ração", 14),
            new Column("Custo por ovo", 13));

    private static final List<Column> ALERT_COLUMNS =
            List.of(new Column("Tipo", 12), new Column("Alerta", 44), new Column("Detalhe", 60));

    private static final List<Column> LATEST_COLUMNS = List.of(
            new Column("Data", 12),
            new Column("Hora", 8),
            new Column("Aberto por", 28),
            new Column("Ovos coletados", 12),
            new Column("Aves removidas", 12),
            new Column("Situação", 22));

    private DashboardSpreadsheet() {}

    /**
     * A planilha do painel.
     *
     * @param today o dia de hoje da granja, para o nome do arquivo e o cabecalho
     * @param now a hora de agora da granja, para o cabecalho
     */
    public static Spreadsheet of(SectorDashboard dashboard, LocalDate today, LocalTime now) {
        List<String> heading = SpreadsheetHeading.of(
                "Ovyx — Painel",
                List.of(
                        "Setor: " + dashboard.sector().name(),
                        "Período: " + DashboardSheets.periodOf(dashboard.period(), dashboard.from(), dashboard.to())),
                today,
                now);
        return new Spreadsheet(
                "painel-" + FileNames.slug(dashboard.sector().name()) + "-" + DashboardSheets.FILE_DAY.format(today)
                        + ".xlsx",
                List.of(
                        Sheet.withRows(
                                "Indicadores",
                                heading,
                                DashboardSheets.INDICATOR_COLUMNS,
                                DashboardSheets.indicatorRows(dashboard.indicators(), PendingUnit.DAY),
                                List.of()),
                        Sheet.withRows("7 dias", heading, TREND_COLUMNS, trendRows(dashboard), List.of()),
                        DashboardSheets.grading(dashboard.grades(), heading),
                        alerts(dashboard, heading),
                        latest(dashboard, heading)));
    }

    // ---------------------------------------------------------------- 7 dias

    private static List<List<Cell>> trendRows(SectorDashboard dashboard) {
        List<List<Cell>> rows = new ArrayList<>();
        for (DashboardDay day : dashboard.trend()) {
            rows.add(List.of(
                    Cell.date(day.date()),
                    day.production() == null ? Cell.blank() : Cell.count(day.production()),
                    Cell.number(day.layingRate(), CellFormat.PERCENT_2),
                    Cell.number(dashboard.target(), CellFormat.PERCENT_2),
                    Cell.number(day.feedCost(), CellFormat.MONEY),
                    Cell.number(day.costPerEgg(), CellFormat.MONEY_3)));
        }
        return rows;
    }

    // ---------------------------------------------------------------- Alertas

    private static Sheet alerts(SectorDashboard dashboard, List<String> heading) {
        if (dashboard.alerts().isEmpty()) {
            return Sheet.empty("Alertas", heading, ALERT_COLUMNS, "Nenhum alerta aberto");
        }
        List<List<Cell>> rows = dashboard.alerts().stream()
                .map(alert -> List.of(
                        Cell.text(alert.tone() == AlertTone.WARNING ? "Atenção" : "Informação"),
                        Cell.text(alert.title()),
                        Cell.text(alert.detail())))
                .toList();
        return Sheet.withRows("Alertas", heading, ALERT_COLUMNS, rows, List.of());
    }

    // ---------------------------------------------------------------- Ultimos relatorios

    private static Sheet latest(SectorDashboard dashboard, List<String> heading) {
        if (dashboard.latestReports().isEmpty()) {
            return Sheet.empty("Últimos relatórios", heading, LATEST_COLUMNS, "Nenhum relatório no setor");
        }
        List<List<Cell>> rows = dashboard.latestReports().stream()
                .map(report -> List.of(
                        Cell.date(report.collectionDate()),
                        Cell.time(report.collectionTime()),
                        Cell.text(report.openedBy().name()),
                        Cell.count(report.collectedEggs()),
                        Cell.count(report.removedBirds()),
                        Cell.text(DashboardSheets.situationOf(
                                report.productionStatus(), report.feedStatus(), report.mortalityStatus()))))
                .toList();
        return Sheet.withRows("Últimos relatórios", heading, LATEST_COLUMNS, rows, List.of());
    }
}
