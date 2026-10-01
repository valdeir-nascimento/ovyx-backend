package io.github.ovyx.production.application.dashboard;

import java.util.UUID;

/**
 * Um setor ativo, como o painel da granja o le do farm (feature 009): o nome e a meta de produtividade atual. Nao e
 * o {@code FarmSector} do dominio, a visao do setor que as escritas do relatorio usam.
 *
 * @param target a meta do setor (feature 008)
 */
public record ActiveSector(UUID id, String name, LayingRateTarget target) {}
