package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.application.dashboard.DashboardSheets.PendingUnit;
import io.github.ovyx.shared.application.spreadsheet.Cell;
import io.github.ovyx.shared.application.spreadsheet.CellFormat;
import io.github.ovyx.shared.application.spreadsheet.Column;
import io.github.ovyx.shared.application.spreadsheet.Sheet;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetHeading;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A planilha da granja toda num periodo (US4 da 009), montada sem banco a partir do {@link FarmDashboard}: os mesmos
 * numeros da tela, nos formatos das planilhas da 007.
 *
 * <p>Quatro abas, cada uma com o cabecalho da granja, do periodo com a comparacao e de quando foi gerada:
 * "Indicadores", "7 dias", "Classificacao" e "Setores".
 */
public final class FarmDashboardSpreadsheet {

    private static final List<Column> TREND_COLUMNS = List.of(
            new Column("Data", 12),
            new Column("Produção", 12),
            new Column("Produtividade", 13),
            new Column("Meta", 10),
            new Column("Custo de ração", 14),
            new Column("Custo por ovo", 13),
            new Column("Setores com relatório", 12));

    private static final List<Column> SECTOR_COLUMNS = List.of(
            new Column("Setor", 28),
            new Column("Produção", 12),
            new Column("Produtividade", 13),
            new Column("Meta", 10),
            new Column("Situação", 16),
            new Column("Custo por ovo", 13),
            new Column("Relatório de hoje", 20),
            new Column("Alertas", 10));

    private FarmDashboardSpreadsheet() {}

    /**
     * A planilha da granja.
     *
     * @param today o dia de hoje da granja, para o nome do arquivo e o cabecalho
     * @param now a hora de agora da granja, para o cabecalho
     */
    public static Spreadsheet of(FarmDashboard farm, LocalDate today, LocalTime now) {
        List<String> heading = SpreadsheetHeading.of(
                "Ovyx — Painel da granja toda",
                List.of(
                        "Setores ativos: " + farm.activeSectors() + ", " + farm.reportingSectors()
                                + " com relatório no período",
                        "Período: " + DashboardSheets.periodOf(farm.period(), farm.from(), farm.to())),
                today,
                now);
        return new Spreadsheet(
                "painel-granja-" + DashboardSheets.FILE_DAY.format(today) + ".xlsx",
                List.of(
                        Sheet.withRows(
                                "Indicadores",
                                heading,
                                DashboardSheets.INDICATOR_COLUMNS,
                                DashboardSheets.indicatorRows(farm.indicators(), PendingUnit.REPORT),
                                List.of()),
                        Sheet.withRows("7 dias", heading, TREND_COLUMNS, trendRows(farm), List.of()),
                        DashboardSheets.grading(farm.grades(), heading),
                        sectors(farm, heading)));
    }

    // ---------------------------------------------------------------- 7 dias

    private static List<List<Cell>> trendRows(FarmDashboard farm) {
        List<List<Cell>> rows = new ArrayList<>();
        for (FarmDay day : farm.trend()) {
            rows.add(List.of(
                    Cell.date(day.date()),
                    day.production() == null ? Cell.blank() : Cell.count(day.production()),
                    Cell.number(day.layingRate(), CellFormat.PERCENT_2),
                    Cell.number(day.target(), CellFormat.PERCENT_2),
                    Cell.number(day.feedCost(), CellFormat.MONEY),
                    Cell.number(day.costPerEgg(), CellFormat.MONEY_3),
                    Cell.count(day.reportingSectors())));
        }
        return rows;
    }

    // ---------------------------------------------------------------- Setores

    private static Sheet sectors(FarmDashboard farm, List<String> heading) {
        if (farm.sectors().isEmpty()) {
            return Sheet.empty("Setores", heading, SECTOR_COLUMNS, "Nenhum setor ativo");
        }
        List<List<Cell>> rows = farm.sectors().stream()
                .map(row -> List.of(
                        Cell.text(row.sector().name()),
                        row.production() == null ? Cell.blank() : Cell.count(row.production()),
                        Cell.number(row.layingRate(), CellFormat.PERCENT_2),
                        Cell.number(row.target(), CellFormat.PERCENT_2),
                        row.targetStatus() == null ? Cell.blank() : Cell.text(statusOf(row.targetStatus())),
                        Cell.number(row.costPerEgg(), CellFormat.MONEY_3),
                        Cell.text(todayOf(row.todayReport())),
                        Cell.count(row.openAlerts())))
                .toList();
        return Sheet.withRows("Setores", heading, SECTOR_COLUMNS, rows, List.of());
    }

    private static String statusOf(TargetStatus status) {
        return status == TargetStatus.ABOVE ? "Acima da meta" : "Abaixo da meta";
    }

    /** Como a tela: "Completo", o primeiro lancamento que falta, ou "Nao aberto". */
    private static String todayOf(TodayReport report) {
        return report == null
                ? "Não aberto"
                : DashboardSheets.situationOf(report.productionStatus(), report.feedStatus(), report.mortalityStatus());
    }
}
