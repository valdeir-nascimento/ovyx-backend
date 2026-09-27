package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.shared.application.Command;

/**
 * Excluir uma pesagem, que fica guardada como anulada (US4 da 005; FR-006).
 *
 * @param actor quem anula, como esta na sessao
 */
public record VoidWeighingCommand(String sectorId, String cageId, String weighingId, Actor actor)
        implements Command<WeighingId> {}
