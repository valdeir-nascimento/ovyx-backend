package io.github.ovyx.farm.fixtures;

import io.github.ovyx.farm.domain.model.FeedFormula;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.farm.domain.port.FeedFormulaRepository;
import io.github.ovyx.farm.domain.valueobject.FeedFormulaName;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dublê em memória do repositório de fórmulas, para os testes do domínio e dos casos de uso.
 *
 * <p>Fiel ao adaptador real: o nome em uso é o de qualquer outra fórmula, ativa ou inativa, comparado
 * sem maiúsculas (R-011 da 004).
 */
public final class InMemoryFeedFormulaRepository implements FeedFormulaRepository {

    private final Map<FeedFormulaId, FeedFormula> stored = new LinkedHashMap<>();
    private int saves;

    @Override
    public void save(FeedFormula formula) {
        stored.put(formula.id(), formula);
        saves++;
    }

    @Override
    public Optional<FeedFormula> findById(FeedFormulaId id) {
        return Optional.ofNullable(stored.get(id));
    }

    @Override
    public boolean nameInUse(FeedFormulaName name, FeedFormulaId exceptId) {
        return stored.values().stream()
                .filter(formula -> !formula.id().equals(exceptId))
                .anyMatch(formula -> formula.name().sameAs(name));
    }

    /** Quantas gravações houve: é como o teste prova que uma recusa não gravou nada. */
    public int saves() {
        return saves;
    }
}
