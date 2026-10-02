package io.github.ovyx.farm.application.sector;

import java.time.DayOfWeek;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.domain.valueobject.ReferenceWeight;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Um setor, com os totais e os instantes do cadastro e da ultima alteracao.
 *
 * @param description a descricao, ou {@code null} quando o setor nao tem
 * @param batteries   as baterias que as gaiolas do setor usam, ativas ou inativas, em ordem e sem
 *                    repetir: e o filtro de bateria da lista de gaiolas (R-013)
 * @param referenceWeight a faixa de peso de referencia, ou {@code null} sem faixa (feature 005)
 * @param layingRateTarget a meta de produtividade, em porcentagem, com uma casa (feature 008)
 * @param weighingDay o dia da pesagem, ou {@code null} quando o setor segue o prazo de 7 dias (feature 010)
 */
public record SectorDetail(
    SectorId id,
    String name,
    String description,
    Status status,
    int activeCageCount,
    int birdCount,
    List<String> batteries,
    Instant createdAt,
    Instant updatedAt,
    ReferenceWeight referenceWeight,
    BigDecimal layingRateTarget,
    DayOfWeek weighingDay
) {
}
