package io.github.ovyx.farm.application.formula;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura das formulas: monta os modelos de leitura sem carregar o agregado.
 *
 * <p>Fica na aplicacao, e nao no dominio, porque serve as consultas, e nao as regras.
 */
public interface FeedFormulaDirectory {

    /** As formulas da situacao pedida, por nome, sem distinguir maiusculas. */
    List<FeedFormulaSummary> list(StatusFilter status);

    Optional<FeedFormulaSummary> findById(FeedFormulaId id);
}
