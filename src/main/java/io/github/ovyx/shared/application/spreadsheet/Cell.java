package io.github.ovyx.shared.application.spreadsheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Uma celula da planilha, tipada (data-model §1 da 007). Quem monta diz o tipo e o formato; o escritor decide
 * como gravar. Nao ha celula de formula: o texto e sempre texto (R-011).
 *
 * <p>As fabricas transformam o valor ausente em {@link Blank}, o "—" da tela, que na planilha e a celula
 * vazia, e nao zero (FR-005).
 */
public sealed interface Cell permits Cell.Text, Cell.Number, Cell.Date, Cell.Time, Cell.Blank {

    /** Texto, gravado como texto mesmo que comece com "=", "+", "-" ou "@". */
    record Text(String value) implements Cell {
        public Text {
            Objects.requireNonNull(value, "texto");
        }
    }

    /** Numero ja arredondado como na tela, com o formato do Excel. */
    record Number(BigDecimal value, CellFormat format) implements Cell {
        public Number {
            Objects.requireNonNull(value, "numero");
            Objects.requireNonNull(format, "formato");
        }
    }

    /** Data, gravada como data do Excel. */
    record Date(LocalDate value) implements Cell {
        public Date {
            Objects.requireNonNull(value, "data");
        }
    }

    /** Hora, gravada como fracao do dia do Excel. */
    record Time(LocalTime value) implements Cell {
        public Time {
            Objects.requireNonNull(value, "hora");
        }
    }

    /** O "—" da tela: a celula vazia. */
    record Blank() implements Cell {}

    static Cell text(String value) {
        return value == null || value.isEmpty() ? new Blank() : new Text(value);
    }

    static Cell count(long value) {
        return new Number(BigDecimal.valueOf(value), CellFormat.COUNT);
    }

    static Cell number(BigDecimal value, CellFormat format) {
        return value == null ? new Blank() : new Number(value, format);
    }

    static Cell date(LocalDate value) {
        return value == null ? new Blank() : new Date(value);
    }

    static Cell time(LocalTime value) {
        return value == null ? new Blank() : new Time(value);
    }

    static Cell blank() {
        return new Blank();
    }
}
