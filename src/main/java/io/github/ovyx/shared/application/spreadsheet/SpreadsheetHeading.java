package io.github.ovyx.shared.application.spreadsheet;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * O cabecalho de cada aba (R-012 da 007): o titulo, as linhas de quem monta (o setor, o periodo ou os filtros)
 * e quando a planilha foi gerada, no horario da granja.
 */
public final class SpreadsheetHeading {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private SpreadsheetHeading() {}

    public static List<String> of(String title, List<String> lines, LocalDate day, LocalTime time) {
        List<String> heading = new ArrayList<>();
        heading.add(title);
        heading.addAll(lines);
        heading.add("Gerada em " + DAY.format(day) + " às " + TIME.format(time) + " (horário da granja)");
        return List.copyOf(heading);
    }
}
