package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.shared.application.Command;

/**
 * Lancamento ou correcao da producao de uma gaiola do relatorio: os ovos e as classificacoes como foram
 * digitados, para o dominio recusar todos de uma vez, e quem lanca, pela sessao.
 *
 * @param sectorId o setor como veio no endereco
 * @param reportId o relatorio como veio no endereco
 * @param cageId a gaiola como veio no endereco
 */
public record RecordProductionCommand(
        String sectorId,
        String reportId,
        String cageId,
        String eggs,
        String small,
        String jumbo,
        String dirty,
        String cracked,
        String bloodSpot,
        String abnormal,
        Actor actor)
        implements Command<CageId> {}
