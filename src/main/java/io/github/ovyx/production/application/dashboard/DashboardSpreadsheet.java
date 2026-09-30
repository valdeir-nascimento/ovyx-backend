package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import io.github.ovyx.shared.application.spreadsheet.Cell;
import io.github.ovyx.shared.application.spreadsheet.CellFormat;
import io.github.ovyx.shared.application.spreadsheet.Column;
import io.github.ovyx.shared.application.spreadsheet.FileNames;
import io.github.ovyx.shared.application.spreadsheet.Sheet;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetHeading;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A planilha do painel de um setor num periodo (US2 da 007; data-model §3), montada sem banco a partir do
 * {@link SectorDashboard}: os mesmos numeros da tela, e os textos que a tela escreve (a nota dos dias
 * incompletos, a situacao dos ultimos relatorios, "sem comparacao").
 *
 * <p>Cinco abas, cada uma com o cabecalho do setor, do periodo com a comparacao e de quando foi gerada:
 * "Indicadores", "7 dias", "Classificacao", "Alertas" e "Ultimos relatorios".
 */
public final class DashboardSpreadsheet {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FILE_DAY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final String NO_COMPARISON = "sem comparação";
    private static final String COMPARED = ", comparado com ";
    private static final BigDecimal ALL = new BigDecimal("100.0");

    private static final List<Column> INDICATOR_COLUMNS = List.of(
            new Column("Indicador", 18),
            new Column("Valor", 14),
            new Column("Período anterior", 16),
            new Column("Variação", 14),
            new Column("Observação", 34));

    private static final List<Column> TREND_COLUMNS = List.of(
            new Column("Data", 12),
            new Column("Produção", 12),
            new Column("Produtividade", 13),
            new Column("Meta", 10),
            new Column("Custo de ração", 14),
            new Column("Custo por ovo", 13));

    private static final List<Column> GRADING_COLUMNS =
            List.of(new Column("Classe", 16), new Column("Quantidade", 12), new Column("% dos coletados", 15));

    private static final List<Column> ALERT_COLUMNS =
            List.of(new Column("Tipo", 12), new Column("Alerta", 44), new Column("Detalhe", 60));

    private static final List<Column> LATEST_COLUMNS = List.of(
            new Column("Data", 12),
            new Column("Hora", 8),
            new Column("Aberto por", 28),
            new Column("Ovos coletados", 12),
            new Column("Aves removidas", 12),
            new Column("Situação", 22));

    /** As classes da classificacao, na ordem da tela, com o nome em portugues. */
    private static final Map<String, String> GRADES = Map.of(
            "standard", "Padrão",
            "small", "Pequenos",
            "jumbo", "Jumbo",
            "dirty", "Sujos",
            "cracked", "Trincados",
            "bloodSpot", "Com sangue",
            "abnormal", "Anormais");

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
                List.of("Setor: " + dashboard.sector().name(), "Período: " + periodOf(dashboard)),
                today,
                now);
        return new Spreadsheet(
                "painel-" + FileNames.slug(dashboard.sector().name()) + "-" + FILE_DAY.format(today) + ".xlsx",
                List.of(
                        Sheet.withRows("Indicadores", heading, INDICATOR_COLUMNS, indicatorRows(dashboard), List.of()),
                        Sheet.withRows("7 dias", heading, TREND_COLUMNS, trendRows(dashboard), List.of()),
                        grading(dashboard, heading),
                        alerts(dashboard, heading),
                        latest(dashboard, heading)));
    }

    // ---------------------------------------------------------------- periodo

    /** "hoje, 28/09/2026, comparado com 27/09/2026", ou "7 dias, de 22/09/2026 a 28/09/2026, comparado com ...". */
    private static String periodOf(SectorDashboard dashboard) {
        long length = ChronoUnit.DAYS.between(dashboard.from(), dashboard.to()) + 1;
        LocalDate previousTo = dashboard.from().minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(length - 1);
        return switch (dashboard.period()) {
            case TODAY -> "hoje, " + DAY.format(dashboard.to()) + COMPARED + DAY.format(previousTo);
            case YESTERDAY -> "ontem, " + DAY.format(dashboard.to()) + COMPARED + DAY.format(previousTo);
            case LAST_7_DAYS -> "7 dias, de " + DAY.format(dashboard.from()) + " a " + DAY.format(dashboard.to())
                    + COMPARED + DAY.format(previousFrom) + " a " + DAY.format(previousTo);
        };
    }

    // ---------------------------------------------------------------- Indicadores

    private static List<List<Cell>> indicatorRows(SectorDashboard dashboard) {
        Indicators indicators = dashboard.indicators();
        return List.of(
                indicatorRow("Produção", indicators.production(), CellFormat.COUNT, CellFormat.PERCENT_1, "com a produção pendente"),
                indicatorRow(
                        "Produtividade", indicators.layingRate(), CellFormat.PERCENT_2, CellFormat.POINTS, "com a produção pendente"),
                indicatorRow("Custo de ração", indicators.feedCost(), CellFormat.MONEY, CellFormat.PERCENT_1, "sem ração completa"),
                indicatorRow("Custo por ovo", indicators.costPerEgg(), CellFormat.MONEY_3, CellFormat.PERCENT_1, "sem ração completa"));
    }

    private static List<Cell> indicatorRow(
            String name, Indicator indicator, CellFormat format, CellFormat changeFormat, String incomplete) {
        return List.of(
                Cell.text(name),
                Cell.number(indicator.value(), format),
                Cell.number(indicator.previous(), format),
                indicator.change() == null ? Cell.text(NO_COMPARISON) : Cell.number(indicator.change(), changeFormat),
                Cell.text(daysNote(indicator.incompleteDays(), incomplete)));
    }

    /** A nota do cartao do painel: "1 dia sem racao completa", "2 dias com a producao pendente". */
    private static String daysNote(int days, String what) {
        if (days == 0) {
            return null;
        }
        return days + (days == 1 ? " dia " : " dias ") + what;
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

    // ---------------------------------------------------------------- Classificacao

    private static Sheet grading(SectorDashboard dashboard, List<String> heading) {
        EggGrading grades = dashboard.grades();
        if (grades == null) {
            return Sheet.empty("Classificação", heading, GRADING_COLUMNS, "Nenhum ovo coletado no período");
        }
        List<List<Cell>> rows = new ArrayList<>();
        rows.add(gradeRow(grades.standard()));
        grades.shares().forEach(share -> rows.add(gradeRow(share)));
        List<Cell> totals = List.of(
                Cell.text("Coletados"), Cell.count(grades.collected()), Cell.number(ALL, CellFormat.PERCENT_1));
        return Sheet.withRows("Classificação", heading, GRADING_COLUMNS, rows, totals);
    }

    private static List<Cell> gradeRow(EggGradeShare share) {
        return List.of(
                Cell.text(GRADES.getOrDefault(share.grade(), share.grade())),
                Cell.count(share.count()),
                Cell.number(share.percent(), CellFormat.PERCENT_1));
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
                        Cell.text(situationOf(report))))
                .toList();
        return Sheet.withRows("Últimos relatórios", heading, LATEST_COLUMNS, rows, List.of());
    }

    /** Como a tela: "Completo", ou o primeiro lancamento que falta, na ordem das abas do relatorio. */
    private static String situationOf(LatestReport report) {
        if (report.productionStatus() == ProductionStatus.PENDING) {
            return "Produção pendente";
        }
        if (report.feedStatus() == FeedStatus.PENDING) {
            return "Ração pendente";
        }
        return report.mortalityStatus() == MortalityStatus.PENDING ? "Mortalidade pendente" : "Completo";
    }
}
