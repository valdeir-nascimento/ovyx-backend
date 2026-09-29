package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.domain.model.WeighingId;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A ultima pesagem valida da gaiola. */
public record LatestWeighing(WeighingId id, LocalDate weighedOn, BigDecimal averageWeight) {}
