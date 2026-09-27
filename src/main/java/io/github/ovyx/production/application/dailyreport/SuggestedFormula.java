package io.github.ovyx.production.application.dailyreport;

import java.math.BigDecimal;
import java.util.UUID;

/** A formula da proposta, com o preco e o consumo esperado atuais. */
public record SuggestedFormula(UUID id, String name, BigDecimal pricePerKg, int expectedIntake) {}
