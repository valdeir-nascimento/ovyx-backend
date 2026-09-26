package io.github.ovyx.production.presentation.dailyreport;

import io.github.ovyx.shared.presentation.AuthenticatedUser;
import io.github.ovyx.shared.presentation.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Documentacao das operacoes de relatorio diario ({@code contracts/production-api.yaml}).
 *
 * <p>O relatorio e recurso do setor: todo caminho passa por ele. Qualquer responsavel autenticado abre,
 * corrige, lanca e consulta relatorios (FR-019).
 */
@Tag(name = "Relatórios diários")
public interface DailyReportApi {

    /** Tipo de midia do corpo de erro, comum a todas as operacoes. */
    String PROBLEM_JSON = "application/problem+json";

    String SESSION_REFUSED = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta"
            + " (`CARETAKER_UNAVAILABLE`), caso em que a sessão é encerrada.";
    String WRITE_REFUSED = "Sem o token de proteção (`CSRF_TOKEN_INVALID`) ou com a troca de senha pendente"
            + " (`PASSWORD_CHANGE_REQUIRED`). Qualquer perfil escreve; não há 403 de perfil.";
    String READ_REFUSED = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).";
    String NOT_ACCEPTABLE = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).";
    String UNSUPPORTED = "Corpo em formato diferente de JSON (`REQUEST_NOT_ACCEPTABLE`).";
    String INTERNAL = "Falha inesperada (`INTERNAL_ERROR`), sem nenhum detalhe da causa.";
    String SECTOR_NOT_FOUND = "Setor inexistente, ou identificador malformado (`SECTOR_NOT_FOUND`).";
    String SECTOR_ID = "Identificador do setor.";
    String SECTOR_ID_EXAMPLE = "5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33";
    String REPORT_ID = "Identificador do relatório diário.";
    String REPORT_ID_EXAMPLE = "6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55";
    String CAGE_ID = "Identificador da gaiola, o mesmo do cadastro de gaiolas.";
    String CAGE_ID_EXAMPLE = "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44";
    String REPORT_NOT_FOUND = "Setor ou relatório inexistente, relatório de outro setor, ou identificador malformado"
            + " (`SECTOR_NOT_FOUND` ou `DAILY_REPORT_NOT_FOUND`).";
    String CAGE_NOT_FOUND = "Setor, relatório ou gaiola inexistente, gaiola que não está no relatório, ou"
            + " identificador malformado (`SECTOR_NOT_FOUND`, `DAILY_REPORT_NOT_FOUND` ou `CAGE_NOT_FOUND`).";

    @Operation(
            summary = "Listar relatórios do setor",
            description = "Relatórios do setor, do dia mais recente para o mais antigo, com o setor junto. O filtro"
                    + " por data traz o relatório daquele dia, se houver.")
    @ApiResponse(
            responseCode = "200",
            description = "Relatórios encontrados, possivelmente nenhum",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DailyReportPageResponse.class),
                    examples = @ExampleObject(
                            name = "relatoriosDoSetor",
                            summary = "Três dias do Galpão 4, o de hoje ainda pendente",
                            value = DailyReportExamples.LIST_200_RELATORIOS_DO_SETOR)))
    @ApiResponse(
            responseCode = "400",
            description = "Página fora do permitido ou data em formato inválido (`VALIDATION_FAILED`). Data inválida"
                    + " traz o nome do parâmetro em `details`.",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "paginaInvalida", summary = "Tamanho de página acima de 100", value = DailyReportExamples.LIST_400_PAGINA_INVALIDA),
                        @ExampleObject(name = "dataInvalida", summary = "Data fora do formato AAAA-MM-DD", value = DailyReportExamples.LIST_400_DATA_INVALIDA)
                    }))
    @ApiResponse(
            responseCode = "401",
            description = SESSION_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = DailyReportExamples.LIST_401_SEM_SESSAO)))
    @ApiResponse(
            responseCode = "403",
            description = READ_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "trocaDeSenhaPendente",
                            summary = "Senha provisória ainda não trocada",
                            value = DailyReportExamples.LIST_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
            responseCode = "404",
            description = SECTOR_NOT_FOUND,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "setorNaoEncontrado",
                            summary = "Identificador que não é de nenhum setor",
                            value = DailyReportExamples.LIST_404_SETOR_NAO_ENCONTRADO)))
    @ApiResponse(
            responseCode = "406",
            description = NOT_ACCEPTABLE,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.LIST_406)))
    @ApiResponse(
            responseCode = "500",
            description = INTERNAL,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.LIST_500)))
    ResponseEntity<Object> listDailyReports(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(description = "Data da coleta (`AAAA-MM-DD`); ausente, todos os relatórios do setor.", example = "2026-09-24")
            LocalDate collectionDate,
            @Parameter(description = "Página, a partir de 0.", example = "0") int page,
            @Parameter(description = "Tamanho da página, de 1 a 100.", example = "20") int size);

    @Operation(
            summary = "Sugerir a abertura do relatório",
            description = "Valores para o formulário de relatório novo: a data e a hora de agora, no fuso da granja;"
                    + " as aves do início do dia pelo saldo do relatório mais recente do setor — ou, sem relatório,"
                    + " pela soma das aves das gaiolas ativas —; e a idade do lote pela do relatório mais recente,"
                    + " mais as semanas completas passadas desde ele. Sem relatório anterior, a idade não é sugerida.")
    @ApiResponse(
            responseCode = "200",
            description = "Sugestão para o relatório novo",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DailyReportSuggestionResponse.class),
                    examples = {
                        @ExampleObject(name = "sugestaoPeloAnterior", summary = "Saldo e idade do relatório de ontem", value = DailyReportExamples.SUGGEST_200_SUGESTAO_PELO_ANTERIOR),
                        @ExampleObject(name = "primeiroRelatorio", summary = "Primeiro relatório do setor, pelas aves das gaiolas", value = DailyReportExamples.SUGGEST_200_PRIMEIRO_RELATORIO)
                    }))
    @ApiResponse(
            responseCode = "401",
            description = SESSION_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = DailyReportExamples.SUGGEST_401_SEM_SESSAO)))
    @ApiResponse(
            responseCode = "403",
            description = READ_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "trocaDeSenhaPendente",
                            summary = "Senha provisória ainda não trocada",
                            value = DailyReportExamples.SUGGEST_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
            responseCode = "404",
            description = SECTOR_NOT_FOUND,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "setorNaoEncontrado",
                            summary = "Identificador que não é de nenhum setor",
                            value = DailyReportExamples.SUGGEST_404_SETOR_NAO_ENCONTRADO)))
    @ApiResponse(
            responseCode = "406",
            description = NOT_ACCEPTABLE,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.SUGGEST_406)))
    @ApiResponse(
            responseCode = "500",
            description = INTERNAL,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.SUGGEST_500)))
    ResponseEntity<Object> suggestDailyReport(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId);

    @Operation(
            summary = "Abrir relatório do dia",
            description = "Abre o relatório de um dia de coleta do setor. As gaiolas ativas do setor entram no"
                    + " relatório como estão agora, sem lançamento.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DailyReportRequest.class),
                    examples = @ExampleObject(
                            name = "relatorioDeHoje",
                            summary = "Coleta da manhã",
                            value = DailyReportExamples.OPEN_REQUEST_RELATORIO_DE_HOJE)))
    @ApiResponse(
            responseCode = "201",
            description = "Relatório aberto. O cabeçalho `Location` aponta para ele.",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DailyReportDetailResponse.class),
                    examples = @ExampleObject(
                            name = "relatorioAberto",
                            summary = "Relatório novo, com as duas gaiolas por lançar",
                            value = DailyReportExamples.OPEN_201_RELATORIO_ABERTO)))
    @ApiResponse(
            responseCode = "400",
            description = "Campos inválidos, todos de uma vez (`VALIDATION_FAILED`), ou corpo ilegível.",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "camposInvalidos",
                            summary = "Data futura, aves zeradas e idade fora do intervalo",
                            value = DailyReportExamples.OPEN_400_CAMPOS_INVALIDOS)))
    @ApiResponse(
            responseCode = "401",
            description = SESSION_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = DailyReportExamples.OPEN_401_SEM_SESSAO)))
    @ApiResponse(
            responseCode = "403",
            description = WRITE_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "tokenCsrfAusente", summary = "Escrita sem o cabeçalho X-XSRF-TOKEN", value = DailyReportExamples.OPEN_403_TOKEN_CSRF_AUSENTE),
                        @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = DailyReportExamples.OPEN_403_TROCA_DE_SENHA_PENDENTE)
                    }))
    @ApiResponse(
            responseCode = "404",
            description = SECTOR_NOT_FOUND,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "setorNaoEncontrado",
                            summary = "Identificador que não é de nenhum setor",
                            value = DailyReportExamples.OPEN_404_SETOR_NAO_ENCONTRADO)))
    @ApiResponse(
            responseCode = "406",
            description = NOT_ACCEPTABLE,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.OPEN_406)))
    @ApiResponse(
            responseCode = "409",
            description = "Já há relatório do setor na data (`DAILY_REPORT_ALREADY_EXISTS`), o setor está inativo"
                    + " (`SECTOR_INACTIVE`), ou não tem gaiola ativa (`SECTOR_WITHOUT_ACTIVE_CAGES`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "relatorioDoDiaExiste", summary = "Segundo relatório para o mesmo dia", value = DailyReportExamples.OPEN_409_RELATORIO_DO_DIA_EXISTE),
                        @ExampleObject(name = "setorInativo", summary = "Relatório num setor inativo", value = DailyReportExamples.OPEN_409_SETOR_INATIVO),
                        @ExampleObject(name = "setorSemGaiola", summary = "Setor sem nenhuma gaiola ativa", value = DailyReportExamples.OPEN_409_SETOR_SEM_GAIOLA)
                    }))
    @ApiResponse(
            responseCode = "415",
            description = UNSUPPORTED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.OPEN_415)))
    @ApiResponse(
            responseCode = "500",
            description = INTERNAL,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.OPEN_500)))
    ResponseEntity<Object> openDailyReport(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            DailyReportRequest body,
            @Parameter(hidden = true) AuthenticatedUser user);

    @Operation(
            summary = "Consultar relatório",
            description = "O relatório com as gaiolas, os lançamentos de cada uma e os totais do dia.")
    @ApiResponse(
            responseCode = "200",
            description = "Relatório encontrado, de setor ativo ou inativo",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DailyReportDetailResponse.class),
                    examples = @ExampleObject(
                            name = "relatorioCompleto",
                            summary = "Produção completa e uma ocorrência de mortalidade",
                            value = DailyReportExamples.FIND_200_RELATORIO_COMPLETO)))
    @ApiResponse(
            responseCode = "401",
            description = SESSION_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = DailyReportExamples.FIND_401_SEM_SESSAO)))
    @ApiResponse(
            responseCode = "403",
            description = READ_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "trocaDeSenhaPendente",
                            summary = "Senha provisória ainda não trocada",
                            value = DailyReportExamples.FIND_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
            responseCode = "404",
            description = REPORT_NOT_FOUND,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "relatorioNaoEncontrado",
                            summary = "Relatório que não é deste setor",
                            value = DailyReportExamples.FIND_404_RELATORIO_NAO_ENCONTRADO)))
    @ApiResponse(
            responseCode = "406",
            description = NOT_ACCEPTABLE,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.FIND_406)))
    @ApiResponse(
            responseCode = "500",
            description = INTERNAL,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.FIND_500)))
    ResponseEntity<Object> findDailyReport(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(description = REPORT_ID, example = REPORT_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String reportId);

    @Operation(
            summary = "Corrigir relatório",
            description = "Troca os dados gerais do relatório, com as mesmas regras da abertura. As gaiolas e os"
                    + " lançamentos não mudam.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DailyReportRequest.class),
                    examples = @ExampleObject(
                            name = "idadeCorrigida",
                            summary = "Idade do lote corrigida para 21 semanas",
                            value = DailyReportExamples.CORRECT_REQUEST_IDADE_CORRIGIDA)))
    @ApiResponse(
            responseCode = "200",
            description = "Relatório corrigido, com quem corrigiu",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DailyReportDetailResponse.class),
                    examples = @ExampleObject(
                            name = "relatorioCorrigido",
                            summary = "Idade nova e João como autor da última correção",
                            value = DailyReportExamples.CORRECT_200_RELATORIO_CORRIGIDO)))
    @ApiResponse(
            responseCode = "400",
            description = "Campos inválidos, todos de uma vez (`VALIDATION_FAILED`), inclusive aves do início do dia"
                    + " abaixo do que o relatório já removeu, ou corpo ilegível.",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "avesAbaixoDasRemovidas",
                            summary = "Aves do início do dia abaixo das mortes e dos descartes já lançados",
                            value = DailyReportExamples.CORRECT_400_AVES_ABAIXO_DAS_REMOVIDAS)))
    @ApiResponse(
            responseCode = "401",
            description = SESSION_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = DailyReportExamples.CORRECT_401_SEM_SESSAO)))
    @ApiResponse(
            responseCode = "403",
            description = WRITE_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "tokenCsrfAusente", summary = "Escrita sem o cabeçalho X-XSRF-TOKEN", value = DailyReportExamples.CORRECT_403_TOKEN_CSRF_AUSENTE),
                        @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = DailyReportExamples.CORRECT_403_TROCA_DE_SENHA_PENDENTE)
                    }))
    @ApiResponse(
            responseCode = "404",
            description = REPORT_NOT_FOUND,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "relatorioNaoEncontrado",
                            summary = "Relatório que não é deste setor",
                            value = DailyReportExamples.CORRECT_404_RELATORIO_NAO_ENCONTRADO)))
    @ApiResponse(
            responseCode = "406",
            description = NOT_ACCEPTABLE,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.CORRECT_406)))
    @ApiResponse(
            responseCode = "409",
            description = "Outro relatório do setor já tem a data (`DAILY_REPORT_ALREADY_EXISTS`), ou o setor está"
                    + " inativo (`SECTOR_INACTIVE`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "dataDeOutroRelatorio",
                            summary = "Data trocada para um dia que já tem relatório",
                            value = DailyReportExamples.CORRECT_409_DATA_DE_OUTRO_RELATORIO)))
    @ApiResponse(
            responseCode = "415",
            description = UNSUPPORTED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.CORRECT_415)))
    @ApiResponse(
            responseCode = "500",
            description = INTERNAL,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.CORRECT_500)))
    ResponseEntity<Object> correctDailyReport(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(description = REPORT_ID, example = REPORT_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String reportId,
            DailyReportRequest body,
            @Parameter(hidden = true) AuthenticatedUser user);

    @Operation(
            summary = "Consultar gaiola do relatório",
            description = "Uma gaiola do relatório, como estava na abertura, com a produção e a mortalidade lançadas.")
    @ApiResponse(
            responseCode = "200",
            description = "Gaiola do relatório",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReportCageDetailResponse.class),
                    examples = @ExampleObject(
                            name = "gaiolaComOcorrencia",
                            summary = "Gaiola com produção e mortalidade lançadas",
                            value = DailyReportExamples.FIND_CAGE_200_GAIOLA_COM_OCORRENCIA)))
    @ApiResponse(
            responseCode = "401",
            description = SESSION_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = DailyReportExamples.FIND_CAGE_401_SEM_SESSAO)))
    @ApiResponse(
            responseCode = "403",
            description = READ_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "trocaDeSenhaPendente",
                            summary = "Senha provisória ainda não trocada",
                            value = DailyReportExamples.FIND_CAGE_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
            responseCode = "404",
            description = CAGE_NOT_FOUND,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "gaiolaForaDoRelatorio",
                            summary = "Gaiola cadastrada depois da abertura do relatório",
                            value = DailyReportExamples.FIND_CAGE_404_GAIOLA_FORA_DO_RELATORIO)))
    @ApiResponse(
            responseCode = "406",
            description = NOT_ACCEPTABLE,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.FIND_CAGE_406)))
    @ApiResponse(
            responseCode = "500",
            description = INTERNAL,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.FIND_CAGE_500)))
    ResponseEntity<Object> findReportCage(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(description = REPORT_ID, example = REPORT_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String reportId,
            @Parameter(description = CAGE_ID, example = CAGE_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String cageId);

    @Operation(
            summary = "Lançar produção da gaiola",
            description = "Lança ou corrige os ovos coletados na gaiola e a classificação dos que saíram fora do"
                    + " padrão. Classificação em branco vale zero.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProductionRequest.class),
                    examples = @ExampleObject(
                            name = "producaoDaGaiola",
                            summary = "45 ovos, 5 fora do padrão; pequenos e anormais em branco valem zero",
                            value = DailyReportExamples.PRODUCTION_REQUEST_PRODUCAO_DA_GAIOLA)))
    @ApiResponse(
            responseCode = "200",
            description = "Produção gravada na gaiola do relatório",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReportCageDetailResponse.class),
                    examples = @ExampleObject(
                            name = "producaoLancada",
                            summary = "Gaiola com a produção lançada",
                            value = DailyReportExamples.PRODUCTION_200_PRODUCAO_LANCADA)))
    @ApiResponse(
            responseCode = "400",
            description = "Campos inválidos, todos de uma vez (`VALIDATION_FAILED`), inclusive classificações acima dos"
                    + " ovos coletados, ou corpo ilegível.",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "classificacaoAcimaDosOvos", summary = "Classificações somando mais que os ovos coletados", value = DailyReportExamples.PRODUCTION_400_CLASSIFICACAO_ACIMA_DOS_OVOS),
                        @ExampleObject(name = "camposInvalidos", summary = "Ovos em branco e trincados que não são inteiros", value = DailyReportExamples.PRODUCTION_400_CAMPOS_INVALIDOS)
                    }))
    @ApiResponse(
            responseCode = "401",
            description = SESSION_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = DailyReportExamples.PRODUCTION_401_SEM_SESSAO)))
    @ApiResponse(
            responseCode = "403",
            description = WRITE_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "tokenCsrfAusente", summary = "Escrita sem o cabeçalho X-XSRF-TOKEN", value = DailyReportExamples.PRODUCTION_403_TOKEN_CSRF_AUSENTE),
                        @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = DailyReportExamples.PRODUCTION_403_TROCA_DE_SENHA_PENDENTE)
                    }))
    @ApiResponse(
            responseCode = "404",
            description = CAGE_NOT_FOUND,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "gaiolaForaDoRelatorio",
                            summary = "Gaiola cadastrada depois da abertura do relatório",
                            value = DailyReportExamples.PRODUCTION_404_GAIOLA_FORA_DO_RELATORIO)))
    @ApiResponse(
            responseCode = "406",
            description = NOT_ACCEPTABLE,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.PRODUCTION_406)))
    @ApiResponse(
            responseCode = "409",
            description = "Lançamento num relatório de setor inativo (`SECTOR_INACTIVE`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "setorInativo",
                            summary = "Setor inativado depois da abertura do relatório",
                            value = DailyReportExamples.PRODUCTION_409_SETOR_INATIVO)))
    @ApiResponse(
            responseCode = "415",
            description = UNSUPPORTED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.PRODUCTION_415)))
    @ApiResponse(
            responseCode = "500",
            description = INTERNAL,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.PRODUCTION_500)))
    ResponseEntity<Object> recordProduction(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(description = REPORT_ID, example = REPORT_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String reportId,
            @Parameter(description = CAGE_ID, example = CAGE_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String cageId,
            ProductionRequest body,
            @Parameter(hidden = true) AuthenticatedUser user);

    @Operation(
            summary = "Lançar mortalidade da gaiola",
            description = "Lança ou corrige as mortes e os descartes da gaiola, com uma observação. Em branco vale"
                    + " zero. As aves da gaiola no cadastro não mudam.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MortalityRequest.class),
                    examples = @ExampleObject(
                            name = "mortalidadeDaGaiola",
                            summary = "Uma morte e um descarte",
                            value = DailyReportExamples.MORTALITY_REQUEST_MORTALIDADE_DA_GAIOLA)))
    @ApiResponse(
            responseCode = "200",
            description = "Mortalidade gravada na gaiola do relatório",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReportCageDetailResponse.class),
                    examples = @ExampleObject(
                            name = "mortalidadeLancada",
                            summary = "Gaiola com a mortalidade lançada",
                            value = DailyReportExamples.MORTALITY_200_MORTALIDADE_LANCADA)))
    @ApiResponse(
            responseCode = "400",
            description = "Campos inválidos, todos de uma vez (`VALIDATION_FAILED`), inclusive mortes e descartes"
                    + " acima das aves da gaiola ou das do início do dia, ou corpo ilegível.",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "acimaDasAvesDaGaiola", summary = "Mortes e descartes acima das aves da gaiola", value = DailyReportExamples.MORTALITY_400_ACIMA_DAS_AVES_DA_GAIOLA),
                        @ExampleObject(name = "camposInvalidos", summary = "Mortes negativas e descartes que não são inteiros", value = DailyReportExamples.MORTALITY_400_CAMPOS_INVALIDOS)
                    }))
    @ApiResponse(
            responseCode = "401",
            description = SESSION_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = DailyReportExamples.MORTALITY_401_SEM_SESSAO)))
    @ApiResponse(
            responseCode = "403",
            description = WRITE_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "tokenCsrfAusente", summary = "Escrita sem o cabeçalho X-XSRF-TOKEN", value = DailyReportExamples.MORTALITY_403_TOKEN_CSRF_AUSENTE),
                        @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = DailyReportExamples.MORTALITY_403_TROCA_DE_SENHA_PENDENTE)
                    }))
    @ApiResponse(
            responseCode = "404",
            description = CAGE_NOT_FOUND,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "gaiolaForaDoRelatorio",
                            summary = "Gaiola cadastrada depois da abertura do relatório",
                            value = DailyReportExamples.MORTALITY_404_GAIOLA_FORA_DO_RELATORIO)))
    @ApiResponse(
            responseCode = "406",
            description = NOT_ACCEPTABLE,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.MORTALITY_406)))
    @ApiResponse(
            responseCode = "409",
            description = "Lançamento num relatório de setor inativo (`SECTOR_INACTIVE`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "setorInativo",
                            summary = "Setor inativado depois da abertura do relatório",
                            value = DailyReportExamples.MORTALITY_409_SETOR_INATIVO)))
    @ApiResponse(
            responseCode = "415",
            description = UNSUPPORTED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.MORTALITY_415)))
    @ApiResponse(
            responseCode = "500",
            description = INTERNAL,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.MORTALITY_500)))
    ResponseEntity<Object> recordMortality(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(description = REPORT_ID, example = REPORT_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String reportId,
            @Parameter(description = CAGE_ID, example = CAGE_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String cageId,
            MortalityRequest body,
            @Parameter(hidden = true) AuthenticatedUser user);

    @Operation(
            summary = "Confirmar dia sem ocorrência",
            description = "Registra que o dia não teve mortes nem descartes no setor, sem lançamento por gaiola. Só"
                    + " vale sem ocorrência lançada; confirmar de novo não muda nada.")
    @ApiResponse(
            responseCode = "200",
            description = "Mortalidade do relatório confirmada sem ocorrência",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DailyReportDetailResponse.class),
                    examples = @ExampleObject(
                            name = "diaSemOcorrencia",
                            summary = "Mortalidade lançada pela confirmação",
                            value = DailyReportExamples.CONFIRM_200_DIA_SEM_OCORRENCIA)))
    @ApiResponse(
            responseCode = "401",
            description = SESSION_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = DailyReportExamples.CONFIRM_401_SEM_SESSAO)))
    @ApiResponse(
            responseCode = "403",
            description = WRITE_REFUSED,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = {
                        @ExampleObject(name = "tokenCsrfAusente", summary = "Escrita sem o cabeçalho X-XSRF-TOKEN", value = DailyReportExamples.CONFIRM_403_TOKEN_CSRF_AUSENTE),
                        @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = DailyReportExamples.CONFIRM_403_TROCA_DE_SENHA_PENDENTE)
                    }))
    @ApiResponse(
            responseCode = "404",
            description = REPORT_NOT_FOUND,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "relatorioNaoEncontrado",
                            summary = "Relatório que não é deste setor",
                            value = DailyReportExamples.CONFIRM_404_RELATORIO_NAO_ENCONTRADO)))
    @ApiResponse(
            responseCode = "406",
            description = NOT_ACCEPTABLE,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.CONFIRM_406)))
    @ApiResponse(
            responseCode = "409",
            description = "O relatório já tem ocorrência lançada (`MORTALITY_ALREADY_RECORDED`), ou o setor está"
                    + " inativo (`SECTOR_INACTIVE`).",
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(
                            name = "ocorrenciaJaLancada",
                            summary = "Confirmação com uma morte lançada",
                            value = DailyReportExamples.CONFIRM_409_OCORRENCIA_JA_LANCADA)))
    @ApiResponse(
            responseCode = "500",
            description = INTERNAL,
            content = @Content(
                    mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemResponse.class),
                    examples = @ExampleObject(value = DailyReportExamples.CONFIRM_500)))
    ResponseEntity<Object> confirmNoMortality(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(description = REPORT_ID, example = REPORT_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String reportId,
            @Parameter(hidden = true) AuthenticatedUser user);
}
