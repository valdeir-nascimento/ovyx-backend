package io.github.ovyx.identity.presentation.account;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Troca da própria senha")
public record ChangePasswordRequest(
    @Schema(description = "Senha atual", example = "GranjaNorte2026") String currentPassword,
    @Schema(description = "Nova senha: de 12 a 128 caracteres, com letra e dígito", example = "PosturaAviario2027")
    String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=****, newPassword=****]";
    }
}
