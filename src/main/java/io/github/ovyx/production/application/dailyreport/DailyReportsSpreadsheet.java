package io.github.ovyx.production.application.dailyreport;

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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * A planilha dos relatorios de um intervalo (US1 da 007; data-model §2), montada sem banco a partir dos detalhes
 * dos relatorios.
 *
 * <p>A aba "Relatorios" tem uma linha por relatorio, do mais antigo para o mais novo, com os totais do proprio
 * detalhe ({@link DailyReportTotals}), e a linha de totais do intervalo com as contas de periodo
 * ({@link PeriodTotals}). A aba "Gaiolas" tem uma linha por gaiola de cada relatorio. O custo de um relatorio com
 * a racao pendente fica em branco, como o painel o deixa fora do custo: com a racao pela metade, o custo do dia
 * nao e o custo do dia.
 */
public final class DailyReportsSpreadsheet {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FILE_DAY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final BigDecimal GRAMS_PER_KILOGRAM = BigDecimal.valueOf(1000);
    private static final String REPORTS = "Relatórios";
    private static final String CAGES = "Gaiolas";

    private static final List<Column> REPORT_COLUMNS = List.of(
            new Column("Data", 12),
            new Column("Hora", 8),
            new Column("Aberto por", 28),
            new Column("Idade do lote (semanas)", 12),
            new Column("Aves no início do dia", 12),
            new Column("Ovos coletados", 12),
            new Column("Produtividade", 13),
            new Column("Ovos padrão", 12),
            new Column("Pequenos", 10),
            new Column("Jumbo", 10),
            new Column("Sujos", 10),
            new Column("Trincados", 10),
            new Column("Com sangue", 10),
            new Column("Anormais", 10),
            new Column("Mortes", 10),
            new Column("Descartes", 10),
            new Column("Aves removidas", 12),
            new Column("Saldo de aves", 12),
            new Column("Consumo de ração (kg)", 13),
            new Column("Custo de ração", 14),
            new Column("Custo por ovo", 13),
            new Column("Produção", 20),
            new Column("Ração", 22),
            new Column("Mortalidade", 13),
            new Column("Observação", 40));

    private static final List<Column> CAGE_COLUMNS = List.of(
            new Column("Data", 12),
            new Column("Gaiola", 10),
            new Column("Bateria", 9),
            new Column("Número", 9),
            new Column("Aves", 9),
            new Column("Ovos coletados", 12),
            new Column("Pequenos", 10),
            new Column("Jumbo", 10),
            new Column("Sujos", 10),
            new Column("Trincados", 10),
            new Column("Com sangue", 10),
            new Column("Anormais", 10),
            new Column("Mortes", 10),
            new Column("Descartes", 10),
            new Column("Observação da mortalidade", 36),
            new Column("Fórmula", 24),
            new Column("Consumo (g)", 12),
            new Column("Custo", 12));

    private DailyReportsSpreadsheet() {}

    /**
     * A planilha dos relatorios.
     *
     * @param reports os relatorios do intervalo, em qualquer ordem
     * @param today o dia de hoje da granja, para o cabecalho
     * @param now a hora de agora da granja, para o cabecalho
     */
    public static Spreadsheet of(
            ReportingSector sector,
            LocalDate from,
            LocalDate to,
            List<DailyReportDetail> reports,
            LocalDate today,
            LocalTime now) {
        List<String> heading = SpreadsheetHeading.of(
                "Ovyx — Relatórios diários",
                List.of("Setor: " + sector.name(), "Período: " + DAY.format(from) + " a " + DAY.format(to)),
                today,
                now);
        String fileName = "relatorios-" + FileNames.slug(sector.name()) + "-" + FILE_DAY.format(from) + "-a-"
                + FILE_DAY.format(to) + ".xlsx";
        if (reports.isEmpty()) {
            String notice = "Nenhum relatório de " + DAY.format(from) + " a " + DAY.format(to);
            return new Spreadsheet(
                    fileName,
                    List.of(
                            Sheet.empty(REPORTS, heading, REPORT_COLUMNS, notice),
                            Sheet.empty(CAGES, heading, CAGE_COLUMNS, notice)));
        }
        List<DailyReportDetail> ordered = reports.stream()
                .sorted(Comparator.comparing(DailyReportDetail::collectionDate))
                .toList();
        List<List<Cell>> cageRows = cageRows(ordered);
        // O relatorio de um setor aberto sem gaiola ativa nao tem gaiola: a aba diz isso, em vez de ficar vazia.
        Sheet cages = cageRows.isEmpty()
                ? Sheet.empty(
                        CAGES,
                        heading,
                        CAGE_COLUMNS,
                        "Nenhuma gaiola nos relatórios de " + DAY.format(from) + " a " + DAY.format(to))
                : Sheet.withRows(CAGES, heading, CAGE_COLUMNS, cageRows, List.of());
        return new Spreadsheet(
                fileName,
                List.of(
                        Sheet.withRows(
                                REPORTS,
                                heading,
                                REPORT_COLUMNS,
                                ordered.stream().map(DailyReportsSpreadsheet::reportRow).toList(),
                                totalsRow(ordered)),
                        cages));
    }

