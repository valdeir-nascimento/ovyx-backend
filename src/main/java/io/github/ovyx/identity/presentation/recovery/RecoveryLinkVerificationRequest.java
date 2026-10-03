package io.github.ovyx.identity.presentation.recovery;

import io.swagger.v3.oas.annotations.media.Schema;

/** A conferencia do link de recuperacao, como o cliente envia (feature 012). */
@Schema(name = "RecoveryLinkVerificationRequest", description = "Conferência do link de recuperação")
public record RecoveryLinkVerificationRequest(
        @Schema(
                description = "O código do link de recuperação, como veio no e-mail (depois do `#` do endereço).",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 128,
                example = "3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx")
        String token) {

    /** Nunca mostra o codigo. */
    @Override
    public String toString() {
        return "RecoveryLinkVerificationRequest[token=****]";
    }
}
