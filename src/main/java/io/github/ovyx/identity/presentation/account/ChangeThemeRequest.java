package io.github.ovyx.identity.presentation.account;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Troca do proprio tema, como o cliente envia (feature 011). O tema vem como texto, para o valor errado chegar ao
 * dominio e ser recusado no campo.
 */
@Schema(name = "ChangeThemeRequest", description = "Troca do próprio tema")
public record ChangeThemeRequest(
        @Schema(
                description = "`LIGHT`, `DARK` ou `SYSTEM`. Outro valor, ou a falta dele, é recusado no campo `theme`.",
                example = "DARK")
        String theme) {}
