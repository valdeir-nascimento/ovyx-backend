package io.github.ovyx.farm.domain.port;

import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.WeighingId;
import java.time.LocalDate;

/**
 * As datas ja pesadas de cada gaiola (FR-004 e R-005 da 005): a pesagem pergunta aqui se o dia esta livre
 * antes de ser gravada.
 */
public interface WeighingRoster {

    /**
     * Se a gaiola ja tem outra pesagem valida, que nao {@code exceptId}, no dia. A anulada nao conta: a data
     * dela fica livre.
     */
    boolean isDayTaken(CageId cageId, LocalDate day, WeighingId exceptId);
}
