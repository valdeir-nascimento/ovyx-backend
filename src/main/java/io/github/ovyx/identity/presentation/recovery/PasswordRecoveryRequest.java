package io.github.ovyx.identity.presentation.recovery;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * O pedido do link de recuperacao, como o cliente envia (feature 012). O e-mail vem como texto, para o valor errado
 * chegar a aplicacao e ser recusado no campo.
 */
@Schema(name = "PasswordRecoveryRequest", description = "Pedido do link de recuperação da senha")
public record PasswordRecoveryRequest(
        @Schema(
                description = "O e-mail da conta, sem distinguir maiúsculas e sem os espaços das pontas.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                format = "email",
                maxLength = 254,
                example = "marina.costa@ovyx.com.br")
        String email) {}
