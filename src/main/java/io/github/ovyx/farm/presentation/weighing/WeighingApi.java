package io.github.ovyx.farm.presentation.weighing;

import io.github.ovyx.shared.presentation.AuthenticatedUser;
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
 * Documentacao das operacoes de pesagem ({@code contracts/farm-api.yaml} da 005), gerada do contrato: os
 * textos, as respostas e os exemplos nomeados sao os dele.
 *
 * <p>Registrar, corrigir, excluir e consultar e de qualquer responsavel autenticado (FR-007 e R-010 da
 * 005): as primeiras escritas do farm abertas aos dois perfis.
 */
@Tag(name = "Pesagens")
public interface WeighingApi {

    /** Tipo de midia do corpo de erro, comum a todas as operacoes. */
    String PROBLEM_JSON = "application/problem+json";

    String SECTOR_ID = "Identificador do setor.";
    String SECTOR_ID_EXAMPLE = "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11";
    String CAGE_ID = "Identificador da gaiola.";
    String CAGE_ID_EXAMPLE = "2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66";
    String WEIGHING_ID = "Identificador da pesagem.";
    String WEIGHING_ID_EXAMPLE = "7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77";

    @Operation(
        summary = "Acompanhar o peso da gaiola",
        description = "A tela Peso médio numa resposta só: a gaiola, o setor com a faixa, a última pesagem, a "
            + "variação em 4 semanas, a situação diante da faixa, as últimas 12 pesagens para o gráfico e "
            + "o histórico inteiro. Só as pesagens válidas entram; as anuladas não aparecem. Vale também "
            + "para a gaiola e o setor inativos.")
    @ApiResponse(
        responseCode = "200",
        description = "O acompanhamento da gaiola; sem pesagem, sem `latest`, `fourWeekChange` e `rangeStatus`, e "
            + "com o gráfico e o histórico vazios",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = WeighingOverviewResponse.class),
            examples = {
                @ExampleObject(name = "dentroDaFaixa", summary = "Cinco pesagens semanais, a última dentro da faixa", value = WeighingExamples.OVERVIEW_200_DENTRO_DA_FAIXA),
                @ExampleObject(name = "semPesagem", summary = "Gaiola ainda não pesada, em setor sem faixa", value = WeighingExamples.OVERVIEW_200_SEM_PESAGEM)
            }))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`), ou sessão aberta antes de a senha ser redefinida pelo link de recuperação "
            + "(`SESSION_REVOKED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = WeighingExamples.OVERVIEW_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = WeighingExamples.OVERVIEW_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
        responseCode = "404",
        description = "Setor ou gaiola inexistente, gaiola de outro setor, ou identificador malformado "
            + "(`SECTOR_NOT_FOUND` ou `CAGE_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "gaiolaNaoEncontrada", summary = "Gaiola que não é deste setor", value = WeighingExamples.OVERVIEW_404_GAIOLA_NAO_ENCONTRADA)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = WeighingExamples.OVERVIEW_406)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = WeighingExamples.OVERVIEW_500)))
    ResponseEntity<Object> getWeighingOverview(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(description = CAGE_ID, example = CAGE_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String cageId);

    @Operation(
        summary = "Registrar pesagem",
        description = "Registra o peso médio de uma amostra de aves da gaiola numa data. Qualquer responsável "
            + "registra. O peso pode vir como número ou como texto, com vírgula ou ponto e até uma casa "
            + "decimal. A data não pode ser futura no fuso da granja, e cada gaiola tem no máximo uma "
            + "pesagem válida por data.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = WeighingRequest.class),
                examples = @ExampleObject(name = "pesagemSemanal", summary = "Pesagem semanal da gaiola", value = WeighingExamples.RECORD_REQUEST_PESAGEM_SEMANAL))))
    @ApiResponse(
        responseCode = "201",
        description = "Pesagem registrada. O cabeçalho `Location` aponta para ela.",
        headers = @Header(
            name = "Location",
            description = "Caminho da pesagem registrada",
            schema = @Schema(type = "string", example = "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77")),
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = WeighingResponse.class),
            examples = @ExampleObject(name = "pesagemRegistrada", summary = "A pesagem nova", value = WeighingExamples.RECORD_201_PESAGEM_REGISTRADA)))
    @ApiResponse(
        responseCode = "400",
        description = "Campos inválidos, todos de uma vez (`VALIDATION_FAILED`), ou corpo ilegível.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "camposInvalidos", summary = "Peso em branco e data de amanhã", value = WeighingExamples.RECORD_400_CAMPOS_INVALIDOS),
                @ExampleObject(name = "pesoInvalido", summary = "Peso com duas casas decimais", value = WeighingExamples.RECORD_400_PESO_INVALIDO)
            }))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`), ou sessão aberta antes de a senha ser redefinida pelo link de recuperação "
            + "(`SESSION_REVOKED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = WeighingExamples.RECORD_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = WeighingExamples.RECORD_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
        responseCode = "404",
        description = "Setor ou gaiola inexistente, gaiola de outro setor, ou identificador malformado "
            + "(`SECTOR_NOT_FOUND` ou `CAGE_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "gaiolaNaoEncontrada", summary = "Gaiola que não é deste setor", value = WeighingExamples.RECORD_404_GAIOLA_NAO_ENCONTRADA)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = WeighingExamples.RECORD_406)))
    @ApiResponse(
        responseCode = "409",
        description = "A data já tem pesagem na gaiola (`WEIGHING_DATE_IN_USE`), ou a gaiola ou o setor estão "
            + "inativos (`CAGE_INACTIVE`, `SECTOR_INACTIVE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = {
                @ExampleObject(name = "dataOcupada", summary = "Outra pesagem válida na mesma data", value = WeighingExamples.RECORD_409_DATA_OCUPADA),
                @ExampleObject(name = "gaiolaInativa", summary = "Pesagem de gaiola inativa", value = WeighingExamples.RECORD_409_GAIOLA_INATIVA)
            }))
    @ApiResponse(
        responseCode = "415",
        description = "Corpo em formato diferente de JSON (`REQUEST_NOT_ACCEPTABLE`, 415).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = WeighingExamples.RECORD_415)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = WeighingExamples.RECORD_500)))
    ResponseEntity<Object> recordWeighing(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(description = CAGE_ID, example = CAGE_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String cageId,
            WeighingRequest body,
            @Parameter(hidden = true) AuthenticatedUser user);

    @Operation(
        summary = "Consultar pesagem",
        description = "Uma pesagem válida da gaiola, com quem a registrou e quem a corrigiu por último. A anulada "
            + "responde 404.")
    @ApiResponse(
        responseCode = "200",
        description = "Pesagem encontrada",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = WeighingResponse.class),
            examples = @ExampleObject(name = "pesagemCorrigida", summary = "Pesagem corrigida pelo administrador", value = WeighingExamples.FIND_200_PESAGEM_CORRIGIDA)))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`), ou sessão aberta antes de a senha ser redefinida pelo link de recuperação "
            + "(`SESSION_REVOKED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = WeighingExamples.FIND_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = WeighingExamples.FIND_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
        responseCode = "404",
        description = "Setor, gaiola ou pesagem inexistente, pesagem anulada ou de outra gaiola, ou identificador "
            + "malformado (`SECTOR_NOT_FOUND`, `CAGE_NOT_FOUND`, `WEIGHING_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "pesagemNaoEncontrada", summary = "Pesagem anulada", value = WeighingExamples.FIND_404_PESAGEM_NAO_ENCONTRADA)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = WeighingExamples.FIND_406)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = WeighingExamples.FIND_500)))
    ResponseEntity<Object> findWeighing(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(description = CAGE_ID, example = CAGE_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String cageId,
            @Parameter(description = WEIGHING_ID, example = WEIGHING_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String weighingId);

    @Operation(
        summary = "Corrigir pesagem",
        description = "Troca a data e o peso, com as regras do registro, e marca quem corrigiu e quando. A "
            + "anulada não se corrige.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = WeighingRequest.class),
                examples = @ExampleObject(name = "pesoCorrigido", summary = "Peso digitado errado", value = WeighingExamples.CORRECT_REQUEST_PESO_CORRIGIDO))))
    @ApiResponse(
        responseCode = "200",
        description = "Pesagem corrigida",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = WeighingResponse.class),
            examples = @ExampleObject(name = "pesagemCorrigida", summary = "O peso de 116 g corrigido para 161 g", value = WeighingExamples.CORRECT_200_PESAGEM_CORRIGIDA)))
    @ApiResponse(
        responseCode = "400",
        description = "Campos inválidos, todos de uma vez (`VALIDATION_FAILED`), ou corpo ilegível.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "pesoForaDaFaixa", summary = "Peso acima de 10.000 g", value = WeighingExamples.CORRECT_400_PESO_FORA_DA_FAIXA)))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`), ou sessão aberta antes de a senha ser redefinida pelo link de recuperação "
            + "(`SESSION_REVOKED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = WeighingExamples.CORRECT_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = WeighingExamples.CORRECT_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
        responseCode = "404",
        description = "Setor, gaiola ou pesagem inexistente, pesagem anulada ou de outra gaiola, ou identificador "
            + "malformado (`SECTOR_NOT_FOUND`, `CAGE_NOT_FOUND`, `WEIGHING_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "pesagemNaoEncontrada", summary = "Pesagem anulada", value = WeighingExamples.CORRECT_404_PESAGEM_NAO_ENCONTRADA)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = WeighingExamples.CORRECT_406)))
    @ApiResponse(
        responseCode = "409",
        description = "A nova data já tem outra pesagem na gaiola (`WEIGHING_DATE_IN_USE`), ou a gaiola ou o "
            + "setor estão inativos (`CAGE_INACTIVE`, `SECTOR_INACTIVE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "setorInativo", summary = "Correção em setor inativo", value = WeighingExamples.CORRECT_409_SETOR_INATIVO)))
    @ApiResponse(
        responseCode = "415",
        description = "Corpo em formato diferente de JSON (`REQUEST_NOT_ACCEPTABLE`, 415).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = WeighingExamples.CORRECT_415)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = WeighingExamples.CORRECT_500)))
    ResponseEntity<Object> correctWeighing(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(description = CAGE_ID, example = CAGE_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String cageId,
            @Parameter(description = WEIGHING_ID, example = WEIGHING_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String weighingId,
            WeighingRequest body,
            @Parameter(hidden = true) AuthenticatedUser user);

    @Operation(
        summary = "Excluir pesagem",
        description = "A pesagem sai do histórico, do gráfico e da última pesagem da gaiola, mas não é apagada: "
            + "fica guardada como anulada, com quem a anulou e quando. A data dela fica livre para uma "
            + "nova pesagem. Anular de novo uma pesagem já anulada não muda nada.")
    @ApiResponse(
        responseCode = "204",
        description = "Pesagem anulada",
        content = @Content)
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`), ou sessão aberta antes de a senha ser redefinida pelo link de recuperação "
            + "(`SESSION_REVOKED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = WeighingExamples.VOID_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = WeighingExamples.VOID_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
        responseCode = "404",
        description = "Setor, gaiola ou pesagem inexistente, pesagem de outra gaiola, ou identificador malformado "
            + "(`SECTOR_NOT_FOUND`, `CAGE_NOT_FOUND`, `WEIGHING_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "pesagemNaoEncontrada", summary = "Pesagem que não é desta gaiola", value = WeighingExamples.VOID_404_PESAGEM_NAO_ENCONTRADA)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = WeighingExamples.VOID_406)))
    @ApiResponse(
        responseCode = "409",
        description = "A gaiola ou o setor estão inativos (`CAGE_INACTIVE`, `SECTOR_INACTIVE`), e as pesagens só "
            + "se consultam.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "gaiolaInativa", summary = "Exclusão em gaiola inativa", value = WeighingExamples.VOID_409_GAIOLA_INATIVA)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = WeighingExamples.VOID_500)))
    ResponseEntity<Object> voidWeighing(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(description = CAGE_ID, example = CAGE_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String cageId,
            @Parameter(description = WEIGHING_ID, example = WEIGHING_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String weighingId,
            @Parameter(hidden = true) AuthenticatedUser user);
}
