package io.github.ovyx.identity.presentation.authentication;

import io.github.ovyx.shared.presentation.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;


@Tag(name = "Acesso")
public interface AuthenticationApi {

    /**
     * Tipo de midia do corpo de erro, comum a todas as operacoes.
     */
    String PROBLEM_JSON = "application/problem+json";

    // Unica operacao publica: dispensa o cookie de sessao que o documento exige das demais.
    @SecurityRequirements
    @Operation(
        summary = "Entrar no sistema",
        description = "Autentica por e-mail ou celular, indistintamente, no campo `identifier`."
            + " Em caso de sucesso, devolve o cookie de sessão e a identidade do responsável.",
        requestBody =
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SignInRequest.class),
                examples = {
                    @ExampleObject(
                        name = "porEmail",
                        summary = "Entrada por e-mail",
                        value = AuthenticationExamples.SIGN_IN_BY_EMAIL),
                    @ExampleObject(
                        name = "porCelular",
                        summary = "Entrada por celular",
                        value = AuthenticationExamples.SIGN_IN_BY_MOBILE_PHONE)
                })))
    @ApiResponse(
        responseCode = "200",
        description = "Autenticado. A resposta traz o cookie de sessão e a identidade do responsável.",
        content =
        @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = AuthenticatedCaretakerResponse.class),
            examples = {
                @ExampleObject(
                    name = "usuarioComum",
                    summary = "Usuário comum",
                    value = AuthenticationExamples.SIGN_IN_COMMON_USER),
                @ExampleObject(
                    name = "administradorSemeado",
                    summary = "Administrador inicial, ainda com senha provisória",
                    value = AuthenticationExamples.SIGN_IN_SEEDED_ADMINISTRATOR)
            }))
    @ApiResponse(
        responseCode = "400",
        description = "Campo ausente, identificador longo demais ou corpo que não pôde ser lido"
            + " (`VALIDATION_FAILED` ou `REQUEST_NOT_ACCEPTABLE`).",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(
                    name = "campoAusente",
                    value = AuthenticationExamples.SIGN_IN_VALIDATION_FAILED),
                @ExampleObject(
                    name = "identificadorLongo",
                    summary = "Identificador acima de 254 caracteres",
                    value = AuthenticationExamples.SIGN_IN_IDENTIFIER_TOO_LONG),
                @ExampleObject(
                    name = "corpoMalformado",
                    summary = "Corpo que não é JSON válido",
                    value = AuthenticationExamples.SIGN_IN_UNREADABLE_BODY)
            }))
    @ApiResponse(
        responseCode = "401",
        description = "Credencial inválida (`INVALID_CREDENTIALS`). Resposta idêntica para identificador"
            + " inexistente, senha incorreta, responsável inativo e tentativas em excesso.",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = AuthenticationExamples.SIGN_IN_INVALID_CREDENTIALS)))
    @ApiResponse(
        responseCode = "403",
        description = "Token de proteção ausente ou vencido (`CSRF_TOKEN_INVALID`).",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = AuthenticationExamples.SIGN_IN_CSRF_TOKEN_INVALID)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`). A negociação recusa antes de"
            + " a credencial ser conferida: nenhuma sessão é aberta e nenhum acesso é registrado.",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = AuthenticationExamples.SIGN_IN_NOT_ACCEPTABLE)))
    @ApiResponse(
        responseCode = "415",
        description = "Corpo em formato diferente de JSON (`REQUEST_NOT_ACCEPTABLE`).",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = AuthenticationExamples.SIGN_IN_UNSUPPORTED_MEDIA_TYPE)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = AuthenticationExamples.SIGN_IN_INTERNAL_ERROR)))
    ResponseEntity<Object> signIn(SignInRequest body, HttpServletRequest request, HttpServletResponse response);

    @Operation(
        summary = "Sair do sistema",
        description = "Encerra a sessão. O cookie anterior deixa de ser aceito imediatamente.")
    // Sem corpo: sem o content vazio, o documento anuncia um objeto onde não há nada.
    @ApiResponse(responseCode = "204", description = "Sessão encerrada", content = @Content)
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`).",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = AuthenticationExamples.SIGN_OUT_UNAUTHENTICATED)))
    @ApiResponse(
        responseCode = "403",
        description = "Token de proteção ausente ou vencido (`CSRF_TOKEN_INVALID`).",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = AuthenticationExamples.SIGN_OUT_CSRF_TOKEN_INVALID)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = AuthenticationExamples.SIGN_OUT_INTERNAL_ERROR)))
    ResponseEntity<Object> signOut(HttpServletRequest request);

    @Operation(
        summary = "Consultar o responsável autenticado",
        description = "Devolve a identidade da sessão atual. É como o cliente descobre que a troca de senha"
            + " provisória está pendente, e também como obtém o cookie `XSRF-TOKEN` antes do primeiro POST.")
    @ApiResponse(
        responseCode = "200",
        description = "Identidade da sessão",
        content =
        @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = AuthenticatedCaretakerResponse.class)))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta"
            + " (`CARETAKER_UNAVAILABLE`), caso em que a sessão é encerrada.",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semSessao", value = AuthenticationExamples.ME_UNAUTHENTICATED),
                @ExampleObject(
                    name = "responsavelIndisponivel",
                    value = AuthenticationExamples.ME_CARETAKER_UNAVAILABLE)
            }))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`). A negociação recusa antes de"
            + " a consulta rodar: a sessão de um responsável inativado só é encerrada na próxima requisição"
            + " em JSON.",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = AuthenticationExamples.ME_NOT_ACCEPTABLE)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
        content =
        @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = AuthenticationExamples.ME_INTERNAL_ERROR)))
    ResponseEntity<Object> me(HttpServletRequest request);
}
