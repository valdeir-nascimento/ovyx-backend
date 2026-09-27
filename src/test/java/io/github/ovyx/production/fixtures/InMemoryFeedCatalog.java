package io.github.ovyx.production.fixtures;

import io.github.ovyx.production.domain.model.CatalogFormula;
import io.github.ovyx.production.domain.model.FeedFormulaId;
import io.github.ovyx.production.domain.port.FeedCatalog;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Dublê do catálogo de fórmulas: as fórmulas que o teste monta. */
public final class InMemoryFeedCatalog implements FeedCatalog {

    private final Map<FeedFormulaId, CatalogFormula> formulas = new LinkedHashMap<>();

    public CatalogFormula put(CatalogFormula formula) {
        formulas.put(formula.id(), formula);
        return formula;
    }

    @Override
    public Optional<CatalogFormula> formulaOf(FeedFormulaId id) {
        return Optional.ofNullable(formulas.get(id));
    }
}
