package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.shared.application.Query;

/** O cabecalho do painel e as abas (R-003 da 006). */
public record GetDashboardOverviewQuery() implements Query<DashboardOverview> {}
