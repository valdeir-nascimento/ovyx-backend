package io.github.ovyx.identity.presentation.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisicao de entrada.
 *
 * <p>A validacao aqui cobre apenas a <strong>forma</strong> da requisicao: campo ausente ou
 * identificador longo demais produz 400. Se a credencial esta certa ou errada e regra de negocio, e
 * volta como 401 pelo caminho do {@code Result}, nunca por anotacao.
 *
 * <p>O limite do identificador nao e cosmetico: sem ele, um identificador acima de 254 caracteres
 * estourava a coluna da auditoria, a tentativa nao era contada nem registrada, e o erro virava uma
 * resposta distinguivel das demais.
 *
 * <p>A senha, ao contrario, <strong>nao</strong> tem limite de tamanho aqui. A violacao de
 * {@code @Size} registra o valor recusado no log de validacao do Spring, e com ele a propria senha
 * (FR-021). Uma senha longa demais nao corresponde a conta nenhuma — a politica limita as senhas
 * cadastradas a 128 caracteres — e por isso cai no caminho comum de credencial invalida. O custo
 * tambem nao cresce com o tamanho: o Argon2 condensa a senha num resumo inicial, e o trabalho caro
 * vem da memoria e das iteracoes configuradas.
 */
@Schema(description = "Credenciais de acesso")
public record SignInRequest(
        @Schema(description = "E-mail ou celular, indistintamente", example = "maria.silva@ovyx.com.br")
                @NotBlank(message = "Informe o e-mail ou o celular.")
                @Size(max = 254, message = "O identificador deve ter no máximo 254 caracteres.")
                String identifier,
        // Em branco nao e segredo: o valor recusado que o log registraria e vazio.
        @Schema(description = "Senha", example = "GranjaNorte2026") @NotBlank(message = "Informe a senha.")
                String password) {

    /** Nunca inclui a senha: o Spring MVC registra o corpo desserializado em DEBUG (FR-021). */
    @Override
    public String toString() {
        return "SignInRequest[identifier=" + identifier + ", password=****]";
    }
}
