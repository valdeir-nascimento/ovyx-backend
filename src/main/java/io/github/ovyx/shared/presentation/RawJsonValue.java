package io.github.ovyx.shared.presentation;

import tools.jackson.databind.JsonNode;

/**
 * O valor de um campo do JSON como texto, do jeito que veio: as quantidades chegam assim ao dominio.
 *
 * <p>Convertido para inteiro na borda, um "12.5" ou um texto tornava o corpo inteiro ilegivel e escondia
 * as violacoes dos outros campos. O dominio recebe o texto e recusa o que nao e inteiro, junto das demais
 * violacoes (FR-017 e R-013 da 002). Nasceu no {@code CageRequest} do farm e veio para o nucleo
 * compartilhado na feature 003, para o production usar tambem.
 */
public final class RawJsonValue {

    private RawJsonValue() {}

    /**
     * O valor como texto: ausente ou nulo vira {@code null}; numero inteiro, os digitos; numero com casas,
     * o decimal; texto, o proprio texto; qualquer outra coisa, o JSON dela, que o dominio recusa.
     */
    public static String of(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isString()) {
            return node.asString();
        }
        if (node.isIntegralNumber()) {
            return node.bigIntegerValue().toString();
        }
        if (node.isNumber()) {
            return node.decimalValue().toPlainString();
        }
        return node.toString();
    }
}
