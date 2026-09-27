package io.github.ovyx.farm.application.formula;

import io.github.ovyx.shared.application.Query;

/**
 * Consulta de uma formula, ativa ou inativa.
 *
 * @param formulaId o identificador como veio no endereco; malformado e "nao encontrada"
 */
public record FindFeedFormulaQuery(String formulaId) implements Query<FeedFormulaSummary> {}
