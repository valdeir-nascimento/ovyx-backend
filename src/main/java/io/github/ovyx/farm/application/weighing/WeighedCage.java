package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.Status;

/**
 * A gaiola pesada, como a tela Peso medio a mostra.
 *
 * @param code bateria, hifen e numero com ao menos dois digitos ("A-01")
 */
public record WeighedCage(CageId id, String code, String battery, int number, int birdCount, Status status) {}
