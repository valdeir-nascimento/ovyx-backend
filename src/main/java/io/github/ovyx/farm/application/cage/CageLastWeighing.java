package io.github.ovyx.farm.application.cage;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A ultima pesagem valida de uma gaiola, na lista de gaiolas (FR-012 e R-009 da 005).
 *
 * @param averageWeight o peso medio, em gramas, com uma casa
 */
public record CageLastWeighing(LocalDate weighedOn, BigDecimal averageWeight) {}
