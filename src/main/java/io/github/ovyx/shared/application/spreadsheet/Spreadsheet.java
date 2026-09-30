package io.github.ovyx.shared.application.spreadsheet;

import java.util.List;
import java.util.Objects;

/**
 * O conteudo de uma planilha do Excel, sem biblioteca (R-003 da 007): o nome do arquivo e as abas, na ordem.
 * Quem grava e a porta {@link SpreadsheetWriter}.
 */
public record Spreadsheet(String fileName, List<Sheet> sheets) {

    private static final String EXTENSION = ".xlsx";

    public Spreadsheet {
        Objects.requireNonNull(fileName, "nome do arquivo");
        if (!fileName.endsWith(EXTENSION)) {
            throw new IllegalArgumentException("O nome da planilha deve terminar em " + EXTENSION + ": " + fileName);
        }
        sheets = List.copyOf(sheets);
        if (sheets.isEmpty()) {
            throw new IllegalArgumentException("A planilha precisa de ao menos uma aba.");
        }
    }
}
