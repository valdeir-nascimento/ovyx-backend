package io.github.ovyx.farm.domain.model;

/**
 * Um peso diante da faixa de referencia do setor (FR-009 da 005): dentro dela, com os limites incluidos;
 * fora dela; ou sem faixa, quando o setor nao tem.
 */
public enum WeightRangeStatus {
    WITHIN,
    OUTSIDE,
    NO_RANGE
}
