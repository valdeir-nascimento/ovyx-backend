package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.shared.application.Query;

/** O acompanhamento do peso de uma gaiola, para a tela Peso medio (US1 e US3 da 005). */
public record GetWeighingOverviewQuery(String sectorId, String cageId) implements Query<WeighingOverview> {}
