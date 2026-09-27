package io.github.ovyx.farm.application.formula;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.shared.application.Query;
import java.util.List;

/**
 * Lista de formulas, com o custo por ave ao dia (FR-005 da 004). Nao pagina: uma granja tem poucas
 * formulas, como os setores (R-009).
 *
 * @param status situacao das formulas listadas; ausente, so as ativas
 */
public record ListFeedFormulasQuery(StatusFilter status) implements Query<List<FeedFormulaSummary>> {}
