package io.github.ovyx.farm.domain.valueobject;

import java.util.OptionalInt;
import java.util.regex.Pattern;

/**
 * Leitura de um numero inteiro digitado, para as quantidades da gaiola.
 *
 * <p>O numero chega como texto, e nao como {@code int}: convertido na borda, "12.5" tornava o corpo
 * inteiro ilegivel e escondia as violacoes dos outros campos (FR-017, R-013). Aqui ele vira inteiro,
 * ou nada; o sinal e os zeros a esquerda valem ("07" e "7" sao o mesmo numero).
 */
final class WholeNumber {

    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");

    /** Acima disso, o numero ja esta fora de qualquer faixa da gaiola; nao precisa caber num {@code int}. */
    private static final int MAXIMUM_DIGITS = 9;

    private WholeNumber() {}

    /** Se o texto e um numero inteiro, qualquer que seja o tamanho. */
    static boolean isInteger(String text) {
        return INTEGER.matcher(text.strip()).matches();
    }

    /**
     * O valor do inteiro, ou nenhum quando ele e grande demais para caber: quem chama trata como fora da
     * faixa.
     */
    static OptionalInt valueOf(String text) {
        String digits = text.strip().replaceFirst("^[+-]", "").replaceFirst("^0+(?=\\d)", "");
        if (digits.length() > MAXIMUM_DIGITS) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Integer.parseInt(text.strip()));
    }

    /** Se o inteiro digitado fica entre os limites, inclusive. */
    static boolean within(String text, int minimum, int maximum) {
        OptionalInt value = valueOf(text);
        return value.isPresent() && value.getAsInt() >= minimum && value.getAsInt() <= maximum;
    }
}
