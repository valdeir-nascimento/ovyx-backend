package io.github.ovyx.farm.presentation.cage;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.shared.presentation.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Documentacao das operacoes de gaiola ({@code contracts/farm-api.yaml}).
 *
 * <p>A gaiola e recurso do setor: todo caminho passa por ele. Consultar e de qualquer responsavel
 * autenticado; cadastrar e editar, so do Administrador.
 */
@Tag(name = "Gaiolas")
public interface CageApi {

    /** Tipo de midia do corpo de erro, comum a todas as operacoes. */
    String PROBLEM_JSON = "application/problem+json";

    String SESSION_REFUSED = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta"
            + " (`CARETAKER_UNAVAILABLE`), caso em que a sessão é encerrada.";

    String WRITE_REFUSED = "Sem o perfil Administrador (`FORBIDDEN`), sem o token de proteção"
            + " (`CSRF_TOKEN_INVALID`) ou com a troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`). A recusa por"
            + " perfil é a mesma para a gaiola que existe e para a que não existe.";

    String READ_REFUSED = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).";

    String NOT_ACCEPTABLE = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).";

    String UNSUPPORTED = "Corpo em formato diferente de JSON (`REQUEST_NOT_ACCEPTABLE`).";

    String INTERNAL = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.";

    String CAGE_NOT_FOUND = "Setor ou gaiola inexistente, gaiola de outro setor, ou identificador malformado"
            + " (`SECTOR_NOT_FOUND` ou `CAGE_NOT_FOUND`).";

    String CONFLICT = "Outra gaiola ativa do setor tem a bateria e o número (`CAGE_ALREADY_EXISTS`), ou o setor está"
            + " inativo (`SECTOR_INACTIVE`).";

    @Operation(
            summary = "Pesquisar gaiolas do setor",
            description = "Gaiolas do setor por bateria e número, com busca pelo código e filtros.")
    @ApiResponse(
            responseCode = "200",
            description = "Página de gaiolas, possivelmente vazia",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CagePageResponse.class),
                    examples = @ExampleObject(name = "bateriaB", summary = "Busca por \"B-07\"", value = CageExamples.BATTERY_B_PAGE)))
    @ApiResponse(
            responseCode = "400",
            description = "Página fora do permitido ou situação desconhecida (`VALIDATION_FAILED`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "paginaInvalida", value = CageExamples.SEARCH_PAGE_INVALID),
                        @ExampleObject(name = "situacaoInvalida", value = CageExamples.SEARCH_STATUS_INVALID)
                    }))
    @ApiResponse(
            responseCode = "401",
            description = SESSION_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "semSessao", value = CageExamples.SEARCH_UNAUTHENTICATED)))
    @ApiResponse(
            responseCode = "403",
            description = READ_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "trocaDeSenhaPendente", value = CageExamples.SEARCH_PASSWORD_CHANGE_REQUIRED)))
    @ApiResponse(
            responseCode = "404",
            description = "Setor inexistente, ou identificador malformado (`SECTOR_NOT_FOUND`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "setorNaoEncontrado", value = CageExamples.SEARCH_SECTOR_NOT_FOUND)))
    @ApiResponse(
            responseCode = "406",
            description = NOT_ACCEPTABLE,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = CageExamples.SEARCH_NOT_ACCEPTABLE)))
    @ApiResponse(
            responseCode = "500",
            description = INTERNAL,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = CageExamples.SEARCH_INTERNAL_ERROR)))
    ResponseEntity<Object> searchCages(
            @Parameter(
                    description = "Identificador do setor.",
                    example = "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
                    schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(description = "Trecho do código, sem distinguir maiúsculas (`B-07`, `b-0`, `07`).", example = "B-07")
            String code,
            @Parameter(description = "Só as gaiolas desta bateria.", example = "B") String battery,
            @Parameter(
                    description = "Situação das gaiolas — ativas, inativas ou todas (`ALL`); ausente, só as ativas.",
                    example = "ACTIVE")
            StatusFilter status,
            @Parameter(description = "Página, a partir de 0.", example = "0") int page,
            @Parameter(description = "Gaiolas por página, de 1 a 100.", example = "20") int size);

    @Operation(
            summary = "Cadastrar gaiola",
            description = "Exclusivo do perfil Administrador. Todas as falhas de preenchimento vêm juntas na mesma"
                    + " resposta, inclusive o número ou as aves que não são inteiros.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CageRequest.class),
                            examples = @ExampleObject(
                                    name = "novaGaiola", summary = "Gaiola B-13 com 50 codornas", value = CageExamples.NEW_CAGE))))
    @ApiResponse(
            responseCode = "201",
            description = "Gaiola cadastrada. O cabeçalho `Location` aponta para ela.",
            headers = @Header(
                    name = "Location",
                    description = "Caminho da gaiola cadastrada",
                    schema = @Schema(
                            type = "string",
                            example = "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/0e3f5a7b-2c4d-4e6f-9a8b-3c5d7e9f1a55")),
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CageDetailResponse.class),
                    examples = @ExampleObject(name = "gaiolaCadastrada", summary = "Gaiola B-13", value = CageExamples.REGISTERED)))
    @ApiResponse(
            responseCode = "400",
            description = "Campos inválidos, todos de uma vez (`VALIDATION_FAILED`), ou corpo ilegível (`REQUEST_NOT_ACCEPTABLE`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "camposInvalidos", value = CageExamples.REGISTER_FIELDS_INVALID),
                        @ExampleObject(name = "avesNaoInteiras", value = CageExamples.REGISTER_BIRDS_NOT_INTEGER),
                        @ExampleObject(name = "corpoIlegivel", value = CageExamples.REGISTER_UNREADABLE_BODY)
                    }))
    @ApiResponse(
            responseCode = "401",
            description = SESSION_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "semSessao", value = CageExamples.REGISTER_UNAUTHENTICATED)))
    @ApiResponse(
            responseCode = "403",
            description = WRITE_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "semPermissao", value = CageExamples.REGISTER_FORBIDDEN),
                        @ExampleObject(name = "tokenCsrfAusente", value = CageExamples.REGISTER_CSRF_TOKEN_INVALID),
                        @ExampleObject(name = "trocaDeSenhaPendente", value = CageExamples.REGISTER_PASSWORD_CHANGE_REQUIRED)
                    }))
    @ApiResponse(
            responseCode = "404",
            description = "Setor inexistente, ou identificador malformado (`SECTOR_NOT_FOUND`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "setorNaoEncontrado", value = CageExamples.REGISTER_SECTOR_NOT_FOUND)))
    @ApiResponse(
            responseCode = "406",
            description = NOT_ACCEPTABLE,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = CageExamples.REGISTER_NOT_ACCEPTABLE)))
    @ApiResponse(
            responseCode = "409",
            description = CONFLICT,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "gaiolaExistente", value = CageExamples.REGISTER_CAGE_EXISTS),
                        @ExampleObject(name = "setorInativo", value = CageExamples.REGISTER_SECTOR_INACTIVE)
                    }))
    @ApiResponse(
            responseCode = "415",
            description = UNSUPPORTED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = CageExamples.REGISTER_UNSUPPORTED_MEDIA_TYPE)))
    @ApiResponse(
            responseCode = "500",
            description = INTERNAL,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = CageExamples.REGISTER_INTERNAL_ERROR)))
    ResponseEntity<Object> registerCage(
            @Parameter(
                    description = "Identificador do setor.",
                    example = "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
                    schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            CageRequest body);

    @Operation(summary = "Consultar gaiola", description = "Gaiola ativa ou inativa do setor.")
    @ApiResponse(
            responseCode = "200",
            description = "Gaiola encontrada, ativa ou inativa",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CageDetailResponse.class),
                    examples = @ExampleObject(value = CageExamples.DETAIL)))
    @ApiResponse(
            responseCode = "401",
            description = SESSION_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "semSessao", value = CageExamples.FIND_UNAUTHENTICATED)))
    @ApiResponse(
            responseCode = "403",
            description = READ_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "trocaDeSenhaPendente", value = CageExamples.FIND_PASSWORD_CHANGE_REQUIRED)))
    @ApiResponse(
            responseCode = "404",
            description = CAGE_NOT_FOUND,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "gaiolaNaoEncontrada", value = CageExamples.FIND_CAGE_NOT_FOUND)))
    @ApiResponse(
            responseCode = "406",
            description = NOT_ACCEPTABLE,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = CageExamples.FIND_NOT_ACCEPTABLE)))
    @ApiResponse(
            responseCode = "500",
            description = INTERNAL,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = CageExamples.FIND_INTERNAL_ERROR)))
    ResponseEntity<Object> findCage(
            @Parameter(
                    description = "Identificador do setor.",
                    example = "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
                    schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(
                    description = "Identificador da gaiola.",
                    example = "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
                    schema = @Schema(type = "string", format = "uuid"))
            String cageId);

    @Operation(
            summary = "Editar gaiola",
            description = "Exclusivo do perfil Administrador. Troca a bateria, o número e a quantidade de aves. O setor"
                    + " da gaiola não muda.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CageRequest.class),
                            examples = @ExampleObject(
                                    name = "avesCorrigidas", summary = "Quantidade de aves corrigida", value = CageExamples.CORRECTED_BIRDS))))
    @ApiResponse(
            responseCode = "200",
            description = "Gaiola atualizada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CageDetailResponse.class),
                    examples = @ExampleObject(value = CageExamples.UPDATED)))
    @ApiResponse(
            responseCode = "400",
            description = "Campos inválidos, todos de uma vez (`VALIDATION_FAILED`), ou corpo ilegível (`REQUEST_NOT_ACCEPTABLE`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "camposInvalidos", value = CageExamples.UPDATE_FIELDS_INVALID),
                        @ExampleObject(name = "corpoIlegivel", value = CageExamples.UPDATE_UNREADABLE_BODY)
                    }))
    @ApiResponse(
            responseCode = "401",
            description = SESSION_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "semSessao", value = CageExamples.UPDATE_UNAUTHENTICATED)))
    @ApiResponse(
            responseCode = "403",
            description = WRITE_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "semPermissao", value = CageExamples.UPDATE_FORBIDDEN),
                        @ExampleObject(name = "tokenCsrfAusente", value = CageExamples.UPDATE_CSRF_TOKEN_INVALID),
                        @ExampleObject(name = "trocaDeSenhaPendente", value = CageExamples.UPDATE_PASSWORD_CHANGE_REQUIRED)
                    }))
    @ApiResponse(
            responseCode = "404",
            description = CAGE_NOT_FOUND,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "gaiolaNaoEncontrada", value = CageExamples.UPDATE_CAGE_NOT_FOUND)))
    @ApiResponse(
            responseCode = "406",
            description = NOT_ACCEPTABLE,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = CageExamples.UPDATE_NOT_ACCEPTABLE)))
    @ApiResponse(
            responseCode = "409",
            description = CONFLICT,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "gaiolaExistente", value = CageExamples.UPDATE_CAGE_EXISTS),
                        @ExampleObject(name = "setorInativo", value = CageExamples.UPDATE_SECTOR_INACTIVE)
                    }))
    @ApiResponse(
            responseCode = "415",
            description = UNSUPPORTED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = CageExamples.UPDATE_UNSUPPORTED_MEDIA_TYPE)))
    @ApiResponse(
            responseCode = "500",
            description = INTERNAL,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = CageExamples.UPDATE_INTERNAL_ERROR)))
    ResponseEntity<Object> updateCage(
            @Parameter(
                    description = "Identificador do setor.",
                    example = "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
                    schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(
                    description = "Identificador da gaiola.",
                    example = "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
                    schema = @Schema(type = "string", format = "uuid"))
            String cageId,
            CageRequest body);

    @Operation(
            summary = "Inativar gaiola",
            description = "Exclusivo do perfil Administrador. A gaiola sai dos totais do setor e continua consultável."
                    + " Inativar uma gaiola já inativa não muda nada.")
    @ApiResponse(
            responseCode = "200",
            description = "Gaiola inativada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CageDetailResponse.class),
                    examples = @ExampleObject(value = CageExamples.DEACTIVATED)))
    @ApiResponse(
            responseCode = "401",
            description = SESSION_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "semSessao", value = CageExamples.DEACTIVATE_UNAUTHENTICATED)))
    @ApiResponse(
            responseCode = "403",
            description = WRITE_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "semPermissao", value = CageExamples.DEACTIVATE_FORBIDDEN),
                        @ExampleObject(name = "tokenCsrfAusente", value = CageExamples.DEACTIVATE_CSRF_TOKEN_INVALID),
                        @ExampleObject(name = "trocaDeSenhaPendente", value = CageExamples.DEACTIVATE_PASSWORD_CHANGE_REQUIRED)
                    }))
    @ApiResponse(
            responseCode = "404",
            description = CAGE_NOT_FOUND,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "gaiolaNaoEncontrada", value = CageExamples.DEACTIVATE_CAGE_NOT_FOUND)))
    @ApiResponse(
            responseCode = "406",
            description = NOT_ACCEPTABLE,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = CageExamples.DEACTIVATE_NOT_ACCEPTABLE)))
    @ApiResponse(
            responseCode = "500",
            description = INTERNAL,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = CageExamples.DEACTIVATE_INTERNAL_ERROR)))
    ResponseEntity<Object> deactivateCage(
            @Parameter(
                    description = "Identificador do setor.",
                    example = "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
                    schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(
                    description = "Identificador da gaiola.",
                    example = "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
                    schema = @Schema(type = "string", format = "uuid"))
            String cageId);

    @Operation(
            summary = "Reativar gaiola",
            description = "Exclusivo do perfil Administrador. Recusada enquanto o setor estiver inativo, ou quando outra"
                    + " gaiola ativa tomou a bateria e o número.")
    @ApiResponse(
            responseCode = "200",
            description = "Gaiola reativada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CageDetailResponse.class),
                    examples = @ExampleObject(value = CageExamples.REACTIVATED)))
    @ApiResponse(
            responseCode = "401",
            description = SESSION_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "semSessao", value = CageExamples.REACTIVATE_UNAUTHENTICATED)))
    @ApiResponse(
            responseCode = "403",
            description = WRITE_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "semPermissao", value = CageExamples.REACTIVATE_FORBIDDEN),
                        @ExampleObject(name = "tokenCsrfAusente", value = CageExamples.REACTIVATE_CSRF_TOKEN_INVALID),
                        @ExampleObject(name = "trocaDeSenhaPendente", value = CageExamples.REACTIVATE_PASSWORD_CHANGE_REQUIRED)
                    }))
    @ApiResponse(
            responseCode = "404",
            description = CAGE_NOT_FOUND,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "gaiolaNaoEncontrada", value = CageExamples.REACTIVATE_CAGE_NOT_FOUND)))
    @ApiResponse(
            responseCode = "406",
            description = NOT_ACCEPTABLE,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = CageExamples.REACTIVATE_NOT_ACCEPTABLE)))
    @ApiResponse(
            responseCode = "409",
            description = "Outra gaiola ativa tomou a bateria e o número (`CAGE_ALREADY_EXISTS`), ou o setor está inativo (`SECTOR_INACTIVE`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "gaiolaExistente", value = CageExamples.REACTIVATE_CAGE_EXISTS),
                        @ExampleObject(name = "setorInativo", value = CageExamples.REACTIVATE_SECTOR_INACTIVE)
                    }))
    @ApiResponse(
            responseCode = "500",
            description = INTERNAL,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = CageExamples.REACTIVATE_INTERNAL_ERROR)))
    ResponseEntity<Object> reactivateCage(
            @Parameter(
                    description = "Identificador do setor.",
                    example = "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
                    schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(
                    description = "Identificador da gaiola.",
                    example = "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
                    schema = @Schema(type = "string", format = "uuid"))
            String cageId);
}
