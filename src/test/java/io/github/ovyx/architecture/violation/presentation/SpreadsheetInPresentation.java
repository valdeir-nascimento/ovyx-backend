package io.github.ovyx.architecture.violation.presentation;

import java.io.OutputStream;
import org.dhatim.fastexcel.Workbook;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Classe da apresentacao que grava a planilha com a biblioteca, em vez de pedir o arquivo ao caso de uso.
 * A biblioteca de planilha fica atras da porta {@code SpreadsheetWriter}, so no {@code shared.infrastructure}
 * (R-003 da 007). Existe apenas para {@link io.github.ovyx.architecture.ArchitectureRulesSelfCheckTest}
 * comprovar que a suite de arquitetura reprova esta situacao.
 */
public final class SpreadsheetInPresentation {

    private SpreadsheetInPresentation() {}

    public static Workbook workbookOf(OutputStream output) {
        return new Workbook(output, "Ovyx", "1.0");
    }
}
