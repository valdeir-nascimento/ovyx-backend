package io.github.ovyx.production.presentation.dailyreport;

import io.github.ovyx.production.domain.model.Actor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Quem fez a operacao, como estava na sessao naquele momento. */
@Schema(description = "Quem fez a operação, como estava na sessão naquele momento")
public record ActorResponse(
        @Schema(example = "1e3a5c7b-9d1f-4b3d-8e5a-7c9e1a3b5d77") UUID id,
        @Schema(example = "Marina Alves") String name) {

    public static ActorResponse from(Actor actor) {
        return actor == null ? null : new ActorResponse(actor.id(), actor.name());
    }
}
