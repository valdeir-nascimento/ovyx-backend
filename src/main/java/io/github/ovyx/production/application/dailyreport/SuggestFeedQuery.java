package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.shared.application.Query;

/**
 * A proposta da racao do setor, antes de gravar (FR-009 e R-006 da 004).
 *
 * @param sectorId o setor como veio no endereco
 * @param reportId o relatorio como veio no endereco
 * @param formulaId a formula como veio na consulta
 */
public record SuggestFeedQuery(String sectorId, String reportId, String formulaId) implements Query<FeedSuggestion> {}
