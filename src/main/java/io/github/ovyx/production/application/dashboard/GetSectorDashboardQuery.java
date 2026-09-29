package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.shared.application.Query;

/**
 * O painel de um setor num periodo (R-003 da 006).
 *
 * @param sectorId o identificador do setor como veio no endereco
 * @param period o periodo; {@code null} vale {@link DashboardPeriod#TODAY}
 */
public record GetSectorDashboardQuery(String sectorId, DashboardPeriod period) implements Query<SectorDashboard> {}
