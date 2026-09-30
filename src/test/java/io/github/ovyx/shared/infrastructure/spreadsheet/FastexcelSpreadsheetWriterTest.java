package io.github.ovyx.shared.infrastructure.spreadsheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.ovyx.shared.application.spreadsheet.Cell;
import io.github.ovyx.shared.application.spreadsheet.CellFormat;
import io.github.ovyx.shared.application.spreadsheet.Column;
import io.github.ovyx.shared.application.spreadsheet.Sheet;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * O escritor da planilha (R-010 a R-012 da 007), testado lendo de volta o arquivo que ele grava: os tipos e os
 * formatos pelo leitor da fastexcel, e o negrito, os títulos congelados, o autofiltro e as larguras pelo XML
 * do próprio .xlsx, que o leitor não expõe.
 */
@DisplayName("FastexcelSpreadsheetWriter")
class FastexcelSpreadsheetWriterTest {

    private static final List<String> HEADING = List.of(
            "Ovyx — Relatórios diários",
            "Setor: Codornas — Galpão 1",
            "Período: 01/09/2026 a 28/09/2026",
            "Gerada em 28/09/2026 às 21:40 (horário da granja)");
    /** Quatro linhas de cabeçalho, uma em branco: os títulos na linha 6 do Excel, os dados a partir da 7. */
    private static final int TITLES = 5;

    private static final int FIRST_ROW = TITLES + 1;

    private final FastexcelSpreadsheetWriter writer = new FastexcelSpreadsheetWriter();

    @ParameterizedTest(name = "given {0} then the format {2}")
    @CsvSource(
            delimiter = '|',
            value = {
                "COUNT|1740|#,##0|1740",
                "GRAMS|158.4|#,##0.0|158.4",
                "KILOGRAMS|12.3|#,##0.0|12.3",
                "MONEY|159.60|\"R$\" #,##0.00|159.60",
                "MONEY_3|0.092|\"R$\" #,##0.000|0.092",
                "PERCENT_2|87.00|0.00%|0.87",
                "PERCENT_1|94.8|0.0%|0.948",
                "POINTS|2.05|+0.00\" p.p.\";-0.00\" p.p.\"|2.05"
            })
    @DisplayName("writes each number with the format of Excel, and the percentages as fractions")
    void givenNumberInAFormat_whenWriting_thenReadTheNumberWithTheFormatOfExcel(
            CellFormat format, String value, String excelFormat, String stored) {
        // given
        Spreadsheet spreadsheet = oneCell(Cell.number(new BigDecimal(value), format));

        // when
        byte[] file = writer.write(spreadsheet);

        // then
        org.dhatim.fastexcel.reader.Cell cell = cellOf(file, 0, FIRST_ROW, 0);
        assertThat(cell.getType()).isEqualTo(CellType.NUMBER);
        assertThat(cell.asNumber()).isEqualByComparingTo(stored);
        assertThat(cell.getDataFormatString()).isEqualTo(excelFormat);
    }

    @Test
    @DisplayName("writes the date as a date of Excel")
    void givenDate_whenWriting_thenReadADateWithTheBrazilianFormat() {
        // given
        Spreadsheet spreadsheet = oneCell(Cell.date(LocalDate.of(2026, 9, 28)));

        // when
        byte[] file = writer.write(spreadsheet);

        // then
        org.dhatim.fastexcel.reader.Cell cell = cellOf(file, 0, FIRST_ROW, 0);
        assertThat(cell.getType()).isEqualTo(CellType.NUMBER);
        assertThat(cell.asDate()).isEqualTo(LocalDateTime.of(2026, 9, 28, 0, 0));
        assertThat(cell.getDataFormatString()).isEqualTo("dd/mm/yyyy");
    }

    @Test
    @DisplayName("writes the time as the fraction of the day, with hours and minutes")
    void givenTime_whenWriting_thenReadTheFractionOfTheDayWithHoursAndMinutes() {
        // given
        Spreadsheet spreadsheet = oneCell(Cell.time(LocalTime.of(21, 40)));

        // when
        byte[] file = writer.write(spreadsheet);

        // then
        org.dhatim.fastexcel.reader.Cell cell = cellOf(file, 0, FIRST_ROW, 0);
        assertThat(cell.getType()).isEqualTo(CellType.NUMBER);
        assertThat(cell.asNumber().doubleValue()).isCloseTo((21 * 60 + 40) / 1440.0, within(1e-9));
        assertThat(cell.getDataFormatString()).isEqualTo("hh:mm");
    }

