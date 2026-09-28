package io.github.ovyx.production.presentation.dashboard;

import io.github.ovyx.production.application.dashboard.DashboardOverview;
import io.github.ovyx.production.application.dashboard.DashboardSector;
import io.github.ovyx.production.application.dashboard.PartOfDay;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** O cabecalho do painel e as abas (feature 006). */
@Schema(name = "DashboardOverview", description = "O cabeçalho do painel e as abas")
public record DashboardOverviewResponse(
        @Schema(description = "O dia de hoje da granja, no fuso dela", example = "2026-09-24") LocalDate today,
        @Schema(description = "A parte do dia no relógio da granja, para a saudação", example = "MORNING")
        PartOfDay partOfDay,
        @Schema(description = "Quantos setores ativos existem", example = "3") int activeSectors,
        @Schema(
                description = "Quantos setores ativos têm o relatório de hoje com a produção, a ração e a"
                        + " mortalidade lançadas",
                example = "2")
        int completeToday,
        @Schema(description = "Os setores ativos com ao menos um relatório, por nome") List<Tab> sectors) {

    /** Uma aba do painel. */
    @Schema(name = "DashboardSector", description = "Uma aba do painel")
    public record Tab(
            @Schema(example = "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11") UUID id,
            @Schema(example = "Codornas — Galpão 1") String name) {

        static Tab from(DashboardSector sector) {
            return new Tab(sector.id(), sector.name());
        }
    }

    public static DashboardOverviewResponse from(DashboardOverview overview) {
        return new DashboardOverviewResponse(
                overview.today(),
                overview.partOfDay(),
                overview.activeSectors(),
                overview.completeToday(),
                overview.sectors().stream().map(Tab::from).toList());
    }
}
