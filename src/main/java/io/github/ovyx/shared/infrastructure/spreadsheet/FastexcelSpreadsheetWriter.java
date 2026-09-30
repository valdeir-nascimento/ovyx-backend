package io.github.ovyx.shared.infrastructure.spreadsheet;

import io.github.ovyx.shared.application.spreadsheet.Cell;
import io.github.ovyx.shared.application.spreadsheet.CellFormat;
import io.github.ovyx.shared.application.spreadsheet.Column;
import io.github.ovyx.shared.application.spreadsheet.Sheet;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.dhatim.fastexcel.StyleSetter;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

/**
 * Grava a planilha em .xlsx com a fastexcel (R-002, R-010 a R-012 da 007).
 *
 * <p>Cada aba: as linhas do cabecalho, com o titulo em negrito; uma linha em branco; os titulos das colunas em
 * negrito, com o autofiltro e a planilha congelada abaixo deles; as linhas; e a linha de totais em negrito, ou
 * o aviso de sem dados no lugar das linhas.
 *
 * <p>Os numeros levam o formato do Excel do {@link CellFormat}, e as porcentagens sao gravadas como fracao (87,00%
 * vira 0,87), para o Excel somar e fazer grafico. O texto e gravado sempre como texto: nenhuma celula e formula
 * (R-011), mesmo a que comeca com "=".
 */
public final class FastexcelSpreadsheetWriter implements SpreadsheetWriter {

    private static final String APPLICATION = "Ovyx";
    private static final String VERSION = "1.0";

    private static final String DATE = "dd/mm/yyyy";
    private static final String TIME = "hh:mm";
    private static final double SECONDS_PER_DAY = 86_400d;

    /** O titulo sai em negrito: cada letra ocupa mais que a largura do digito 0, a unidade da coluna. */
    private static final double BOLD_LETTER = 1.25;

    /** O botao do autofiltro e a margem da celula, no fim do titulo, em larguras de digito. */
    private static final int FILTER_BUTTON = 6;

    private static final int WIDEST = 60;

    private static final Map<CellFormat, String> FORMATS = new EnumMap<>(Map.of(
            CellFormat.COUNT, "#,##0",
            CellFormat.GRAMS, "#,##0.0",
            CellFormat.KILOGRAMS, "#,##0.0",
            CellFormat.MONEY, "\"R$\" #,##0.00",
            CellFormat.MONEY_3, "\"R$\" #,##0.000",
            CellFormat.PERCENT_2, "0.00%",
            CellFormat.PERCENT_1, "0.0%",
            CellFormat.POINTS, "+0.00\" p.p.\";-0.00\" p.p.\""));

    @Override
    public byte[] write(Spreadsheet spreadsheet) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            Workbook workbook = new Workbook(output, APPLICATION, VERSION);
            for (Sheet sheet : spreadsheet.sheets()) {
                Worksheet worksheet = workbook.newWorksheet(sheet.name());
                write(worksheet, sheet);
                worksheet.finish();
            }
            workbook.finish();
        } catch (IOException exception) {
            throw new UncheckedIOException("Nao foi possivel gravar a planilha " + spreadsheet.fileName(), exception);
        }
        return output.toByteArray();
    }

    private static void write(Worksheet worksheet, Sheet sheet) {
        int row = 0;
        for (String line : sheet.heading()) {
            write(worksheet, row, 0, Cell.text(line), row == 0);
            row++;
        }
        int titles = row + 1;
        List<Column> columns = sheet.columns();
        for (int column = 0; column < columns.size(); column++) {
            worksheet.width(column, widthOf(columns.get(column)));
            write(worksheet, titles, column, Cell.text(columns.get(column).title()), true);
        }
        worksheet.freezePane(0, titles + 1);
        worksheet.setAutoFilter(titles, 0, columns.size() - 1);

        row = titles + 1;
        if (sheet.rows().isEmpty()) {
            worksheet.value(row, 0, sheet.emptyNotice());
            worksheet.style(row, 0).italic().set();
            return;
        }
        for (List<Cell> cells : sheet.rows()) {
            writeRow(worksheet, row, cells, false);
            row++;
        }
        if (!sheet.totals().isEmpty()) {
            writeRow(worksheet, row, sheet.totals(), true);
        }
    }

    /**
     * A largura da coluna, e ao menos a do titulo em negrito com o botao do autofiltro, para nenhum titulo sair
     * cortado no Excel (QA 1 e 2 da 007), ate a largura maxima de uma coluna do modelo. A regra cobre, com folga,
     * o que o proprio Excel pediu para os titulos das tres planilhas.
     */
    private static int widthOf(Column column) {
        int title = (int) Math.ceil(column.title().length() * BOLD_LETTER) + FILTER_BUTTON;
        return Math.min(WIDEST, Math.max(column.width(), title));
    }

    private static void writeRow(Worksheet worksheet, int row, List<Cell> cells, boolean bold) {
        for (int column = 0; column < cells.size(); column++) {
            write(worksheet, row, column, cells.get(column), bold);
        }
    }

    private static void write(Worksheet worksheet, int row, int column, Cell cell, boolean bold) {
        String format = switch (cell) {
            case Cell.Text text -> {
                worksheet.value(row, column, text.value());
                yield null;
            }
            case Cell.Number number -> {
                worksheet.value(row, column, stored(number));
                yield FORMATS.get(number.format());
            }
            case Cell.Date date -> {
                worksheet.value(row, column, date.value());
                yield DATE;
            }
            case Cell.Time time -> {
                worksheet.value(row, column, time.value().toSecondOfDay() / SECONDS_PER_DAY);
                yield TIME;
            }
            case Cell.Blank blank -> null;
        };
        if (format == null && !bold) {
            return;
        }
        StyleSetter style = worksheet.style(row, column);
        if (format != null) {
            style.format(format);
        }
        if (bold) {
            style.bold();
        }
        style.set();
    }

    /** A porcentagem vai como fracao, para o formato de porcentagem do Excel a mostrar como na tela. */
    private static BigDecimal stored(Cell.Number number) {
        return switch (number.format()) {
            case PERCENT_2, PERCENT_1 -> number.value().movePointLeft(2);
            default -> number.value();
        };
    }
}
