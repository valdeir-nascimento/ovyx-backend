package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.domain.valueobject.ReferenceWeight;

/**
 * O setor da gaiola pesada, como a tela Peso medio o mostra.
 *
 * @param referenceWeight a faixa de peso de referencia, ou {@code null} quando o setor nao tem
 */
public record WeighedSector(SectorId id, String name, Status status, ReferenceWeight referenceWeight) {}
