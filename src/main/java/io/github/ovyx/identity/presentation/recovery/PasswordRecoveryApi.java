package io.github.ovyx.identity.presentation.recovery;

import io.github.ovyx.shared.presentation.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

/**
 * A documentacao das operacoes da recuperacao de senha (feature 012), implementada pelo
 * {@link PasswordRecoveryController}.
 */
@Tag(name = "Acesso")
public interface PasswordRecoveryApi {

    /** Tipo de midia do corpo de erro, comum a todas as operacoes. */
    String PROBLEM_JSON = "application/problem+json";

    @Operation(
            operationId = "requestPasswordRecovery",
            summary = "Pedir o link de recuperação da senha",
            description = "Recebe o e-mail da conta. Se ele pertencer a um responsável **ativo**, um e-mail com o link"
                    + " de recuperação é enviado depois da resposta. A resposta é **sempre** 202, sem corpo, para conta"
                    + " ativa, inativa ou inexistente, e também quando a conta já recebeu 3 links na última hora ou a"
                    + " origem passou do limite de pedidos: dizer qual caso aconteceu revelaria quem tem conta. Só o"
                    + " e-mail em formato inválido é recusado, porque não depende de nenhuma conta.")
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    schema = @Schema(implementation = PasswordRecoveryRequest.class),
                    examples = @ExampleObject(
                            name = "pedido",
                            summary = "E-mail da conta",
                            value = PasswordRecoveryExamples.REQUEST)))
    @ApiResponse(
            responseCode = "202",
            description = "Pedido recebido. Se houver uma conta ativa com esse e-mail, o link foi enviado a ele.",
            content = @Content)
    @ApiResponse(
            responseCode = "400",
            description = "E-mail ausente ou em formato inválido (`VALIDATION_FAILED`), em `details.email`.",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(
                                name = "emailAusente",
                                summary = "E-mail ausente",
                                value = PasswordRecoveryExamples.EMAIL_REQUIRED),
                        @ExampleObject(
                                name = "emailInvalido",
                                summary = "E-mail em formato inválido",
                                value = PasswordRecoveryExamples.EMAIL_MALFORMED)
                    }))
    @ApiResponse(
            responseCode = "403",
            description = "Token de proteção ausente (`CSRF_TOKEN_INVALID`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "tokenCsrfAusente",
                            value = PasswordRecoveryExamples.REQUEST_CSRF_TOKEN_INVALID)))
    @ApiResponse(
            responseCode = "415",
            description = "Corpo em formato diferente de JSON (`REQUEST_NOT_ACCEPTABLE`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = PasswordRecoveryExamples.REQUEST_UNSUPPORTED_MEDIA_TYPE)))
    @ApiResponse(
            responseCode = "500",
            description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = PasswordRecoveryExamples.REQUEST_INTERNAL_ERROR)))
    ResponseEntity<Object> requestPasswordRecovery(PasswordRecoveryRequest body, HttpServletRequest request);

    @Operation(
            operationId = "verifyRecoveryLink",
            summary = "Conferir se o link de recuperação ainda vale",
            description = "Usada pela tela de redefinição ao abrir, para dizer logo se o link não vale mais. Não troca"
                    + " nada e não gasta o link. O link usado, vencido, substituído, de conta inativa, alterado ou"
                    + " inexistente recebe a mesma recusa, sem revelar de quem era; a origem que passou do limite de"
                    + " tentativas também.")
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    schema = @Schema(implementation = RecoveryLinkVerificationRequest.class),
                    examples = @ExampleObject(
                            name = "conferencia",
                            summary = "Código do link",
                            value = PasswordRecoveryExamples.VERIFICATION)))
    @ApiResponse(responseCode = "204", description = "O link vale. A tela pode pedir a nova senha.", content = @Content)
    @ApiResponse(
            responseCode = "400",
            description = "Link usado, vencido, substituído por um mais recente, anulado por uma troca de senha ou pela"
                    + " inativação, alterado ou inexistente; ou origem acima do limite de tentativas"
                    + " (`RECOVERY_LINK_INVALID`). A resposta é a mesma em todos os casos.",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "linkInvalido",
                            value = PasswordRecoveryExamples.VERIFICATION_LINK_INVALID)))
    @ApiResponse(
            responseCode = "403",
            description = "Token de proteção ausente (`CSRF_TOKEN_INVALID`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "tokenCsrfAusente",
                            value = PasswordRecoveryExamples.VERIFICATION_CSRF_TOKEN_INVALID)))
    @ApiResponse(
            responseCode = "415",
            description = "Corpo em formato diferente de JSON (`REQUEST_NOT_ACCEPTABLE`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = PasswordRecoveryExamples.VERIFICATION_UNSUPPORTED_MEDIA_TYPE)))
    @ApiResponse(
            responseCode = "500",
            description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = PasswordRecoveryExamples.VERIFICATION_INTERNAL_ERROR)))
    ResponseEntity<Object> verifyRecoveryLink(RecoveryLinkVerificationRequest body, HttpServletRequest request);

    @Operation(
            operationId = "resetPassword",
            summary = "Definir a nova senha pelo link de recuperação",
            description = "Troca a senha da conta do link, com a política da troca da própria senha. Concluída, o link"
                    + " deixa de valer, a troca da senha provisória deixa de ser exigida, todas as sessões abertas da"
                    + " pessoa são encerradas (a próxima requisição delas responde 401 `SESSION_REVOKED`) e um e-mail"
                    + " avisa a pessoa da redefinição. Não abre sessão: a pessoa entra depois, pela entrada, com a"
                    + " senha nova. Uma sessão de outra conta que esteja no mesmo navegador não é usada nem alterada."
                    + " A senha fora da política é recusada com todas as violações de uma vez, e o link continua"
                    + " valendo.")
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    schema = @Schema(implementation = PasswordResetRequest.class),
                    examples = @ExampleObject(
                            name = "redefinicao",
                            summary = "Código do link e nova senha",
                            value = PasswordRecoveryExamples.RESET)))
    @ApiResponse(
            responseCode = "204",
            description = "Senha redefinida. A senha anterior e as sessões abertas deixam de valer.",
            content = @Content)
    @ApiResponse(
            responseCode = "400",
            description = "Nova senha fora da política (`VALIDATION_FAILED`, com o link ainda valendo) ou link que não"
                    + " vale mais (`RECOVERY_LINK_INVALID`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(
                                name = "politicaNaoAtendida",
                                summary = "Nova senha fora da política, com várias violações de uma vez",
                                value = PasswordRecoveryExamples.RESET_POLICY),
                        @ExampleObject(
                                name = "linkInvalido",
                                summary = "Link usado, vencido, substituído ou inexistente",
                                value = PasswordRecoveryExamples.RESET_LINK_INVALID)
                    }))
    @ApiResponse(
            responseCode = "403",
            description = "Token de proteção ausente (`CSRF_TOKEN_INVALID`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "tokenCsrfAusente",
                            value = PasswordRecoveryExamples.RESET_CSRF_TOKEN_INVALID)))
    @ApiResponse(
            responseCode = "415",
            description = "Corpo em formato diferente de JSON (`REQUEST_NOT_ACCEPTABLE`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = PasswordRecoveryExamples.RESET_UNSUPPORTED_MEDIA_TYPE)))
    @ApiResponse(
            responseCode = "500",
            description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = PasswordRecoveryExamples.RESET_INTERNAL_ERROR)))
    ResponseEntity<Object> resetPassword(PasswordResetRequest body, HttpServletRequest request);
}
