package io.github.ovyx.production.presentation.dailyreport;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lancamento da racao do setor pela sugestao, como o cliente envia: so a formula. Sem anotacoes de
 * validacao: a formula ausente, a inexistente e a inativa sao recusadas pelo dominio, no proprio campo.
 */
@Schema(
        name = "FeedSuggestionRequest",
        description = "Lançamento da ração do setor pela sugestão: as gaiolas sem ração recebem o consumo esperado"
                + " da fórmula")
public record FeedSuggestionRequest(
        @Schema(description = "Fórmula ativa", example = "4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11") String formulaId) {}
