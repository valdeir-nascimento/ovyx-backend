package io.github.ovyx.farm.presentation.formula;

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
 * Documentacao das operacoes de formula de racao ({@code contracts/feed-formulas-api.yaml}), gerada do
 * contrato: os textos, as respostas e os exemplos nomeados sao os dele.
 *
 * <p>Consultar e de qualquer responsavel autenticado; cadastrar, editar, inativar e reativar, so do
 * Administrador (R-010 da 004).
 */
@Tag(name = "Fórmulas de ração")
public interface FeedFormulaApi {

    /** Tipo de midia do corpo de erro, comum a todas as operacoes. */
    String PROBLEM_JSON = "application/problem+json";

    /** O identificador da formula no caminho: malformado, a formula simplesmente nao e encontrada. */
    String FORMULA_ID = "Identificador da fórmula de ração.";

    String EXAMPLE_FORMULA_ID = "4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11";

    @Operation(
        summary = "Listar fórmulas",
        description = "Fórmulas por nome, com o custo por ave ao dia.")
    @ApiResponse(
        responseCode = "200",
        description = "Fórmulas encontradas, possivelmente nenhuma",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            array = @ArraySchema(schema = @Schema(implementation = FeedFormulaResponse.class)),
            examples = @ExampleObject(name = "formulasAtivas", summary = "Duas fórmulas ativas", value = FeedFormulaExamples.LIST_200_FORMULAS_ATIVAS)))
    @ApiResponse(
        responseCode = "400",
        description = "Situação desconhecida (`VALIDATION_FAILED`), com o nome do parâmetro em `details`.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "situacaoInvalida", summary = "Situação fora da lista", value = FeedFormulaExamples.LIST_400_SITUACAO_INVALIDA)))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`), ou sessão aberta antes de a senha ser redefinida pelo link de recuperação "
            + "(`SESSION_REVOKED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = FeedFormulaExamples.LIST_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = FeedFormulaExamples.LIST_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = FeedFormulaExamples.LIST_406)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = FeedFormulaExamples.LIST_500)))
    ResponseEntity<Object> listFeedFormulas(
        @Parameter(description = "Situação das fórmulas listadas — ativas, inativas ou todas (`ALL`); ausente, só as ativas.", example = "ACTIVE")
        StatusFilter status);

    @Operation(
        summary = "Cadastrar fórmula",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = FeedFormulaRequest.class),
                examples = @ExampleObject(name = "novaFormula", summary = "Ração de postura", value = FeedFormulaExamples.REGISTER_REQUEST_NOVA_FORMULA))))
    @ApiResponse(
        responseCode = "201",
        description = "Fórmula cadastrada, ativa. O cabeçalho `Location` aponta para ela.",
        headers = @Header(
            name = "Location",
            description = "Caminho da fórmula cadastrada",
            schema = @Schema(type = "string", example = "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11")),
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = FeedFormulaResponse.class),
            examples = @ExampleObject(name = "formulaCadastrada", summary = "Fórmula nova, ativa", value = FeedFormulaExamples.REGISTER_201_FORMULA_CADASTRADA)))
    @ApiResponse(
        responseCode = "400",
        description = "Campos inválidos, todos de uma vez (`VALIDATION_FAILED`), ou corpo ilegível.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "camposInvalidos", summary = "Nome em branco, preço zero e consumo acima do limite", value = FeedFormulaExamples.REGISTER_400_CAMPOS_INVALIDOS)))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`), ou sessão aberta antes de a senha ser redefinida pelo link de recuperação "
            + "(`SESSION_REVOKED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = FeedFormulaExamples.REGISTER_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Sem o perfil Administrador (`FORBIDDEN`), sem o token de proteção (`CSRF_TOKEN_INVALID`) "
            + "ou com a troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semPermissao", summary = "Usuário comum tentando alterar", value = FeedFormulaExamples.REGISTER_403_SEM_PERMISSAO),
                @ExampleObject(name = "tokenCsrfAusente", summary = "Escrita sem o cabeçalho X-XSRF-TOKEN", value = FeedFormulaExamples.REGISTER_403_TOKEN_CSRF_AUSENTE)
            }))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = FeedFormulaExamples.REGISTER_406)))
    @ApiResponse(
        responseCode = "409",
        description = "Outra fórmula, ativa ou inativa, já usa o nome (`FEED_FORMULA_NAME_IN_USE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "nomeEmUso", summary = "Nome de outra fórmula", value = FeedFormulaExamples.REGISTER_409_NOME_EM_USO)))
    @ApiResponse(
        responseCode = "415",
        description = "Corpo em formato diferente de JSON (`REQUEST_NOT_ACCEPTABLE`, 415).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = FeedFormulaExamples.REGISTER_415)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = FeedFormulaExamples.REGISTER_500)))
    ResponseEntity<Object> registerFeedFormula(FeedFormulaRequest body);

    @Operation(
        summary = "Consultar fórmula")
    @ApiResponse(
        responseCode = "200",
        description = "Fórmula encontrada, ativa ou inativa",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = FeedFormulaResponse.class),
            examples = @ExampleObject(name = "formula", summary = "Fórmula ativa", value = FeedFormulaExamples.FIND_200_FORMULA)))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`), ou sessão aberta antes de a senha ser redefinida pelo link de recuperação "
            + "(`SESSION_REVOKED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = FeedFormulaExamples.FIND_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = FeedFormulaExamples.FIND_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
        responseCode = "404",
        description = "Fórmula inexistente, ou identificador malformado (`FEED_FORMULA_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "formulaNaoEncontrada", summary = "Identificador que não é de nenhuma fórmula", value = FeedFormulaExamples.FIND_404_FORMULA_NAO_ENCONTRADA)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = FeedFormulaExamples.FIND_406)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = FeedFormulaExamples.FIND_500)))
    ResponseEntity<Object> findFeedFormula(
        @Parameter(
            description = FORMULA_ID,
            example = EXAMPLE_FORMULA_ID,
            schema = @Schema(type = "string", format = "uuid"))
        String formulaId);

    @Operation(
        summary = "Editar fórmula",
        description = "Troca os campos da fórmula. Mudar o preço não muda o custo das rações já lançadas, que "
            + "guardam o preço de quando foram feitas.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = FeedFormulaRequest.class),
                examples = @ExampleObject(name = "precoNovo", summary = "Preço do fornecedor atualizado", value = FeedFormulaExamples.UPDATE_REQUEST_PRECO_NOVO))))
    @ApiResponse(
        responseCode = "200",
        description = "Fórmula atualizada",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = FeedFormulaResponse.class),
            examples = @ExampleObject(name = "formulaAtualizada", summary = "Preço novo", value = FeedFormulaExamples.UPDATE_200_FORMULA_ATUALIZADA)))
    @ApiResponse(
        responseCode = "400",
        description = "Campos inválidos, todos de uma vez (`VALIDATION_FAILED`), ou corpo ilegível.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "precoInvalido", summary = "Preço com três casas decimais", value = FeedFormulaExamples.UPDATE_400_PRECO_INVALIDO)))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`), ou sessão aberta antes de a senha ser redefinida pelo link de recuperação "
            + "(`SESSION_REVOKED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = FeedFormulaExamples.UPDATE_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Sem o perfil Administrador (`FORBIDDEN`), sem o token de proteção (`CSRF_TOKEN_INVALID`) "
            + "ou com a troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semPermissao", summary = "Usuário comum tentando alterar", value = FeedFormulaExamples.UPDATE_403_SEM_PERMISSAO),
                @ExampleObject(name = "tokenCsrfAusente", summary = "Escrita sem o cabeçalho X-XSRF-TOKEN", value = FeedFormulaExamples.UPDATE_403_TOKEN_CSRF_AUSENTE)
            }))
    @ApiResponse(
        responseCode = "404",
        description = "Fórmula inexistente, ou identificador malformado (`FEED_FORMULA_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "formulaNaoEncontrada", summary = "Identificador que não é de nenhuma fórmula", value = FeedFormulaExamples.UPDATE_404_FORMULA_NAO_ENCONTRADA)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = FeedFormulaExamples.UPDATE_406)))
    @ApiResponse(
        responseCode = "409",
        description = "Outra fórmula, ativa ou inativa, já usa o nome (`FEED_FORMULA_NAME_IN_USE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "nomeEmUso", summary = "Nome de outra fórmula", value = FeedFormulaExamples.UPDATE_409_NOME_EM_USO)))
    @ApiResponse(
        responseCode = "415",
        description = "Corpo em formato diferente de JSON (`REQUEST_NOT_ACCEPTABLE`, 415).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = FeedFormulaExamples.UPDATE_415)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = FeedFormulaExamples.UPDATE_500)))
    ResponseEntity<Object> updateFeedFormula(
        @Parameter(
            description = FORMULA_ID,
            example = EXAMPLE_FORMULA_ID,
            schema = @Schema(type = "string", format = "uuid"))
        String formulaId,
        FeedFormulaRequest body);

    @Operation(
        summary = "Inativar fórmula",
        description = "A fórmula deixa de ser oferecida nos lançamentos novos; os lançamentos que já a usam "
            + "continuam como estão. Inativar uma fórmula já inativa não muda nada.")
    @ApiResponse(
        responseCode = "200",
        description = "Fórmula inativada",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = FeedFormulaResponse.class),
            examples = @ExampleObject(name = "inativada", summary = "Fórmula inativada", value = FeedFormulaExamples.DEACTIVATE_200_INATIVADA)))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`), ou sessão aberta antes de a senha ser redefinida pelo link de recuperação "
            + "(`SESSION_REVOKED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = FeedFormulaExamples.DEACTIVATE_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Sem o perfil Administrador (`FORBIDDEN`), sem o token de proteção (`CSRF_TOKEN_INVALID`) "
            + "ou com a troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semPermissao", summary = "Usuário comum tentando alterar", value = FeedFormulaExamples.DEACTIVATE_403_SEM_PERMISSAO),
                @ExampleObject(name = "tokenCsrfAusente", summary = "Escrita sem o cabeçalho X-XSRF-TOKEN", value = FeedFormulaExamples.DEACTIVATE_403_TOKEN_CSRF_AUSENTE)
            }))
    @ApiResponse(
        responseCode = "404",
        description = "Fórmula inexistente, ou identificador malformado (`FEED_FORMULA_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "formulaNaoEncontrada", summary = "Identificador que não é de nenhuma fórmula", value = FeedFormulaExamples.DEACTIVATE_404_FORMULA_NAO_ENCONTRADA)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = FeedFormulaExamples.DEACTIVATE_406)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = FeedFormulaExamples.DEACTIVATE_500)))
    ResponseEntity<Object> deactivateFeedFormula(
        @Parameter(
            description = FORMULA_ID,
            example = EXAMPLE_FORMULA_ID,
            schema = @Schema(type = "string", format = "uuid"))
        String formulaId);

    @Operation(
        summary = "Reativar fórmula",
        description = "A fórmula volta a ser oferecida nos lançamentos. Reativar uma fórmula já ativa não muda "
            + "nada.")
    @ApiResponse(
        responseCode = "200",
        description = "Fórmula reativada",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = FeedFormulaResponse.class),
            examples = @ExampleObject(name = "reativada", summary = "Fórmula reativada", value = FeedFormulaExamples.REACTIVATE_200_REATIVADA)))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`), ou sessão aberta antes de a senha ser redefinida pelo link de recuperação "
            + "(`SESSION_REVOKED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = FeedFormulaExamples.REACTIVATE_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Sem o perfil Administrador (`FORBIDDEN`), sem o token de proteção (`CSRF_TOKEN_INVALID`) "
            + "ou com a troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "semPermissao", summary = "Usuário comum tentando alterar", value = FeedFormulaExamples.REACTIVATE_403_SEM_PERMISSAO),
                @ExampleObject(name = "tokenCsrfAusente", summary = "Escrita sem o cabeçalho X-XSRF-TOKEN", value = FeedFormulaExamples.REACTIVATE_403_TOKEN_CSRF_AUSENTE)
            }))
    @ApiResponse(
        responseCode = "404",
        description = "Fórmula inexistente, ou identificador malformado (`FEED_FORMULA_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "formulaNaoEncontrada", summary = "Identificador que não é de nenhuma fórmula", value = FeedFormulaExamples.REACTIVATE_404_FORMULA_NAO_ENCONTRADA)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = FeedFormulaExamples.REACTIVATE_406)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = FeedFormulaExamples.REACTIVATE_500)))
    ResponseEntity<Object> reactivateFeedFormula(
        @Parameter(
            description = FORMULA_ID,
            example = EXAMPLE_FORMULA_ID,
            schema = @Schema(type = "string", format = "uuid"))
        String formulaId);
}
