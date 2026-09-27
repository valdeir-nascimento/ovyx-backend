package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.shared.application.Command;

/**
 * Correcao dos dados gerais do relatorio: os campos como foram digitados, para o dominio recusar todos de
 * uma vez, e quem corrige, pela sessao.
 *
 * @param sectorId o setor como veio no endereco
 * @param reportId o relatorio como veio no endereco
 */
public record CorrectDailyReportCommand(
        String sectorId,
        String reportId,
        String collectionDate,
        String collectionTime,
        String openingBirdCount,
        String flockAge,
        String note,
        Actor actor)
        implements Command<DailyReportId> {}
