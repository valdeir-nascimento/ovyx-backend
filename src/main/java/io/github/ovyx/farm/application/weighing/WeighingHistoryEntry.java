package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.WeighingId;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma pesagem no historico, com a variacao sobre a anterior.
 *
 * @param change o peso desta pesagem menos o da anterior, em gramas, com uma casa; {@code null} na primeira
 * @param lastCorrectedBy quem corrigiu por ultimo, ou {@code null} sem correcao
 */
public record WeighingHistoryEntry(
        WeighingId id,
        LocalDate weighedOn,
        BigDecimal averageWeight,
        BigDecimal change,
        Actor recordedBy,
        Actor lastCorrectedBy) {}
