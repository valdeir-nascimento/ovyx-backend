package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.shared.application.Query;

/** Uma pesagem valida da gaiola (US1 e US4 da 005). */
public record FindWeighingQuery(String sectorId, String cageId, String weighingId) implements Query<WeighingDetail> {}
