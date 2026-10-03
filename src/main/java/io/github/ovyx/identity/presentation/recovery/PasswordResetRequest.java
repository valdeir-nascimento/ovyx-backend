package io.github.ovyx.identity.presentation.recovery;

import io.swagger.v3.oas.annotations.media.Schema;

/** A redefinicao da senha pelo link, como o cliente envia (feature 012). */
@Schema(name = "PasswordResetRequest", description = "Redefinição da senha pelo link de recuperação")
public record PasswordResetRequest(
        @Schema(
                description = "O código do link de recuperação, como veio no e-mail (depois do `#` do endereço).",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 128,
                example = "3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx")
        String token,
        @Schema(
                description = "De 12 a 128 caracteres, com ao menos uma letra e um dígito, diferente do e-mail e do CPF.",
                format = "password",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 12,
                maxLength = 128,
                example = "PosturaAviario2027")
        String newPassword) {

    /** Nunca mostra o codigo nem a senha. */
    @Override
    public String toString() {
        return "PasswordResetRequest[token=****, newPassword=****]";
    }
}
