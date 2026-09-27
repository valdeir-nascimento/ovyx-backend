package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.shared.application.Command;

/**
 * Confirmacao de que o dia nao teve mortes nem descartes no setor (FR-013), e quem confirma, pela sessao.
 *
 * @param sectorId o setor como veio no endereco
 * @param reportId o relatorio como veio no endereco
 */
public record ConfirmNoMortalityCommand(String sectorId, String reportId, Actor actor)
        implements Command<DailyReportId> {}
