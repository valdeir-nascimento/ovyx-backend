package io.github.ovyx.shared.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * Forma do corpo de erro, para a documentacao da API.
 *
 * <p>Existe porque o corpo real e um {@code ProblemDetail} com extensoes: o springdoc publicaria
 * apenas os campos da RFC, e quem le a documentacao nao veria o {@code code} nem o {@code details} —
 * justamente o que o cliente precisa para reagir. Este record nao e usado em tempo de execucao; e a
 * descricao do que {@link ResultHttpMapper} e {@link ProblemResponses} montam.
 *
 * @param code identificador estavel da regra violada
 * @param title natureza da falha, em portugues
 * @param status codigo HTTP
 * @param detail mensagem em portugues, destinada ao usuario final
 * @param instance caminho da requisicao que produziu a recusa
 * @param details uma mensagem por campo recusado; ausente quando a recusa nao e de validacao
 */
@Schema(name = "Problem", description = "Corpo de erro em RFC 9457, com o código da regra violada.")
public record ProblemResponse(
        @Schema(example = "VALIDATION_FAILED") String code,
        @Schema(example = "Dados inválidos") String title,
        @Schema(example = "400") int status,
        @Schema(example = "A requisição contém campos inválidos.") String detail,
        @Schema(example = "/api/v1/auth/sign-in") String instance,
        @Schema(example = "{\"cpf\": \"CPF inválido.\"}") Map<String, String> details) {}
