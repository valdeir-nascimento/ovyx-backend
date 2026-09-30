package io.github.ovyx.shared.application.spreadsheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** As regras da aba, da coluna e da planilha (data-model §1 da 007). */
@DisplayName("Sheet, Column and Spreadsheet")
class SheetTest {

    private static final List<String> HEADING = List.of("Ovyx — Gaiolas", "Setor: Codornas — Galpão 1");
    private static final List<Column> COLUMNS = List.of(new Column("Gaiola", 10), new Column("Aves", 8));
    private static final List<List<Cell>> ROWS = List.of(List.of(Cell.text("A-01"), Cell.count(50)));

    @Test
    @DisplayName("keeps a sheet with rows, one cell per column")
    void givenOneCellPerColumn_whenBuildingTheSheet_thenKeepTheRows() {
        // given
        List<List<Cell>> rows = ROWS;

        // when
        Sheet sheet = Sheet.withRows("Gaiolas", HEADING, COLUMNS, rows, List.of());

        // then
        assertThat(sheet.rows()).hasSize(1);
        assertThat(sheet.emptyNotice()).isNull();
        assertThat(sheet.totals()).isEmpty();
    }

    @Test
    @DisplayName("refuses a row with fewer cells than columns")
    void givenRowShorterThanTheColumns_whenBuildingTheSheet_thenRefuse() {
        // given
        List<List<Cell>> rows = List.of(List.of(Cell.text("A-01")));

        // when
        ThrowingCallable building = () -> Sheet.withRows("Gaiolas", HEADING, COLUMNS, rows, List.of());

        // then
        assertThatThrownBy(building).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("refuses totals with fewer cells than columns")
    void givenTotalsShorterThanTheColumns_whenBuildingTheSheet_thenRefuse() {
        // given
        List<Cell> totals = List.of(Cell.text("Total"));

        // when
        ThrowingCallable building = () -> Sheet.withRows("Gaiolas", HEADING, COLUMNS, ROWS, totals);

        // then
        assertThatThrownBy(building).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("refuses a sheet without rows and without the notice of no data")
    void givenNoRowsAndNoNotice_whenBuildingTheSheet_thenRefuse() {
        // given
        List<List<Cell>> rows = List.of();

        // when
        ThrowingCallable building = () -> Sheet.withRows("Gaiolas", HEADING, COLUMNS, rows, List.of());

        // then
        assertThatThrownBy(building).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("keeps an empty sheet with the notice of no data")
    void givenNotice_whenBuildingAnEmptySheet_thenKeepTheNotice() {
        // given
        String notice = "Nenhuma gaiola com esses filtros";

        // when
        Sheet sheet = Sheet.empty("Gaiolas", HEADING, COLUMNS, notice);

        // then
        assertThat(sheet.rows()).isEmpty();
        assertThat(sheet.emptyNotice()).isEqualTo(notice);
    }

    @Test
    @DisplayName("refuses an empty sheet with a blank notice")
    void givenBlankNotice_whenBuildingAnEmptySheet_thenRefuse() {
        // given
        String notice = "  ";

        // when
        ThrowingCallable building = () -> Sheet.empty("Gaiolas", HEADING, COLUMNS, notice);

        // then
        assertThatThrownBy(building).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("accepts a name of 31 characters, the most Excel allows")
    void givenNameOf31Characters_whenBuildingTheSheet_thenKeepIt() {
        // given
        String name = "a".repeat(31);

        // when
        Sheet sheet = Sheet.withRows(name, HEADING, COLUMNS, ROWS, List.of());

        // then
        assertThat(sheet.name()).hasSize(31);
    }

    @ParameterizedTest(name = "given the name \"{0}\" then refuse")
    @ValueSource(strings = {"", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "Custo[1]", "a:b", "a*b", "a?b", "a/b", "a\\b"})
    @DisplayName("refuses a name Excel does not accept")
    void givenNameExcelRefuses_whenBuildingTheSheet_thenRefuse(String name) {
        // given
        String sheetName = name;

        // when
        ThrowingCallable building = () -> Sheet.withRows(sheetName, HEADING, COLUMNS, ROWS, List.of());

        // then
        assertThatThrownBy(building).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "given the width {0} then refuse")
    @ValueSource(ints = {5, 61})
    @DisplayName("refuses a column narrower than 6 or wider than 60 characters")
    void givenWidthOutOfBounds_whenBuildingTheColumn_thenRefuse(int width) {
        // given
        int columnWidth = width;

        // when
        ThrowingCallable building = () -> new Column("Aves", columnWidth);

        // then
        assertThatThrownBy(building).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "given the width {0} then keep it")
    @ValueSource(ints = {6, 60})
    @DisplayName("accepts the widths of 6 and 60 characters")
    void givenWidthOnTheBounds_whenBuildingTheColumn_thenKeepIt(int width) {
        // given
        int columnWidth = width;

        // when
        Column column = new Column("Aves", columnWidth);

        // then
        assertThat(column.width()).isEqualTo(columnWidth);
    }

    @Test
    @DisplayName("turns the missing values into blank cells")
    void givenMissingValues_whenBuildingTheCells_thenAnswerBlank() {
        // given
        String noText = null;
        BigDecimal noNumber = null;

        // when
        Cell text = Cell.text(noText);
        Cell number = Cell.number(noNumber, CellFormat.MONEY);
        Cell date = Cell.date(null);
        Cell time = Cell.time(null);

        // then
        assertThat(List.of(text, number, date, time)).allMatch(Cell.Blank.class::isInstance);
    }

    @Test
    @DisplayName("refuses a spreadsheet without sheets")
    void givenNoSheets_whenBuildingTheSpreadsheet_thenRefuse() {
        // given
        List<Sheet> sheets = List.of();

        // when
        ThrowingCallable building = () -> new Spreadsheet("gaiolas-codornas-galpao-1.xlsx", sheets);

        // then
        assertThatThrownBy(building).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("refuses a file name that is not of an xlsx")
    void givenNameWithoutTheExtension_whenBuildingTheSpreadsheet_thenRefuse() {
        // given
        List<Sheet> sheets = List.of(Sheet.withRows("Gaiolas", HEADING, COLUMNS, ROWS, List.of()));

        // when
        ThrowingCallable building = () -> new Spreadsheet("gaiolas-codornas-galpao-1.csv", sheets);

        // then
        assertThatThrownBy(building).isInstanceOf(IllegalArgumentException.class);
    }
}
