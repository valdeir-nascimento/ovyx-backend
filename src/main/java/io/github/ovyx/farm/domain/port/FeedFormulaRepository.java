package io.github.ovyx.farm.domain.port;

import io.github.ovyx.farm.domain.model.FeedFormula;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import java.util.Optional;

/**
 * Carrega e grava o agregado {@link FeedFormula}.
 *
 * <p>Nao ha exclusao: a formula e inativada, e nunca apagada.
 */
public interface FeedFormulaRepository extends FeedFormulaRoster {

    Optional<FeedFormula> findById(FeedFormulaId id);

    /**
     * Grava a formula. Se outro comando a gravou no meio-tempo, a versao nao confere e a gravacao e
     * recusada, em vez de a copia vencida apagar a mais nova.
     */
    void save(FeedFormula formula);
}
