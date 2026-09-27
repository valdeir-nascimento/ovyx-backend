package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.shared.application.Query;
import java.util.List;

/**
 * Lista de setores com os totais (FR-004, FR-005).
 *
 * @param status situacao dos setores listados; ausente, so os ativos
 */
public record ListSectorsQuery(StatusFilter status) implements Query<List<SectorSummary>> {}
