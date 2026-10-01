package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.shared.application.Query;

/**
 * O painel da granja toda num periodo (feature 009).
 *
 * @param period o periodo; sem ele, hoje
 */
public record GetFarmDashboardQuery(DashboardPeriod period) implements Query<FarmDashboard> {}
