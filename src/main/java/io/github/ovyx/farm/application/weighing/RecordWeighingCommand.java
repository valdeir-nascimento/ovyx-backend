package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.shared.application.Command;

/**
 * Registrar a pesagem de uma gaiola (US1 da 005). A data e o peso chegam como foram digitados, para o
 * dominio recusar no proprio campo.
 *
 * @param actor quem registra, como esta na sessao
 */
public record RecordWeighingCommand(
        String sectorId, String cageId, String weighedOn, String averageWeight, Actor actor)
        implements Command<WeighingId> {}
