package io.github.ovyx.shared.application.spreadsheet;

/**
 * Porta que grava a planilha em .xlsx (R-003 da 007). A biblioteca fica na implementacao, no
 * {@code shared.infrastructure}; a aplicacao so conhece o modelo.
 */
public interface SpreadsheetWriter {

    /** O conteudo do arquivo .xlsx da planilha. */
    byte[] write(Spreadsheet spreadsheet);
}
