package io.github.ovyx.identity.presentation.caretaker;

import io.github.ovyx.identity.domain.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Edicao de responsavel, como o cliente envia. Nao altera senha.
 *
 * <p>Sem anotacoes de validacao, pelo mesmo motivo do cadastro: todas as violacoes vem do agregado,
 * de uma vez (FR-017).
 */
@Schema(description = "Edição de responsável")
public record UpdateCaretakerRequest(
    @Schema(example = "João Pereira de Souza") String fullName,
    @Schema(description = "Apenas dígitos", example = "52998224725") String cpf,
    @Schema(example = "joao.souza@ovyx.com.br") String email,
    @Schema(description = "Apenas dígitos, com DDD", example = "91991234567") String mobilePhone,
    @Schema(example = "ADMINISTRATOR") Role role) {}
