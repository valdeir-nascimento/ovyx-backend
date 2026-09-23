package io.github.ovyx.identity.presentation.authentication;

import io.github.ovyx.identity.application.authentication.AuthenticatedCaretaker;
import io.github.ovyx.identity.domain.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Responsável autenticado")
public record AuthenticatedCaretakerResponse(
    @Schema(example = "7c1f0b2e-3d4a-4f5b-8c9d-0e1f2a3b4c5d") UUID id,
    @Schema(example = "Maria Silva") String fullName,
    @Schema(example = "USER") Role role,
    @Schema(
        description = "Quando verdadeiro, só a troca de senha, a saída, a consulta da própria "
            + "identidade e uma nova entrada são aceitas até que a troca seja concluída",
        example = "false")
    boolean mustChangePassword) {

    public static AuthenticatedCaretakerResponse from(AuthenticatedCaretaker model) {
        return new AuthenticatedCaretakerResponse(
            model.id().value(), model.fullName(), model.role(), model.mustChangePassword());
    }
}
