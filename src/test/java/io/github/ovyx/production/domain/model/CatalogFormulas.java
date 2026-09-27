package io.github.ovyx.production.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/** As fórmulas do catálogo como o production as vê, com os dados dos exemplos da spec da 004. */
public final class CatalogFormulas {

    public static final FeedFormulaId POSTURA_PLUS_ID =
            FeedFormulaId.of(UUID.fromString("4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11"));
    public static final FeedFormulaId RECRIA_ID =
            FeedFormulaId.of(UUID.fromString("8a0c2e4a-6c8e-4a0c-8e2a-4c6e8a0c2e22"));

    /** R$ 2,85 o quilo, 28 g por ave ao dia, ativa. */
    public static final CatalogFormula POSTURA_PLUS =
            new CatalogFormula(POSTURA_PLUS_ID, "Postura Plus", true, new BigDecimal("2.85"), 28);

    /** R$ 3,10 o quilo, 24 g por ave ao dia, ativa. */
    public static final CatalogFormula RECRIA = new CatalogFormula(RECRIA_ID, "Recria", true, new BigDecimal("3.10"), 24);

    /** A Recria depois de inativada. */
    public static final CatalogFormula RECRIA_INACTIVE =
            new CatalogFormula(RECRIA_ID, "Recria", false, new BigDecimal("3.10"), 24);

    private CatalogFormulas() {}
}