    // ---------------------------------------------------------------- aba Relatorios

    private static List<Cell> reportRow(DailyReportDetail report) {
        boolean fed = report.feed().status() == FeedStatus.COMPLETE;
        List<Cell> row = new ArrayList<>();
        row.add(Cell.date(report.collectionDate()));
        row.add(Cell.time(report.collectionTime()));
        row.add(Cell.text(report.openedBy().name()));
        row.add(Cell.count(report.flockAge()));
        row.add(Cell.count(report.openingBirdCount()));
        row.add(Cell.count(report.production().collectedEggs()));
        row.add(Cell.number(report.production().layingRate(), CellFormat.PERCENT_2));
        row.add(Cell.count(report.production().standardEggs()));
        row.addAll(grades(List.of(report)));
        row.add(Cell.count(report.mortality().deaths()));
        row.add(Cell.count(report.mortality().culls()));
        row.add(Cell.count(report.mortality().deaths() + report.mortality().culls()));
        row.add(Cell.count(report.mortality().closingBirdCount()));
        row.add(Cell.number(kilograms(report.feed().consumption()), CellFormat.KILOGRAMS));
        row.add(fed ? Cell.number(report.feed().cost(), CellFormat.MONEY) : Cell.blank());
        row.add(fed ? Cell.number(report.feed().costPerEgg(), CellFormat.MONEY_3) : Cell.blank());
        row.add(Cell.text(situation(report.production().status() == ProductionStatus.COMPLETE, report.production().pendingCages())));
        row.add(Cell.text(situation(fed, report.feed().pendingCages())));
        row.add(Cell.text(report.mortality().status() == MortalityStatus.RECORDED ? "Lançada" : "Pendente"));
        row.add(Cell.text(report.note()));
        return row;
    }

    private static List<Cell> totalsRow(List<DailyReportDetail> reports) {
        PeriodTotals totals = PeriodTotals.of(reports.stream().map(DailyReportsSpreadsheet::periodDay).toList());
        int deaths = sum(reports, report -> report.mortality().deaths());
        int culls = sum(reports, report -> report.mortality().culls());
        List<Cell> row = new ArrayList<>();
        row.add(Cell.text("Total"));
        row.add(Cell.blank());
        row.add(Cell.blank());
        row.add(Cell.blank());
        row.add(Cell.count(sum(reports, DailyReportDetail::openingBirdCount)));
        row.add(Cell.count(totals.eggs()));
        row.add(Cell.number(totals.layingRate(), CellFormat.PERCENT_2));
        row.add(Cell.count(sum(reports, report -> report.production().standardEggs())));
        row.addAll(grades(reports));
        row.add(Cell.count(deaths));
        row.add(Cell.count(culls));
        row.add(Cell.count(deaths + culls));
        row.add(Cell.blank());
        row.add(Cell.number(kilograms(sum(reports, report -> report.feed().consumption())), CellFormat.KILOGRAMS));
        row.add(Cell.number(totals.feedCost(), CellFormat.MONEY));
        row.add(Cell.number(totals.costPerEgg(), CellFormat.MONEY_3));
        row.add(Cell.blank());
        row.add(Cell.text(daysLeftOut(totals.incompleteDays())));
        row.add(Cell.blank());
        row.add(Cell.blank());
        return row;
    }

