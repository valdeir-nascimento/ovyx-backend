package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.shared.application.Command;

/**
 * Lancamento da racao do setor pela sugestao (FR-009 da 004): a formula como veio, para o dominio recusar
 * a ausente, a inexistente e a inativa no proprio campo, e quem lanca, pela sessao.
 *
 * @param sectorId o setor como veio no endereco
 * @param reportId o relatorio como veio no endereco
 * @param formulaId a formula como veio no corpo
 */
public record RecordFeedBySuggestionCommand(String sectorId, String reportId, String formulaId, Actor actor)
        implements Command<DailyReportId> {}
