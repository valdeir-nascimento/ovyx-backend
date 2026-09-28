package io.github.ovyx.production.application.dashboard;

import java.math.BigDecimal;

/**
 * A meta de produtividade: 85%, a mesma para todos os setores (FR-011 da 006, decidido com o usuario na
 * especificacao). O grafico da produtividade diaria a marca, e o alerta de baixa postura a usa.
 */
public final class LayingRateTarget {

    /** A meta, em porcentagem, com duas casas como a produtividade. */
    public static final BigDecimal VALUE = new BigDecimal("85.00");

    private LayingRateTarget() {}
}
