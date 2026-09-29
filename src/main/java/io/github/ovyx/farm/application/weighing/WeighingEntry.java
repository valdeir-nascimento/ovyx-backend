package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.WeighingId;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma pesagem valida, como a leitura a traz do banco, antes do calculo do acompanhamento.
 *
 * @param averageWeight o peso medio, em gramas, com uma casa
 * @param lastCorrectedBy quem corrigiu por ultimo, ou {@code null} sem correcao
 */
public record WeighingEntry(
        WeighingId id, LocalDate weighedOn, BigDecimal averageWeight, Actor recordedBy, Actor lastCorrectedBy) {}
