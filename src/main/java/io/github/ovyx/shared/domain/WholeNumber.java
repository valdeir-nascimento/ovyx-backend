package io.github.ovyx.shared.domain;

import java.util.OptionalInt;
import java.util.regex.Pattern;

/**
 * Leitura de um numero inteiro digitado, para as quantidades que chegam como texto cru.
 *
 * <p>O numero chega como texto, e nao como {@code int}: convertido na borda, "12.5" tornava o corpo
 * inteiro ilegivel e escondia as violacoes dos outros campos (FR-017 e R-013 da 002). Aqui ele vira
 * inteiro, ou nada; o sinal e os zeros a esquerda valem ("07" e "7" sao o mesmo numero).
 *
 * <p>Esta no nucleo compartilhado porque nao tem vocabulario de contexto: nasceu no farm, para o numero
 * e as aves da gaiola, e o production o usa para as quantidades do relatorio (feature 003).
 */
public final class WholeNumber {

    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");

    /** Acima disso, o numero ja esta fora de qualquer faixa das quantidades; nao precisa caber num {@code int}. */
    private static final int MAXIMUM_DIGITS = 9;

    private WholeNumber() {}

    /** Se o texto e um numero inteiro, qualquer que seja o tamanho. */
    public static boolean isInteger(String text) {
        return INTEGER.matcher(text.strip()).matches();
    }

    /**
     * O valor do inteiro, ou nenhum quando ele e grande demais para caber: quem chama trata como fora da
     * faixa.
     */
    public static OptionalInt valueOf(String text) {
        String digits = text.strip().replaceFirst("^[+-]", "").replaceFirst("^0+(?=\\d)", "");
        if (digits.length() > MAXIMUM_DIGITS) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Integer.parseInt(text.strip()));
    }

    /** Se o inteiro digitado fica entre os limites, inclusive. */
    public static boolean within(String text, int minimum, int maximum) {
        OptionalInt value = valueOf(text);
        return value.isPresent() && value.getAsInt() >= minimum && value.getAsInt() <= maximum;
    }
}
