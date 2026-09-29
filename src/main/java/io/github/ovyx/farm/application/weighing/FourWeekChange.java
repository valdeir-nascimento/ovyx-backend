package io.github.ovyx.farm.application.weighing;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A variacao do peso em 4 semanas (FR-009 da 005): a ultima pesagem menos a pesagem mais recente feita ate
 * 28 dias antes dela.
 *
 * @param change a variacao, em gramas, com uma casa
 * @param since o dia da pesagem de referencia
 */
public record FourWeekChange(BigDecimal change, LocalDate since) {}
