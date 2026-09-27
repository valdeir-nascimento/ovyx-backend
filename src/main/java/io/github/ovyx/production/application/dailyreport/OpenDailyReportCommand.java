package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.shared.application.Command;

/**
 * Abertura do relatorio do dia: os campos como foram digitados, para o dominio recusar todos de uma vez,
 * e quem abre, pela sessao.
 */
public record OpenDailyReportCommand(
        String sectorId,
        String collectionDate,
        String collectionTime,
        String openingBirdCount,
        String flockAge,
        String note,
        Actor actor)
        implements Command<DailyReportId> {}
