package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.WeighingId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Uma pesagem valida da gaiola: o que o registro e a correcao devolvem, e o que o dialogo de correcao mostra.
 *
 * @param averageWeight   o peso medio, em gramas, com uma casa
 * @param lastCorrectedBy quem corrigiu por ultimo, ou {@code null} sem correcao
 * @param lastCorrectedAt quando, ou {@code null} sem correcao
 */
public record WeighingDetail(
    WeighingId id,
    LocalDate weighedOn,
    BigDecimal averageWeight,
    Actor recordedBy,
    Instant recordedAt,
    Actor lastCorrectedBy,
    Instant lastCorrectedAt
) {
}