    @Test
    @DisplayName("leaves the blank cell empty, and not zero")
    void givenBlank_whenWriting_thenLeaveTheCellEmpty() {
        // given
        Spreadsheet spreadsheet = oneCell(Cell.blank());

        // when
        byte[] file = writer.write(spreadsheet);

        // then
        assertThat(cellAt(rowsOf(file, 0), FIRST_ROW, 0)
                        .map(org.dhatim.fastexcel.reader.Cell::getType)
                        .orElse(CellType.EMPTY))
                .isEqualTo(CellType.EMPTY);
    }

    @ParameterizedTest(name = "given \"{0}\" then text")
    @ValueSource(strings = {"=SOMA(A1:A9)", "+1", "-2", "@cmd", "=HYPERLINK(\"http://x\")"})
    @DisplayName("keeps what someone typed as text, never as a formula")
    void givenTextThatLooksLikeAFormula_whenWriting_thenKeepItAsText(String typed) {
        // given
        Spreadsheet spreadsheet = oneCell(Cell.text(typed));

        // when
        byte[] file = writer.write(spreadsheet);

        // then
        org.dhatim.fastexcel.reader.Cell cell = cellOf(file, 0, FIRST_ROW, 0);
        assertThat(cell.getType()).isEqualTo(CellType.STRING);
        assertThat(cell.asString()).isEqualTo(typed);
        assertThat(cell.getFormula()).isNull();
        assertThat(partsOf(file).entrySet())
                .filteredOn(part -> part.getKey().startsWith("xl/worksheets/"))
                .allSatisfy(part -> assertThat(part.getValue()).doesNotContain("<f>").doesNotContain("<f "));
    }

    @Test
    @DisplayName("writes the heading, a blank line, the bold titles and then the rows")
    void givenSheetWithRows_whenWriting_thenLayTheHeadingTitlesAndRows() {
        // given
        Spreadsheet spreadsheet = reports();

        // when
        byte[] file = writer.write(spreadsheet);

        // then
        Map<Integer, Row> rows = rowsOf(file, 0);
        assertThat(IntStream.range(0, 4).mapToObj(index -> textAt(rows, index, 0))).containsExactlyElementsOf(HEADING);
        assertThat(textAt(rows, 4, 0)).isEmpty();
        assertThat(textAt(rows, TITLES, 0)).isEqualTo("Data");
        assertThat(textAt(rows, TITLES, 1)).isEqualTo("Ovos coletados");
        assertThat(rows.get(FIRST_ROW).getCellAsNumber(1)).hasValueSatisfying(eggs -> assertThat(eggs).isEqualByComparingTo("1740"));
        assertThat(rows.get(FIRST_ROW + 1).getCellAsNumber(1)).hasValueSatisfying(eggs -> assertThat(eggs).isEqualByComparingTo("1700"));
        assertThat(isBold(file, "A1")).isTrue();
        assertThat(isBold(file, "A" + (TITLES + 1))).isTrue();
        assertThat(isBold(file, "B" + (TITLES + 1))).isTrue();
        assertThat(isBold(file, "B" + (FIRST_ROW + 1))).isFalse();
    }

    @Test
    @DisplayName("writes the totals in bold after the last row")
    void givenSheetWithTotals_whenWriting_thenWriteTheTotalsInBoldAfterTheRows() {
        // given
        Spreadsheet spreadsheet = reports();

        // when
        byte[] file = writer.write(spreadsheet);

        // then
        Map<Integer, Row> rows = rowsOf(file, 0);
        Row totals = rows.get(FIRST_ROW + 2);
        assertThat(textAt(rows, FIRST_ROW + 2, 0)).isEqualTo("Total");
        assertThat(totals.getCellAsNumber(1)).hasValueSatisfying(eggs -> assertThat(eggs).isEqualByComparingTo("3440"));
        assertThat(isBold(file, "A" + (FIRST_ROW + 3))).isTrue();
        assertThat(isBold(file, "B" + (FIRST_ROW + 3))).isTrue();
    }

    @Test
    @DisplayName("freezes the sheet below the titles and filters by them")
    void givenSheetWithRows_whenWriting_thenFreezeBelowTheTitlesWithTheAutoFilter() {
        // given
        Spreadsheet spreadsheet = reports();

        // when
        byte[] file = writer.write(spreadsheet);

        // then
        String sheet = partsOf(file).get("xl/worksheets/sheet1.xml");
        assertThat(sheet).containsPattern("<pane[^>]*ySplit=\"" + (TITLES + 1) + "\"[^>]*state=\"frozen\"");
        assertThat(sheet).containsPattern("<autoFilter ref=\"A" + (TITLES + 1) + ":C" + (TITLES + 1));
    }

