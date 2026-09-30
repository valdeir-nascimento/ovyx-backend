package io.github.ovyx.architecture.violation.application;

import org.dhatim.fastexcel.Worksheet;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Montador da camada de aplicacao que escreve direto na aba da biblioteca de planilha. O conteudo da
 * planilha e montado no modelo do {@code shared} e gravado pela porta (R-003 da 007). Existe apenas para
 * {@link io.github.ovyx.architecture.ArchitectureRulesSelfCheckTest} comprovar que a suite de arquitetura
 * reprova esta situacao.
 */
public final class SpreadsheetInApplication {

    private SpreadsheetInApplication() {}

    public static void writeTitle(Worksheet sheet) {
        sheet.value(0, 0, "Ovyx");
    }
}
