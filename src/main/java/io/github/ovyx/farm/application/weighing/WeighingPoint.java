package io.github.ovyx.farm.application.weighing;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Um ponto do grafico da evolucao do peso: o dia e o peso medio, em gramas. */
public record WeighingPoint(LocalDate weighedOn, BigDecimal averageWeight) {}