    @Test
    @DisplayName("gives each column the width of the model when its title fits in it")
    void givenColumnsWithWidths_whenWriting_thenWriteTheWidths() {
        // given
        Spreadsheet spreadsheet = reports();

        // when
        byte[] file = writer.write(spreadsheet);

        // then
        String sheet = partsOf(file).get("xl/worksheets/sheet1.xml");
        assertThat(sheet).containsPattern("<col min=\"1\" max=\"1\" width=\"12(\\.0+)?\"");
        assertThat(sheet).containsPattern("<col min=\"3\" max=\"3\" width=\"40(\\.0+)?\"");
    }

    /**
     * O título sai em negrito, mais largo que um caractere por letra, e o botão do autofiltro ocupa o fim da
     * célula (QA 1 e 2 da 007): a coluna fica com pelo menos o teto de 1,25 por letra, mais 6 caracteres.
     */
    @Test
    @DisplayName("widens a column to its title in bold with the button of the filter, so no title is cut")
    void givenTitleWiderThanTheColumn_whenWriting_thenWidenTheColumnToTheTitle() {
        // given
        List<Column> columns = List.of(new Column("Idade do lote (semanas)", 12), new Column("Aves", 8));
        Sheet sheet = Sheet.withRows("Relatórios", HEADING, columns, List.of(List.of(Cell.count(20), Cell.count(98))), List.of());

        // when
        byte[] file = writer.write(new Spreadsheet("relatorios.xlsx", List.of(sheet)));

        // then
        String part = partsOf(file).get("xl/worksheets/sheet1.xml");
        assertThat(part).containsPattern("<col min=\"1\" max=\"1\" width=\"35(\\.0+)?\"");
        assertThat(part).containsPattern("<col min=\"2\" max=\"2\" width=\"11(\\.0+)?\"");
        assertThat(partsOf(writer.write(reports())).get("xl/worksheets/sheet1.xml"))
                .as("\"Ovos coletados\", 14 letras, numa coluna de 16")
                .containsPattern("<col min=\"2\" max=\"2\" width=\"24(\\.0+)?\"");
    }

    @Test
    @DisplayName("writes the notice of no data in the place of the rows")
    void givenEmptySheet_whenWriting_thenWriteTheNoticeBelowTheTitles() {
        // given
        Sheet empty = Sheet.empty("Relatórios", HEADING, columns(), "Nenhum relatório de 01/08/2026 a 31/08/2026");

        // when
        byte[] file = writer.write(new Spreadsheet("relatorios.xlsx", List.of(empty)));

        // then
        Map<Integer, Row> rows = rowsOf(file, 0);
        assertThat(textAt(rows, TITLES, 0)).isEqualTo("Data");
        assertThat(textAt(rows, FIRST_ROW, 0)).isEqualTo("Nenhum relatório de 01/08/2026 a 31/08/2026");
        assertThat(rows.keySet()).allMatch(index -> index <= FIRST_ROW);
    }

