package io.github.ovyx.farm.domain.model;

import io.github.ovyx.farm.domain.valueobject.ReferenceWeight;
import java.math.BigDecimal;

/**
 * Um peso diante da faixa de referencia do setor (FR-009 da 005): dentro dela, com os limites incluidos;
 * fora dela; ou sem faixa, quando o setor nao tem.
 */
public enum WeightRangeStatus {
    WITHIN,
    OUTSIDE,
    NO_RANGE;

    /**
     * A situacao do peso diante da faixa (R-009 da 007), a regra que a tela Peso medio e a planilha das gaiolas
     * usam: os limites contam como dentro da faixa.
     *
     * @param range a faixa do setor; {@code null} quando o setor nao tem
     * @param weight o peso medio; {@code null} quando a gaiola nunca foi pesada
     * @return {@code null} sem peso, porque nao ha o que situar
     */
    public static WeightRangeStatus of(ReferenceWeight range, BigDecimal weight) {
        if (weight == null) {
            return null;
        }
        if (range == null) {
            return NO_RANGE;
        }
        return range.contains(weight) ? WITHIN : OUTSIDE;
    }
}
