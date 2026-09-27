package io.github.ovyx.identity.presentation.caretaker;

import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.shared.presentation.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Documentacao das operacoes de responsaveis. Todas sao exclusivas do perfil Administrador.
 *
 * <p>Os 401, 403 e 406 nascem na borda, antes do caso de uso; os 400, 404 e 409, no caso de uso.
 * Cada resposta tem exemplo proprio, com o caminho da propria operacao.
 */
@Tag(name = "Responsáveis")
public interface CaretakerApi {

    /** Tipo de midia do corpo de erro, comum a todas as operacoes. */
    String PROBLEM_JSON = "application/problem+json";

    /** O 401 de toda operacao de responsaveis: a sessao e reconferida a cada requisicao. */
    String SESSION_REFUSED = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta"
        + " (`CARETAKER_UNAVAILABLE`), caso em que a sessão é encerrada.";

    @Operation(
        summary = "Cadastrar responsável",
        description = "Exclusivo do perfil Administrador. A senha informada é provisória: o responsável precisa"
            + " trocá-la no primeiro acesso. Todas as falhas de validação vêm juntas na mesma resposta.")
    @ApiResponse(
        responseCode = "201",
        description = "Responsável cadastrado. O cabeçalho `Location` aponta o recurso criado.",
        headers = @Header(
            name = "Location",
            description = "Caminho do responsável criado",
            schema = @Schema(type = "string", example = "/api/v1/caretakers/9f8e7d6c-5b4a-4938-2716-0f1e2d3c4b5a")),
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = CaretakerDetailResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.DETAIL)))
    @ApiResponse(
        responseCode = "400",
        description = "Campos inválidos, todos na mesma resposta (`VALIDATION_FAILED`), ou corpo que não pôde ser"
            + " lido (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "camposInvalidos", value = CaretakerExamples.REGISTER_VALIDATION_FAILED),
                @ExampleObject(name = "corpoIlegivel", value = CaretakerExamples.REGISTER_UNREADABLE_BODY)
            }))
    @ApiResponse(
        responseCode = "401",
        description = SESSION_REFUSED,
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semSessao", value = CaretakerExamples.REGISTER_UNAUTHENTICATED),
                @ExampleObject(
                    name = "responsavelIndisponivel",
                    value = CaretakerExamples.REGISTER_CARETAKER_UNAVAILABLE)
            }))
    @ApiResponse(
        responseCode = "403",
        description = "Sem o perfil Administrador (`FORBIDDEN`), token de proteção ausente (`CSRF_TOKEN_INVALID`) ou"
            + " troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semPermissao", value = CaretakerExamples.REGISTER_FORBIDDEN),
                @ExampleObject(name = "tokenCsrfAusente", value = CaretakerExamples.REGISTER_CSRF_TOKEN_INVALID),
                @ExampleObject(
                    name = "trocaDeSenhaPendente",
                    value = CaretakerExamples.REGISTER_PASSWORD_CHANGE_REQUIRED)
            }))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`). A negociação recusa antes de o"
            + " cadastro ser feito.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.REGISTER_NOT_ACCEPTABLE)))
    @ApiResponse(
        responseCode = "409",
        description = "CPF já cadastrado (`CPF_ALREADY_IN_USE`), ou e-mail ou celular já em uso por responsável ativo"
            + " (`EMAIL_ALREADY_IN_USE`, `MOBILE_PHONE_ALREADY_IN_USE`). Todos os conflitos vêm em `details`; o"
            + " `code` é o do primeiro, na ordem do formulário.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "emailEmUso", value = CaretakerExamples.REGISTER_EMAIL_ALREADY_IN_USE),
                @ExampleObject(
                    name = "celularEmUso",
                    value = CaretakerExamples.REGISTER_MOBILE_PHONE_ALREADY_IN_USE),
                @ExampleObject(name = "cpfJaCadastrado", value = CaretakerExamples.REGISTER_CPF_ALREADY_IN_USE)
            }))
    @ApiResponse(
        responseCode = "415",
        description = "Corpo em formato diferente de JSON (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.REGISTER_UNSUPPORTED_MEDIA_TYPE)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.REGISTER_INTERNAL_ERROR)))
    ResponseEntity<Object> register(RegisterCaretakerRequest body);

    @Operation(
        summary = "Listar e pesquisar responsáveis",
        description = "Exclusivo do perfil Administrador. Pesquisa por trecho do nome, sem distinção entre maiúsculas"
            + " e minúsculas, em ordem alfabética.")
    @ApiResponse(
        responseCode = "200",
        description = "Página de responsáveis",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = CaretakerPageResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.PAGE)))
    @ApiResponse(
        responseCode = "400",
        description = "Página ou tamanho fora do contrato, ou situação desconhecida (`VALIDATION_FAILED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.SEARCH_INVALID_PAGE)))
    @ApiResponse(
        responseCode = "401",
        description = SESSION_REFUSED,
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semSessao", value = CaretakerExamples.SEARCH_UNAUTHENTICATED),
                @ExampleObject(
                    name = "responsavelIndisponivel",
                    value = CaretakerExamples.SEARCH_CARETAKER_UNAVAILABLE)
            }))
    @ApiResponse(
        responseCode = "403",
        description = "Sem o perfil Administrador (`FORBIDDEN`) ou troca de senha pendente"
            + " (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semPermissao", value = CaretakerExamples.SEARCH_FORBIDDEN),
                @ExampleObject(
                    name = "trocaDeSenhaPendente",
                    value = CaretakerExamples.SEARCH_PASSWORD_CHANGE_REQUIRED)
            }))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.SEARCH_NOT_ACCEPTABLE)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.SEARCH_INTERNAL_ERROR)))
    ResponseEntity<Object> search(
        @Parameter(description = "Trecho do nome a pesquisar", example = "pereira") String name,
        @Parameter(description = "Situação; ausente, traz as duas", example = "ACTIVE") CaretakerStatus status,
        @Parameter(description = "Página, a partir de 0", example = "0") int page,
        @Parameter(description = "Tamanho da página, de 1 a 100", example = "20") int size);

    @Operation(summary = "Consultar responsável", description = "Exclusivo do perfil Administrador.")
    @ApiResponse(
        responseCode = "200",
        description = "Responsável encontrado, ativo ou inativo",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = CaretakerDetailResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.DETAIL)))
    @ApiResponse(
        responseCode = "400",
        description = "Identificador em formato inválido (`VALIDATION_FAILED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.FIND_INVALID_ID)))
    @ApiResponse(
        responseCode = "401",
        description = SESSION_REFUSED,
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semSessao", value = CaretakerExamples.FIND_UNAUTHENTICATED),
                @ExampleObject(
                    name = "responsavelIndisponivel",
                    value = CaretakerExamples.FIND_CARETAKER_UNAVAILABLE)
            }))
    @ApiResponse(
        responseCode = "403",
        description = "Sem o perfil Administrador (`FORBIDDEN`) ou troca de senha pendente"
            + " (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semPermissao", value = CaretakerExamples.FIND_FORBIDDEN),
                @ExampleObject(
                    name = "trocaDeSenhaPendente",
                    value = CaretakerExamples.FIND_PASSWORD_CHANGE_REQUIRED)
            }))
    @ApiResponse(
        responseCode = "404",
        description = "Responsável inexistente (`CARETAKER_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.FIND_NOT_FOUND)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.FIND_NOT_ACCEPTABLE)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.FIND_INTERNAL_ERROR)))
    ResponseEntity<Object> find(
        @Parameter(description = "Identificador do responsável", example = "9f8e7d6c-5b4a-4938-2716-0f1e2d3c4b5a")
        UUID caretakerId);

    @Operation(
        summary = "Editar responsável",
        description = "Exclusivo do perfil Administrador. Altera dados cadastrais e perfil; não altera a senha."
            + " Rebaixar o último administrador ativo é recusado.")
    @ApiResponse(
        responseCode = "200",
        description = "Responsável atualizado",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = CaretakerDetailResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.UPDATED_DETAIL)))
    @ApiResponse(
        responseCode = "400",
        description = "Campos inválidos, todos na mesma resposta (`VALIDATION_FAILED`), identificador em formato"
            + " inválido, ou corpo que não pôde ser lido (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "camposInvalidos", value = CaretakerExamples.UPDATE_VALIDATION_FAILED),
                @ExampleObject(name = "identificadorInvalido", value = CaretakerExamples.UPDATE_INVALID_ID),
                @ExampleObject(name = "corpoIlegivel", value = CaretakerExamples.UPDATE_UNREADABLE_BODY)
            }))
    @ApiResponse(
        responseCode = "401",
        description = SESSION_REFUSED,
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semSessao", value = CaretakerExamples.UPDATE_UNAUTHENTICATED),
                @ExampleObject(
                    name = "responsavelIndisponivel",
                    value = CaretakerExamples.UPDATE_CARETAKER_UNAVAILABLE)
            }))
    @ApiResponse(
        responseCode = "403",
        description = "Sem o perfil Administrador (`FORBIDDEN`), token de proteção ausente (`CSRF_TOKEN_INVALID`) ou"
            + " troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semPermissao", value = CaretakerExamples.UPDATE_FORBIDDEN),
                @ExampleObject(name = "tokenCsrfAusente", value = CaretakerExamples.UPDATE_CSRF_TOKEN_INVALID),
                @ExampleObject(
                    name = "trocaDeSenhaPendente",
                    value = CaretakerExamples.UPDATE_PASSWORD_CHANGE_REQUIRED)
            }))
    @ApiResponse(
        responseCode = "404",
        description = "Responsável inexistente (`CARETAKER_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.UPDATE_NOT_FOUND)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`). A negociação recusa antes de a"
            + " edição ser feita.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.UPDATE_NOT_ACCEPTABLE)))
    @ApiResponse(
        responseCode = "409",
        description = "Identificador já em uso (`CPF_ALREADY_IN_USE`, `EMAIL_ALREADY_IN_USE`,"
            + " `MOBILE_PHONE_ALREADY_IN_USE`), ou rebaixamento do último administrador ativo (`LAST_ADMINISTRATOR`,"
            + " com `role` em `details`). Todos os conflitos vêm em `details`; o `code` é o do primeiro.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "emailEmUso", value = CaretakerExamples.UPDATE_EMAIL_ALREADY_IN_USE),
                @ExampleObject(name = "ultimoAdministrador", value = CaretakerExamples.UPDATE_LAST_ADMINISTRATOR)
            }))
    @ApiResponse(
        responseCode = "415",
        description = "Corpo em formato diferente de JSON (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.UPDATE_UNSUPPORTED_MEDIA_TYPE)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.UPDATE_INTERNAL_ERROR)))
    ResponseEntity<Object> update(
        @Parameter(description = "Identificador do responsável", example = "9f8e7d6c-5b4a-4938-2716-0f1e2d3c4b5a")
        UUID caretakerId,
        UpdateCaretakerRequest body);

    @Operation(
        summary = "Inativar responsável",
        description = "Exclusivo do perfil Administrador. O responsável deixa de conseguir entrar no sistema. O"
            + " histórico é preservado — não há exclusão física, porque registros de outras áreas o referenciam."
            + " Inativar quem já está inativo não altera nada. Inativar o último administrador ativo é recusado.")
    @ApiResponse(
        responseCode = "200",
        description = "Responsável inativado",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = CaretakerDetailResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.DEACTIVATED_DETAIL)))
    @ApiResponse(
        responseCode = "400",
        description = "Identificador em formato inválido (`VALIDATION_FAILED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.DEACTIVATE_INVALID_ID)))
    @ApiResponse(
        responseCode = "401",
        description = SESSION_REFUSED,
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semSessao", value = CaretakerExamples.DEACTIVATE_UNAUTHENTICATED),
                @ExampleObject(
                    name = "responsavelIndisponivel",
                    value = CaretakerExamples.DEACTIVATE_CARETAKER_UNAVAILABLE)
            }))
    @ApiResponse(
        responseCode = "403",
        description = "Sem o perfil Administrador (`FORBIDDEN`), token de proteção ausente (`CSRF_TOKEN_INVALID`) ou"
            + " troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semPermissao", value = CaretakerExamples.DEACTIVATE_FORBIDDEN),
                @ExampleObject(name = "tokenCsrfAusente", value = CaretakerExamples.DEACTIVATE_CSRF_TOKEN_INVALID),
                @ExampleObject(
                    name = "trocaDeSenhaPendente",
                    value = CaretakerExamples.DEACTIVATE_PASSWORD_CHANGE_REQUIRED)
            }))
    @ApiResponse(
        responseCode = "404",
        description = "Responsável inexistente (`CARETAKER_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.DEACTIVATE_NOT_FOUND)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`). A negociação recusa antes de a"
            + " inativação ser feita.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.DEACTIVATE_NOT_ACCEPTABLE)))
    @ApiResponse(
        responseCode = "409",
        description = "Último administrador ativo (`LAST_ADMINISTRATOR`, com `status` em `details`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.DEACTIVATE_LAST_ADMINISTRATOR)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = CaretakerExamples.DEACTIVATE_INTERNAL_ERROR)))
    ResponseEntity<Object> deactivate(
        @Parameter(description = "Identificador do responsável", example = "9f8e7d6c-5b4a-4938-2716-0f1e2d3c4b5a")
        UUID caretakerId);
}
