package io.github.ovyx.shared.application.spreadsheet;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Uma aba da planilha (data-model §1 da 007): o cabecalho, as colunas, as linhas, a linha de totais e o aviso
 * de sem dados.
 *
 * @param heading as linhas do cabecalho, antes dos titulos das colunas ({@link SpreadsheetHeading})
 * @param rows uma celula por coluna em cada linha
 * @param totals a linha de totais, com uma celula por coluna; vazia quando nao ha
 * @param emptyNotice o aviso no lugar das linhas, quando nao ha linha; {@code null} quando ha
 */
public record Sheet(
        String name,
        List<String> heading,
        List<Column> columns,
        List<List<Cell>> rows,
        List<Cell> totals,
        String emptyNotice) {

    /** O Excel nao aceita nome de aba com estes caracteres. */
    private static final Pattern FORBIDDEN = Pattern.compile("[\\[\\]:*?/\\\\]");

    /** Nem com mais de 31 caracteres. */
    private static final int LONGEST_NAME = 31;

    public Sheet {
        Objects.requireNonNull(name, "nome da aba");
        if (name.isEmpty() || name.length() > LONGEST_NAME || FORBIDDEN.matcher(name).find()) {
            throw new IllegalArgumentException("Nome de aba que o Excel recusa: " + name);
        }
        heading = List.copyOf(heading);
        columns = List.copyOf(columns);
        rows = rows.stream().map(List::copyOf).toList();
        totals = List.copyOf(totals);
        int width = columns.size();
        if (rows.stream().anyMatch(row -> row.size() != width)) {
            throw new IllegalArgumentException("Cada linha da aba " + name + " deve ter uma celula por coluna.");
        }
        if (!totals.isEmpty() && totals.size() != width) {
            throw new IllegalArgumentException("A linha de totais da aba " + name + " deve ter uma celula por coluna.");
        }
        if (rows.isEmpty() && (emptyNotice == null || emptyNotice.isBlank())) {
            throw new IllegalArgumentException("A aba " + name + " sem linhas precisa do aviso de sem dados.");
        }
    }

    /** A aba com linhas e, se houver, a linha de totais. */
    public static Sheet withRows(
            String name, List<String> heading, List<Column> columns, List<List<Cell>> rows, List<Cell> totals) {
        return new Sheet(name, heading, columns, rows, totals, null);
    }

    /** A aba sem dados, com o aviso no lugar das linhas. */
    public static Sheet empty(String name, List<String> heading, List<Column> columns, String emptyNotice) {
        return new Sheet(name, heading, columns, List.of(), List.of(), emptyNotice);
    }
}
