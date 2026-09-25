package io.github.ovyx.identity.presentation.caretaker;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cadastro de responsavel, como o cliente envia.
 *
 * <p>Sem anotacoes de validacao, de proposito: se a borda recusasse os campos ausentes, a pessoa
 * veria esses primeiro e as regras do dominio so na submissao seguinte. Todas as violacoes vem do
 * agregado, de uma vez (FR-017).
 */
@Schema(description = "Cadastro de responsável")
public record RegisterCaretakerRequest(
    @Schema(description = "Nome completo, de 3 a 120 caracteres", example = "João Pereira de Souza") String fullName,
    @Schema(description = "Apenas dígitos; os dígitos verificadores são conferidos", example = "52998224725")
    String cpf,
    @Schema(description = "Identificador de acesso", example = "joao.pereira@ovyx.com.br") String email,
    @Schema(description = "Apenas dígitos, com DDD; identificador de acesso", example = "91991234567")
    String mobilePhone,
    @Schema(
        description = "Senha provisória: de 12 a 128 caracteres, com letra e dígito. O responsável a troca no"
            + " primeiro acesso",
        example = "AviarioSul2026")
    String password,
    @Schema(
        description = "Perfil. Um valor fora da lista volta em details, com as demais violações",
        allowableValues = {"ADMINISTRATOR", "USER"},
        example = "USER")
    String role) {

    @Override
    public String toString() {
        return "RegisterCaretakerRequest[fullName=" + fullName + ", cpf=" + cpf + ", email=" + email
            + ", mobilePhone=" + mobilePhone + ", password=****, role=" + role + "]";
    }
}