    @Test
    @DisplayName("writes the sheets in order, with their names")
    void givenTwoSheets_whenWriting_thenReadBothInOrder() throws IOException {
        // given
        Sheet cages = Sheet.empty("Gaiolas", HEADING, columns(), "Nenhum relatório de 01/08/2026 a 31/08/2026");
        Spreadsheet spreadsheet = new Spreadsheet("relatorios.xlsx", List.of(reports().sheets().getFirst(), cages));

        // when
        byte[] file = writer.write(spreadsheet);

        // then
        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(file))) {
            assertThat(workbook.getSheets().map(org.dhatim.fastexcel.reader.Sheet::getName))
                    .containsExactly("Relatórios", "Gaiolas");
        }
    }

    @Test
    @DisplayName("writes and reads back a sheet of 110 thousand rows")
    void givenYearOfReportsOf300Cages_whenWriting_thenReadEveryRowBack() throws IOException {
        // given
        List<List<Cell>> rows = new ArrayList<>();
        for (int index = 0; index < 110_000; index++) {
            rows.add(List.of(Cell.date(LocalDate.of(2026, 1, 1)), Cell.count(index), Cell.text("B-07")));
        }
        Sheet sheet = Sheet.withRows("Gaiolas", HEADING, columns(), rows, List.of());

        // when
        byte[] file = writer.write(new Spreadsheet("relatorios.xlsx", List.of(sheet)));

        // then
        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(file));
                var stream = workbook.getFirstSheet().openStream()) {
            assertThat(stream.filter(row -> row.getRowNum() > FIRST_ROW).count()).isEqualTo(110_000);
        }
    }

    // ---------------------------------------------------------------- apoio

    private static List<Column> columns() {
        return List.of(new Column("Data", 12), new Column("Ovos coletados", 16), new Column("Observação", 40));
    }

    private static Spreadsheet oneCell(Cell cell) {
        Sheet sheet = Sheet.withRows(
                "Relatórios", HEADING, List.of(new Column("Valor", 12)), List.of(List.of(cell)), List.of());
        return new Spreadsheet("relatorios.xlsx", List.of(sheet));
    }

    private static Spreadsheet reports() {
        List<List<Cell>> rows = List.of(
                List.of(Cell.date(LocalDate.of(2026, 9, 27)), Cell.count(1740), Cell.text("Sem ocorrência")),
                List.of(Cell.date(LocalDate.of(2026, 9, 28)), Cell.count(1700), Cell.blank()));
        List<Cell> totals = List.of(Cell.text("Total"), Cell.count(3440), Cell.blank());
        return new Spreadsheet("relatorios.xlsx", List.of(Sheet.withRows("Relatórios", HEADING, columns(), rows, totals)));
    }

    /** As linhas da aba pelo índice a partir de zero; a linha sem célula nenhuma não aparece. */
    private static Map<Integer, Row> rowsOf(byte[] file, int sheet) {
        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(file), new ReadingOptions(true, false))) {
            Map<Integer, Row> rows = new HashMap<>();
            workbook.getSheet(sheet).orElseThrow().read().forEach(row -> rows.put(row.getRowNum() - 1, row));
            return rows;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static Optional<org.dhatim.fastexcel.reader.Cell> cellAt(Map<Integer, Row> rows, int row, int column) {
        return Optional.ofNullable(rows.get(row)).flatMap(found -> found.getOptionalCell(column));
    }

    private static String textAt(Map<Integer, Row> rows, int row, int column) {
        return cellAt(rows, row, column).map(org.dhatim.fastexcel.reader.Cell::getText).orElse("");
    }

    private static org.dhatim.fastexcel.reader.Cell cellOf(byte[] file, int sheet, int row, int column) {
        return cellAt(rowsOf(file, sheet), row, column).orElseThrow();
    }

    /**
     * As partes XML do .xlsx, pelo caminho dentro do zip. Lidas pelo diretório central, como o Excel e o
     * LibreOffice leem: o {@code ZipInputStream} lê só os cabeçalhos locais e recusa o descritor de dados que a
     * biblioteca grava depois de cada parte.
     */
    private static Map<String, String> partsOf(byte[] file) {
        Map<String, String> parts = new HashMap<>();
        try {
            Path copy = Files.createTempFile("planilha", ".xlsx");
            try {
                Files.write(copy, file);
                try (ZipFile zip = new ZipFile(copy.toFile())) {
                    for (ZipEntry entry : Collections.list(zip.entries())) {
                        try (InputStream part = zip.getInputStream(entry)) {
                            parts.put(entry.getName(), new String(part.readAllBytes(), StandardCharsets.UTF_8));
                        }
                    }
                }
            } finally {
                Files.deleteIfExists(copy);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return parts;
    }

    /** Se a célula da primeira aba usa um estilo com a fonte em negrito: célula → xf → fonte → {@code <b/>}. */
    private static boolean isBold(byte[] file, String reference) {
        Map<String, String> parts = partsOf(file);
        Matcher cell = Pattern.compile("<c r=\"" + reference + "\"(?: [^>]*)? s=\"(\\d+)\"").matcher(parts.get("xl/worksheets/sheet1.xml"));
        if (!cell.find()) {
            return false;
        }
        String styles = parts.get("xl/styles.xml");
        List<String> xfs = elementsOf(sectionOf(styles, "cellXfs"), "xf");
        Matcher font = Pattern.compile("fontId=\"(\\d+)\"").matcher(xfs.get(Integer.parseInt(cell.group(1))));
        if (!font.find()) {
            return false;
        }
        List<String> fonts = elementsOf(sectionOf(styles, "fonts"), "font");
        return fonts.get(Integer.parseInt(font.group(1))).contains("<b/>") || fonts.get(Integer.parseInt(font.group(1))).contains("<b ");
    }

    private static String sectionOf(String xml, String tag) {
        Matcher matcher = Pattern.compile("<" + tag + "[ >].*?</" + tag + ">", Pattern.DOTALL).matcher(xml);
        return matcher.find() ? matcher.group() : "";
    }

    private static List<String> elementsOf(String section, String tag) {
        List<String> elements = new ArrayList<>();
        Matcher matcher = Pattern.compile("<" + tag + "(?:[ >][^<]*?/>|[ >].*?</" + tag + ">|/>)", Pattern.DOTALL).matcher(section);
        while (matcher.find()) {
            elements.add(matcher.group());
        }
        return elements;
    }
}
