package io.github.ovyx.production.application.dashboard;

import java.util.UUID;

/**
 * Para onde o alerta leva, como dados: a tela monta a rota. Sem campo, e a abertura do relatorio de hoje.
 *
 * @param reportId o relatorio de hoje, quando existe e o alerta e dele
 * @param cageId a gaiola, nos alertas de gaiola
 * @param cageCode o codigo da gaiola, nos alertas de gaiola
 */
public record AlertTarget(UUID reportId, UUID cageId, String cageCode) {}
