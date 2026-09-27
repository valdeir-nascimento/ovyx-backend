package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.shared.application.Command;

/**
 * Lancamento ou correcao da racao de uma gaiola do relatorio (FR-010 da 004): a formula e o consumo como
 * vieram, para o dominio recusar todas as falhas de uma vez, e quem lanca, pela sessao.
 *
 * @param sectorId o setor como veio no endereco
 * @param reportId o relatorio como veio no endereco
 * @param cageId a gaiola como veio no endereco
 * @param formulaId a formula como veio no corpo
 * @param consumption o consumo, em gramas, como foi digitado
 */
public record RecordFeedCommand(
        String sectorId, String reportId, String cageId, String formulaId, String consumption, Actor actor)
        implements Command<CageId> {}
