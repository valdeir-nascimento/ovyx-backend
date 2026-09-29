package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.shared.application.Command;

/**
 * Corrigir o dia e o peso de uma pesagem (US4 da 005). A data e o peso chegam como foram digitados, para o
 * dominio recusar no proprio campo.
 *
 * @param actor quem corrige, como esta na sessao
 */
public record CorrectWeighingCommand(
        String sectorId, String cageId, String weighingId, String weighedOn, String averageWeight, Actor actor)
        implements Command<WeighingId> {}
