package io.github.ovyx.identity.presentation.account;

import io.github.ovyx.shared.presentation.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "Minha conta")
public interface OwnAccountApi {

    /**
     * Tipo de midia do corpo de erro, comum a todas as operacoes.
     */
    String PROBLEM_JSON = "application/problem+json";

    @Operation(
        summary = "Trocar a própria senha",
        description = "Exige a senha atual. Quando `mustChangePassword` é verdadeiro, só esta operação, a saída, "
            + "a consulta da própria identidade e uma nova entrada são aceitas; as demais respondem 403 "
            + "`PASSWORD_CHANGE_REQUIRED`.")
    // O sucesso e 204, sem corpo; sem o content vazio, o documento anuncia um objeto onde nao ha nada.
    @ApiResponse(
        responseCode = "204",
        description = "Senha alterada. A senha anterior deixa de funcionar.",
        content = @Content)
    @ApiResponse(
        responseCode = "400",
        description = "Senha atual incorreta ou nova senha fora da política (`VALIDATION_FAILED`)."
            + " Todas as violações vêm juntas, uma por campo, em `details`.",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(
                    name = "senhaAtualIncorretaEPoliticaNaoAtendida",
                    value = OwnAccountExamples.PASSWORD_VALIDATION_FAILED),
                @ExampleObject(
                    name = "senhaAtualAusente",
                    value = OwnAccountExamples.PASSWORD_CURRENT_MISSING)
            }))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta"
            + " (`CARETAKER_UNAVAILABLE`), caso em que a sessão é encerrada.",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semSessao", value = OwnAccountExamples.PASSWORD_UNAUTHENTICATED),
                @ExampleObject(
                    name = "responsavelIndisponivel",
                    value = OwnAccountExamples.PASSWORD_CARETAKER_UNAVAILABLE)
            }))
    @ApiResponse(
        responseCode = "403",
        description = "Token de proteção ausente ou vencido (`CSRF_TOKEN_INVALID`).",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = OwnAccountExamples.PASSWORD_CSRF_TOKEN_INVALID)))
    @ApiResponse(
        responseCode = "415",
        description = "Corpo em formato diferente de JSON (`REQUEST_NOT_ACCEPTABLE`).",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = OwnAccountExamples.PASSWORD_UNSUPPORTED_MEDIA_TYPE)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = OwnAccountExamples.PASSWORD_INTERNAL_ERROR)))
    ResponseEntity<Object> changeOwnPassword(
        ChangePasswordRequest body, HttpServletRequest request, HttpServletResponse response);
}
