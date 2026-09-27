package io.github.ovyx.farm.presentation.sector;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.shared.presentation.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Documentacao das operacoes de setor ({@code contracts/farm-api.yaml}).
 *
 * <p>Consultar e de qualquer responsavel autenticado; cadastrar e editar, so do Administrador. Os 401,
 * 403 e 406 nascem na borda, antes do caso de uso; os 400, 404 e 409, no caso de uso.
 */
@Tag(name = "Setores")
public interface SectorApi {

    /**
     * Tipo de midia do corpo de erro, comum a todas as operacoes.
     */
    String PROBLEM_JSON = "application/problem+json";

    /**
     * O identificador do setor no caminho: malformado, o setor simplesmente nao e encontrado.
     */
    String SECTOR_ID = "Identificador do setor.";

    String EXAMPLE_SECTOR_ID = "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11";

    String SESSION_REFUSED = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta"
        + " (`CARETAKER_UNAVAILABLE`), caso em que a sessão é encerrada.";

    String WRITE_REFUSED = "Sem o perfil Administrador (`FORBIDDEN`), sem o token de proteção"
        + " (`CSRF_TOKEN_INVALID`) ou com a troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`). A recusa por"
        + " perfil é a mesma para o setor que existe e para o que não existe.";

    @Operation(
        summary = "Listar setores",
        description = "Setores com a quantidade de gaiolas ativas e a soma das aves delas, por nome.")
    @ApiResponse(
        responseCode = "200",
        description = "Setores encontrados, possivelmente nenhum",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            array = @ArraySchema(schema = @Schema(implementation = SectorSummaryResponse.class)),
            examples = @ExampleObject(
                name = "setoresAtivos",
                summary = "Dois setores ativos",
                value = SectorExamples.ACTIVE_SECTORS)))
    @ApiResponse(
        responseCode = "400",
        description = "Situação desconhecida (`VALIDATION_FAILED`), com o nome do parâmetro em `details`.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(
                name = "situacaoInvalida",
                summary = "Situação fora da lista",
                value = SectorExamples.LIST_STATUS_INVALID)))
    @ApiResponse(
        responseCode = "401",
        description = SESSION_REFUSED,
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", value = SectorExamples.LIST_UNAUTHENTICATED)))
    @ApiResponse(
        responseCode = "403",
        description = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(
                name = "trocaDeSenhaPendente", value = SectorExamples.LIST_PASSWORD_CHANGE_REQUIRED)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.LIST_NOT_ACCEPTABLE)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.LIST_INTERNAL_ERROR)))
    ResponseEntity<Object> listSectors(
        @Parameter(
            description = "Situação dos setores listados — ativos, inativos ou todos (`ALL`); ausente, só os"
                + " ativos.",
            example = "ACTIVE")
        StatusFilter status);

    @Operation(
        summary = "Cadastrar setor",
        description = "Exclusivo do perfil Administrador. Todas as falhas de preenchimento vêm juntas na mesma"
            + " resposta.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SectorRequest.class),
                examples = @ExampleObject(
                    name = "novoSetor", summary = "Galpão de codornas", value = SectorExamples.NEW_SECTOR))))
    @ApiResponse(
        responseCode = "201",
        description = "Setor cadastrado. O cabeçalho `Location` aponta para ele.",
        headers = @Header(
            name = "Location",
            description = "Caminho do setor cadastrado",
            schema = @Schema(type = "string", example = "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33")),
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = SectorDetailResponse.class),
            examples = @ExampleObject(
                name = "setorCadastrado", summary = "Setor novo, sem gaiolas", value = SectorExamples.REGISTERED)))
    @ApiResponse(
        responseCode = "400",
        description = "Campos inválidos, todos de uma vez (`VALIDATION_FAILED`), ou corpo ilegível"
            + " (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(
                    name = "camposInvalidos",
                    summary = "Nome curto e descrição longa demais",
                    value = SectorExamples.REGISTER_FIELDS_INVALID),
                @ExampleObject(name = "corpoIlegivel", value = SectorExamples.REGISTER_UNREADABLE_BODY)
            }))
    @ApiResponse(
        responseCode = "401",
        description = SESSION_REFUSED,
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", value = SectorExamples.REGISTER_UNAUTHENTICATED)))
    @ApiResponse(
        responseCode = "403",
        description = WRITE_REFUSED,
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semPermissao", value = SectorExamples.REGISTER_FORBIDDEN),
                @ExampleObject(name = "tokenCsrfAusente", value = SectorExamples.REGISTER_CSRF_TOKEN_INVALID),
                @ExampleObject(
                    name = "trocaDeSenhaPendente", value = SectorExamples.REGISTER_PASSWORD_CHANGE_REQUIRED)
            }))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`). A negociação recusa antes de o"
            + " cadastro ser feito.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.REGISTER_NOT_ACCEPTABLE)))
    @ApiResponse(
        responseCode = "409",
        description = "Outro setor ativo já usa o nome (`SECTOR_NAME_IN_USE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(
                name = "nomeEmUso", summary = "Nome de um setor ativo", value = SectorExamples.REGISTER_NAME_IN_USE)))
    @ApiResponse(
        responseCode = "415",
        description = "Corpo em formato diferente de JSON (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.REGISTER_UNSUPPORTED_MEDIA_TYPE)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.REGISTER_INTERNAL_ERROR)))
    ResponseEntity<Object> registerSector(SectorRequest body);

    @Operation(summary = "Consultar setor", description = "Setor ativo ou inativo, com os totais das gaiolas ativas.")
    @ApiResponse(
        responseCode = "200",
        description = "Setor encontrado, ativo ou inativo",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = SectorDetailResponse.class),
            examples = @ExampleObject(name = "setor", summary = "Setor com gaiolas", value = SectorExamples.DETAIL)))
    @ApiResponse(
        responseCode = "401",
        description = SESSION_REFUSED,
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", value = SectorExamples.FIND_UNAUTHENTICATED)))
    @ApiResponse(
        responseCode = "403",
        description = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(
                name = "trocaDeSenhaPendente", value = SectorExamples.FIND_PASSWORD_CHANGE_REQUIRED)))
    @ApiResponse(
        responseCode = "404",
        description = "Setor inexistente, ou identificador malformado (`SECTOR_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(
                name = "setorNaoEncontrado",
                summary = "Identificador que não é de nenhum setor",
                value = SectorExamples.FIND_NOT_FOUND)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.FIND_NOT_ACCEPTABLE)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.FIND_INTERNAL_ERROR)))
    ResponseEntity<Object> findSector(
        @Parameter(
            description = SECTOR_ID,
            example = EXAMPLE_SECTOR_ID,
            schema = @Schema(type = "string", format = "uuid"))
        String sectorId);

    @Operation(
        summary = "Editar setor",
        description = "Exclusivo do perfil Administrador. Mesmas regras do cadastro; as gaiolas do setor"
            + " continuam ligadas a ele.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SectorRequest.class),
                examples = @ExampleObject(
                    name = "edicao", summary = "Nome e descrição corrigidos", value = SectorExamples.EDIT))))
    @ApiResponse(
        responseCode = "200",
        description = "Setor atualizado",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = SectorDetailResponse.class),
            examples = @ExampleObject(value = SectorExamples.UPDATED)))
    @ApiResponse(
        responseCode = "400",
        description = "Campos inválidos, todos de uma vez (`VALIDATION_FAILED`), ou corpo ilegível"
            + " (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(
                    name = "nomeAusente", summary = "Nome em branco", value = SectorExamples.UPDATE_NAME_MISSING),
                @ExampleObject(name = "corpoIlegivel", value = SectorExamples.UPDATE_UNREADABLE_BODY)
            }))
    @ApiResponse(
        responseCode = "401",
        description = SESSION_REFUSED,
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", value = SectorExamples.UPDATE_UNAUTHENTICATED)))
    @ApiResponse(
        responseCode = "403",
        description = WRITE_REFUSED,
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semPermissao", value = SectorExamples.UPDATE_FORBIDDEN),
                @ExampleObject(name = "tokenCsrfAusente", value = SectorExamples.UPDATE_CSRF_TOKEN_INVALID),
                @ExampleObject(
                    name = "trocaDeSenhaPendente", value = SectorExamples.UPDATE_PASSWORD_CHANGE_REQUIRED)
            }))
    @ApiResponse(
        responseCode = "404",
        description = "Setor inexistente, ou identificador malformado (`SECTOR_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "setorNaoEncontrado", value = SectorExamples.UPDATE_NOT_FOUND)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.UPDATE_NOT_ACCEPTABLE)))
    @ApiResponse(
        responseCode = "409",
        description = "Outro setor ativo já usa o nome (`SECTOR_NAME_IN_USE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.UPDATE_NAME_IN_USE)))
    @ApiResponse(
        responseCode = "415",
        description = "Corpo em formato diferente de JSON (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.UPDATE_UNSUPPORTED_MEDIA_TYPE)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.UPDATE_INTERNAL_ERROR)))
    ResponseEntity<Object> updateSector(
        @Parameter(
            description = SECTOR_ID,
            example = EXAMPLE_SECTOR_ID,
            schema = @Schema(type = "string", format = "uuid"))
        String sectorId,
        SectorRequest body);

    @Operation(
        summary = "Inativar setor",
        description = "Exclusivo do perfil Administrador. Inativa o setor e, junto, todas as gaiolas ativas dele."
            + " Nada é apagado. Inativar um setor já inativo não muda nada.")
    @ApiResponse(
        responseCode = "200",
        description = "Setor inativado, com as gaiolas dele",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = SectorDetailResponse.class),
            examples = @ExampleObject(name = "inativado", value = SectorExamples.DEACTIVATED)))
    @ApiResponse(
        responseCode = "401",
        description = SESSION_REFUSED,
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", value = SectorExamples.DEACTIVATE_UNAUTHENTICATED)))
    @ApiResponse(
        responseCode = "403",
        description = WRITE_REFUSED,
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semPermissao", value = SectorExamples.DEACTIVATE_FORBIDDEN),
                @ExampleObject(name = "tokenCsrfAusente", value = SectorExamples.DEACTIVATE_CSRF_TOKEN_INVALID),
                @ExampleObject(name = "trocaDeSenhaPendente", value = SectorExamples.DEACTIVATE_PASSWORD_CHANGE_REQUIRED)
            }))
    @ApiResponse(
        responseCode = "404",
        description = "Setor inexistente, ou identificador malformado (`SECTOR_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "setorNaoEncontrado", value = SectorExamples.DEACTIVATE_NOT_FOUND)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.DEACTIVATE_NOT_ACCEPTABLE)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.DEACTIVATE_INTERNAL_ERROR)))
    ResponseEntity<Object> deactivateSector(
        @Parameter(
            description = SECTOR_ID,
            example = EXAMPLE_SECTOR_ID,
            schema = @Schema(type = "string", format = "uuid"))
        String sectorId);

    @Operation(
        summary = "Reativar setor",
        description = "Exclusivo do perfil Administrador. Reativa o setor e as gaiolas que a inativação dele"
            + " inativou; as que já estavam inativas antes continuam inativas.")
    @ApiResponse(
        responseCode = "200",
        description = "Setor reativado",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = SectorDetailResponse.class),
            examples = @ExampleObject(value = SectorExamples.REACTIVATED)))
    @ApiResponse(
        responseCode = "401",
        description = SESSION_REFUSED,
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", value = SectorExamples.REACTIVATE_UNAUTHENTICATED)))
    @ApiResponse(
        responseCode = "403",
        description = WRITE_REFUSED,
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semPermissao", value = SectorExamples.REACTIVATE_FORBIDDEN),
                @ExampleObject(name = "tokenCsrfAusente", value = SectorExamples.REACTIVATE_CSRF_TOKEN_INVALID),
                @ExampleObject(name = "trocaDeSenhaPendente", value = SectorExamples.REACTIVATE_PASSWORD_CHANGE_REQUIRED)
            }))
    @ApiResponse(
        responseCode = "404",
        description = "Setor inexistente, ou identificador malformado (`SECTOR_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "setorNaoEncontrado", value = SectorExamples.REACTIVATE_NOT_FOUND)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.REACTIVATE_NOT_ACCEPTABLE)))
    @ApiResponse(
        responseCode = "409",
        description = "Outro setor ativo tomou o nome enquanto este esteve inativo (`SECTOR_NAME_IN_USE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.REACTIVATE_NAME_IN_USE)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = SectorExamples.REACTIVATE_INTERNAL_ERROR)))
    ResponseEntity<Object> reactivateSector(
        @Parameter(
            description = SECTOR_ID,
            example = EXAMPLE_SECTOR_ID,
            schema = @Schema(type = "string", format = "uuid"))
        String sectorId);
}
