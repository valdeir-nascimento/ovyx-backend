package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import io.github.ovyx.shared.application.spreadsheet.Cell;
import io.github.ovyx.shared.application.spreadsheet.CellFormat;
import io.github.ovyx.shared.application.spreadsheet.Column;
import io.github.ovyx.shared.application.spreadsheet.Sheet;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * O que as planilhas do painel de um setor e da granja toda repetem (R-009 da 009): o periodo com a comparacao, a aba
 * dos indicadores, a da classificacao e a situacao de um relatorio, com os textos que a tela escreve.
 */
final class DashboardSheets {

    static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter FILE_DAY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final String NO_COMPARISON = "sem comparação";
    private static final String COMPARED = ", comparado com ";
    private static final BigDecimal ALL = new BigDecimal("100.0");

    static final List<Column> INDICATOR_COLUMNS = List.of(
            new Column("Indicador", 18),
            new Column("Valor", 14),
            new Column("Período anterior", 16),
            new Column("Variação", 14),
            new Column("Observação", 34));

    private static final List<Column> GRADING_COLUMNS =
            List.of(new Column("Classe", 16), new Column("Quantidade", 12), new Column("% dos coletados", 15));

    /** As classes da classificacao, na ordem da tela, com o nome em portugues. */
    private static final Map<String, String> GRADES = Map.of(
            "standard", "Padrão",
            "small", "Pequenos",
            "jumbo", "Jumbo",
            "dirty", "Sujos",
            "cracked", "Trincados",
            "bloodSpot", "Com sangue",
            "abnormal", "Anormais");

    /** O que os pendentes contam: dias, no setor; relatorios, na granja (R-007 da 009). */
    enum PendingUnit {
        DAY("dia", "dias"),
        REPORT("relatório", "relatórios");

        private final String one;
        private final String many;

        PendingUnit(String one, String many) {
            this.one = one;
            this.many = many;
        }
    }

    private DashboardSheets() {}

    /** "hoje, 28/09/2026, comparado com 27/09/2026", ou "7 dias, de 22/09/2026 a 28/09/2026, comparado com ...". */
    static String periodOf(DashboardPeriod period, LocalDate from, LocalDate to) {
        long length = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate previousTo = from.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(length - 1);
        return switch (period) {
            case TODAY -> "hoje, " + DAY.format(to) + COMPARED + DAY.format(previousTo);
            case YESTERDAY -> "ontem, " + DAY.format(to) + COMPARED + DAY.format(previousTo);
            case LAST_7_DAYS -> "7 dias, de " + DAY.format(from) + " a " + DAY.format(to)
                    + COMPARED + DAY.format(previousFrom) + " a " + DAY.format(previousTo);
        };
    }

    /** As linhas da aba dos indicadores, com a nota dos pendentes na unidade dada. */
    static List<List<Cell>> indicatorRows(Indicators indicators, PendingUnit unit) {
        return List.of(
                indicatorRow("Produção", indicators.production(), CellFormat.COUNT, CellFormat.PERCENT_1, unit, "com a produção pendente"),
                indicatorRow(
                        "Produtividade", indicators.layingRate(), CellFormat.PERCENT_2, CellFormat.POINTS, unit, "com a produção pendente"),
                indicatorRow("Custo de ração", indicators.feedCost(), CellFormat.MONEY, CellFormat.PERCENT_1, unit, "sem ração completa"),
                indicatorRow("Custo por ovo", indicators.costPerEgg(), CellFormat.MONEY_3, CellFormat.PERCENT_1, unit, "sem ração completa"));
    }

    private static List<Cell> indicatorRow(
            String name, Indicator indicator, CellFormat format, CellFormat changeFormat, PendingUnit unit, String what) {
        return List.of(
                Cell.text(name),
                Cell.number(indicator.value(), format),
                Cell.number(indicator.previous(), format),
                indicator.change() == null ? Cell.text(NO_COMPARISON) : Cell.number(indicator.change(), changeFormat),
                Cell.text(pendingNote(indicator.incompleteDays(), unit, what)));
    }

    /** A nota do cartao do painel: "1 dia sem racao completa", "2 relatorios com a producao pendente". */
    private static String pendingNote(int count, PendingUnit unit, String what) {
        if (count == 0) {
            return null;
        }
        return count + " " + (count == 1 ? unit.one : unit.many) + " " + what;
    }

    /** A aba da classificacao, com a linha dos coletados; sem ovo, o aviso. */
    static Sheet grading(EggGrading grades, List<String> heading) {
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

    /** Como a tela: "Completo", ou o primeiro lancamento que falta, na ordem das abas do relatorio. */
    static String situationOf(ProductionStatus production, FeedStatus feed, MortalityStatus mortality) {
        if (production == ProductionStatus.PENDING) {
            return "Produção pendente";
        }
        if (feed == FeedStatus.PENDING) {
            return "Ração pendente";
        }
        return mortality == MortalityStatus.PENDING ? "Mortalidade pendente" : "Completo";
    }
}
