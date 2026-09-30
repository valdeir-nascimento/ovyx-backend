package io.github.ovyx.shared.application.spreadsheet;

import java.util.Arrays;
import java.util.Objects;

/**
 * A planilha gravada: o nome do arquivo e o conteudo em .xlsx. E o resultado das tres exportacoes (R-005 da
 * 007).
 *
 * <p>Igualdade e texto pelo conteudo do arquivo, e nao pela referencia do array.
 */
public record SpreadsheetFile(String fileName, byte[] content) {

    public SpreadsheetFile {
        Objects.requireNonNull(fileName, "nome do arquivo");
        Objects.requireNonNull(content, "conteudo");
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SpreadsheetFile file
                && fileName.equals(file.fileName)
                && Arrays.equals(content, file.content);
    }

    @Override
    public int hashCode() {
        return 31 * fileName.hashCode() + Arrays.hashCode(content);
    }

    @Override
    public String toString() {
        return "SpreadsheetFile[fileName=" + fileName + ", bytes=" + content.length + "]";
    }
}
