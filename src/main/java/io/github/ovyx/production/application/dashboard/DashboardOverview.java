package io.github.ovyx.production.application.dashboard;

import java.time.LocalDate;
import java.util.List;

/**
 * O cabecalho do painel e as abas (FR-002, FR-004 da 006).
 *
 * @param today o dia de hoje da granja
 * @param partOfDay a parte do dia no relogio da granja, para a saudacao
 */
public record DashboardOverview(
        LocalDate today, PartOfDay partOfDay, int activeSectors, int completeToday, List<DashboardSector> sectors) {

    public DashboardOverview {
        sectors = List.copyOf(sectors);
    }
}
