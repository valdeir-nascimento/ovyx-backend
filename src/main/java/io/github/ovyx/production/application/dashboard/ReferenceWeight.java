package io.github.ovyx.production.application.dashboard;

import java.math.BigDecimal;

/**
 * A faixa de peso de referencia do setor, como o painel a le do farm (feature 005): o minimo e o maximo, em
 * gramas, com os limites incluidos.
 */
public record ReferenceWeight(int minimum, int maximum) {

    /** Se o peso esta dentro da faixa, com os limites incluidos, como na tela Peso medio. */
    public boolean contains(BigDecimal grams) {
        return grams.compareTo(BigDecimal.valueOf(minimum)) >= 0 && grams.compareTo(BigDecimal.valueOf(maximum)) <= 0;
    }
}