    /** O relatorio no que as contas do periodo precisam, com o custo exato das gaiolas com racao. */
    private static PeriodDay periodDay(DailyReportDetail report) {
        BigDecimal exactCost = report.cages().stream()
                .map(ReportCageDetail::feed)
                .filter(Objects::nonNull)
                .map(feed -> BigDecimal.valueOf(feed.consumption()).multiply(feed.pricePerKg()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PeriodDay(
                report.openingBirdCount(),
                report.production().collectedEggs(),
                exactCost,
                report.feed().status() == FeedStatus.COMPLETE);
    }

    /** As seis classes fora do padrao, somadas nas gaiolas com producao dos relatorios. */
    private static List<Cell> grades(List<DailyReportDetail> reports) {
        List<CageProduction> productions = reports.stream()
                .flatMap(report -> report.cages().stream())
                .map(ReportCageDetail::production)
                .filter(Objects::nonNull)
                .toList();
        return List.of(
                Cell.count(productions.stream().mapToInt(CageProduction::small).sum()),
                Cell.count(productions.stream().mapToInt(CageProduction::jumbo).sum()),
                Cell.count(productions.stream().mapToInt(CageProduction::dirty).sum()),
                Cell.count(productions.stream().mapToInt(CageProduction::cracked).sum()),
                Cell.count(productions.stream().mapToInt(CageProduction::bloodSpot).sum()),
                Cell.count(productions.stream().mapToInt(CageProduction::abnormal).sum()));
    }

    private static String situation(boolean complete, int pendingCages) {
        if (complete) {
            return "Completa";
        }
        return "Pendente: " + pendingCages + (pendingCages == 1 ? " gaiola" : " gaiolas");
    }

    /** Os dias fora do custo, na coluna Racao da linha de totais; nada quando todos contam. */
    private static String daysLeftOut(int days) {
        if (days == 0) {
            return null;
        }
        return days + (days == 1 ? " dia fora do custo" : " dias fora do custo");
    }

    /** O consumo em quilos com uma casa, como a aba Racao (004). */
    private static BigDecimal kilograms(int grams) {
        return BigDecimal.valueOf(grams).divide(GRAMS_PER_KILOGRAM, 1, RoundingMode.HALF_UP);
    }

    private static int sum(List<DailyReportDetail> reports, ToIntFunction<DailyReportDetail> value) {
        return reports.stream().mapToInt(value).sum();
    }

    // ---------------------------------------------------------------- aba Gaiolas

    private static List<List<Cell>> cageRows(List<DailyReportDetail> reports) {
        List<List<Cell>> rows = new ArrayList<>();
        for (DailyReportDetail report : reports) {
            for (ReportCageDetail cage : report.cages()) {
                rows.add(cageRow(report.collectionDate(), cage));
            }
        }
        return rows;
    }

    private static List<Cell> cageRow(LocalDate date, ReportCageDetail cage) {
        CageProduction production = cage.production();
        CageMortality mortality = cage.mortality();
        CageFeed feed = cage.feed();
        List<Cell> row = new ArrayList<>();
        row.add(Cell.date(date));
        row.add(Cell.text(cage.code()));
        row.add(Cell.text(cage.battery()));
        row.add(Cell.count(cage.number()));
        row.add(Cell.count(cage.birdCount()));
        if (production == null) {
            for (int blank = 0; blank < 7; blank++) {
                row.add(Cell.blank());
            }
        } else {
            row.add(Cell.count(production.eggs()));
            row.add(Cell.count(production.small()));
            row.add(Cell.count(production.jumbo()));
            row.add(Cell.count(production.dirty()));
            row.add(Cell.count(production.cracked()));
            row.add(Cell.count(production.bloodSpot()));
            row.add(Cell.count(production.abnormal()));
        }
        row.add(mortality == null ? Cell.blank() : Cell.count(mortality.deaths()));
        row.add(mortality == null ? Cell.blank() : Cell.count(mortality.culls()));
        row.add(Cell.text(mortality == null ? null : mortality.note()));
        row.add(Cell.text(feed == null ? null : feed.formulaName()));
        row.add(feed == null ? Cell.blank() : Cell.count(feed.consumption()));
        row.add(feed == null ? Cell.blank() : Cell.number(feed.cost(), CellFormat.MONEY));
        return row;
    }
}
