package io.github.ovyx.production.presentation.dashboard;

import io.github.ovyx.production.application.dashboard.DashboardPeriod;
import io.github.ovyx.shared.presentation.ProblemResponse;
import io.github.ovyx.shared.presentation.SpreadsheetResponses;
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
 * Documentacao das operacoes do painel ({@code contracts/production-api.yaml} da 006), gerada do contrato:
 * os textos, as respostas e os exemplos nomeados sao os dele. Consultar e de qualquer responsavel
 * autenticado (FR-001, R-011 da 006).
 */
@Tag(name = "Painel")
public interface DashboardApi {

    /** Tipo de midia do corpo de erro, comum a todas as operacoes. */
    String PROBLEM_JSON = "application/problem+json";

    String SECTOR_ID = "Identificador do setor.";
    String SECTOR_ID_EXAMPLE = "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11";
    String PERIOD = "O período dos indicadores e da classificação. Sem ele, vale `TODAY`.";

    @Operation(
        summary = "Consultar o cabeçalho do painel",
        description = "O dia de hoje da granja, a parte do dia, quantos setores ativos têm o relatório de hoje "
            + "completo e as abas do painel, que são os setores ativos com ao menos um relatório.")
    @ApiResponse(
        responseCode = "200",
        description = "Cabeçalho do painel",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = DashboardOverviewResponse.class),
            examples = {
                @ExampleObject(name = "granjaComSetores", summary = "Três setores ativos, dois com o relatório de hoje completo", value = DashboardExamples.OVERVIEW_200_GRANJA_COM_SETORES),
                @ExampleObject(name = "granjaSemRelatorio", summary = "Nenhum setor com relatório ainda", value = DashboardExamples.OVERVIEW_200_GRANJA_SEM_RELATORIO)
            }))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = DashboardExamples.OVERVIEW_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = DashboardExamples.OVERVIEW_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = DashboardExamples.OVERVIEW_406)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = DashboardExamples.OVERVIEW_500)))
    ResponseEntity<Object> getDashboardOverview();

    @Operation(
        summary = "Consultar o painel da granja toda",
        description = "Os setores ativos somados no período (feature 009) — os quatro indicadores comparados com o "
            + "período anterior, a série dos últimos 7 dias com a meta da granja, a classificação dos ovos e a "
            + "comparação dos setores lado a lado, com a produtividade diante da meta de cada um, o custo por ovo, o "
            + "relatório de hoje e os alertas abertos. Nos indicadores, `incompleteDays` conta os relatórios (de cada "
            + "setor, em cada dia) com o lançamento pendente.")
    @ApiResponse(
        responseCode = "200",
        description = "Painel da granja toda",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = FarmDashboardResponse.class),
            examples = {
                @ExampleObject(name = "granjaComTresSetores", summary = "Hoje, com dois setores com relatório e um sem", value = DashboardExamples.FARM_200_GRANJA_COM_TRES_SETORES),
                @ExampleObject(name = "granjaSemRelatorio", summary = "Nenhum setor com relatório no período", value = DashboardExamples.FARM_200_GRANJA_SEM_RELATORIO)
            }))
    @ApiResponse(
        responseCode = "400",
        description = "Período fora de `TODAY`, `YESTERDAY` e `LAST_7_DAYS` (`VALIDATION_FAILED`), com o nome do "
            + "parâmetro em `details`.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "periodoInvalido", summary = "Período que não existe", value = DashboardExamples.FARM_400_PERIODO_INVALIDO)))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = DashboardExamples.FARM_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = DashboardExamples.FARM_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = DashboardExamples.FARM_406)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = DashboardExamples.FARM_500)))
    ResponseEntity<Object> getFarmDashboard(
        @Parameter(
            description = "O período dos indicadores, da classificação e da comparação. Sem ele, vale `TODAY`.",
            example = "TODAY")
        DashboardPeriod period);

    @Operation(
        summary = "Exportar o painel da granja toda para planilha",
        description = "O painel da granja no período, como planilha do Excel: os quatro indicadores somados com o "
            + "valor, o anterior e a variação; os 7 dias, com a meta da granja e os setores com relatório em cada "
            + "dia; a classificação dos ovos; e a comparação dos setores. Os números são os da consulta do painel da "
            + "granja. O arquivo se chama `painel-granja-<data de hoje da granja>.xlsx`.")
    @ApiResponse(
        responseCode = "200",
        description = "Planilha do painel da granja",
        headers = @Header(
            name = "Content-Disposition",
            description = "O nome do arquivo, para salvar.",
            schema = @Schema(type = "string", example = "attachment; filename=\"painel-granja-28-09-2026.xlsx\"")),
        content = @Content(
            mediaType = SpreadsheetResponses.SPREADSHEET,
            schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(
        responseCode = "400",
        description = "Período fora de `TODAY`, `YESTERDAY` e `LAST_7_DAYS` (`VALIDATION_FAILED`), com o nome do "
            + "parâmetro em `details`.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "periodoInvalido", summary = "Período que não existe", value = DashboardExamples.FARM_EXPORT_400_PERIODO_INVALIDO)))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = DashboardExamples.FARM_EXPORT_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = DashboardExamples.FARM_EXPORT_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = DashboardExamples.FARM_EXPORT_406)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = DashboardExamples.FARM_EXPORT_500)))
    ResponseEntity<?> exportFarmDashboard(
        @Parameter(description = "O período do painel exportado. Sem ele, vale `TODAY`.", example = "LAST_7_DAYS")
        DashboardPeriod period);

    @Operation(
        summary = "Consultar o painel de um setor",
        description = "Os indicadores do período, comparados com o período anterior; a série dos últimos 7 dias, "
            + "para as tendências e os gráficos; a classificação dos ovos do período; os alertas e as "
            + "pendências de hoje; e os 4 relatórios mais recentes. Um setor inativo continua "
            + "consultável, mas não aparece nas abas.")
    @ApiResponse(
        responseCode = "200",
        description = "Painel do setor",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = SectorDashboardResponse.class),
            examples = {
                @ExampleObject(name = "setorComAlertas", summary = "Hoje, com o relatório completo e três alertas nas gaiolas", value = DashboardExamples.SECTOR_200_SETOR_COM_ALERTAS),
                @ExampleObject(name = "hojeSemRelatorio", summary = "Hoje ainda sem relatório, com o de ontem sem a ração", value = DashboardExamples.SECTOR_200_HOJE_SEM_RELATORIO)
            }))
    @ApiResponse(
        responseCode = "400",
        description = "Período fora de `TODAY`, `YESTERDAY` e `LAST_7_DAYS` (`VALIDATION_FAILED`), com o nome do "
            + "parâmetro em `details`.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "periodoInvalido", summary = "Período que não existe", value = DashboardExamples.SECTOR_400_PERIODO_INVALIDO)))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = DashboardExamples.SECTOR_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = DashboardExamples.SECTOR_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
        responseCode = "404",
        description = "Setor inexistente, ou identificador malformado (`SECTOR_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "setorNaoEncontrado", summary = "Identificador que não é de nenhum setor", value = DashboardExamples.SECTOR_404_SETOR_NAO_ENCONTRADO)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = DashboardExamples.SECTOR_406)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = DashboardExamples.SECTOR_500)))
    ResponseEntity<Object> getSectorDashboard(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(description = PERIOD, example = "TODAY") DashboardPeriod period);

    @Operation(
        summary = "Exportar o painel de um setor para planilha",
        description = "O painel do setor no período, como planilha do Excel: os quatro indicadores com o valor, o "
            + "anterior e a variação; os 7 dias do gráfico; a classificação dos ovos; os alertas e as "
            + "pendências abertos; e os 4 relatórios mais recentes. Os números são os da consulta do "
            + "painel. O arquivo se chama `painel-<setor>-<data de hoje da granja>.xlsx`.")
    @ApiResponse(
        responseCode = "200",
        description = "Planilha do painel",
        headers = @Header(
            name = "Content-Disposition",
            description = "O nome do arquivo, para salvar. O nome do setor sai simplificado, sem acento e com hífens.",
            schema = @Schema(type = "string", example = "attachment; filename=\"painel-codornas-galpao-1-28-09-2026.xlsx\"")),
        content = @Content(
            mediaType = SpreadsheetResponses.SPREADSHEET,
            schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(
        responseCode = "400",
        description = "Período fora de `TODAY`, `YESTERDAY` e `LAST_7_DAYS` (`VALIDATION_FAILED`), com o nome do "
            + "parâmetro em `details`.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "periodoInvalido", summary = "Período que não existe", value = DashboardExamples.EXPORT_400_PERIODO_INVALIDO)))
    @ApiResponse(
        responseCode = "401",
        description = "Sem sessão válida (`UNAUTHENTICATED`), ou responsável inativado com a sessão aberta "
            + "(`CARETAKER_UNAVAILABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "semSessao", summary = "Sem sessão", value = DashboardExamples.EXPORT_401_SEM_SESSAO)))
    @ApiResponse(
        responseCode = "403",
        description = "Troca de senha pendente (`PASSWORD_CHANGE_REQUIRED`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "trocaDeSenhaPendente", summary = "Senha provisória ainda não trocada", value = DashboardExamples.EXPORT_403_TROCA_DE_SENHA_PENDENTE)))
    @ApiResponse(
        responseCode = "404",
        description = "Setor inexistente, ou identificador malformado (`SECTOR_NOT_FOUND`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(name = "setorNaoEncontrado", summary = "Identificador que não é de nenhum setor", value = DashboardExamples.EXPORT_404_SETOR_NAO_ENCONTRADO)))
    @ApiResponse(
        responseCode = "406",
        description = "Formato de resposta indisponível (`REQUEST_NOT_ACCEPTABLE`).",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = DashboardExamples.EXPORT_406)))
    @ApiResponse(
        responseCode = "500",
        description = "Falha inesperada (`INTERNAL_ERROR`), sem detalhe da causa.",
        content = @Content(
            mediaType = PROBLEM_JSON,
            schema = @Schema(implementation = ProblemResponse.class),
            examples = @ExampleObject(value = DashboardExamples.EXPORT_500)))
    ResponseEntity<?> exportSectorDashboard(
            @Parameter(description = SECTOR_ID, example = SECTOR_ID_EXAMPLE, schema = @Schema(type = "string", format = "uuid"))
            String sectorId,
            @Parameter(description = "O período do painel exportado. Sem ele, vale `TODAY`.", example = "LAST_7_DAYS") DashboardPeriod period);
}
