package io.github.ovyx.farm.presentation.weighing;

import io.github.ovyx.farm.domain.model.Actor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Quem fez a operacao, como estava na sessao. */
@Schema(name = "Actor", description = "Quem fez a operação, como estava na sessão")
public record ActorResponse(
        @Schema(example = "5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22") UUID id,
        @Schema(example = "Marina Alves") String name) {

    /** O responsavel, ou {@code null} quando nao ha. */
    public static ActorResponse from(Actor actor) {
        return actor == null ? null : new ActorResponse(actor.id(), actor.name());
    }
}
