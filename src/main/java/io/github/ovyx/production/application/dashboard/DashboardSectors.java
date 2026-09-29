package io.github.ovyx.production.application.dashboard;

import java.util.List;

/**
 * Os setores do cabecalho do painel, como a porta os le.
 *
 * @param activeSectors quantos setores ativos existem
 * @param completeToday quantos deles tem o relatorio de hoje com a producao, a racao e a mortalidade lancadas
 * @param sectors as abas: os setores ativos com ao menos um relatorio, por nome
 */
public record DashboardSectors(int activeSectors, int completeToday, List<DashboardSector> sectors) {

    public DashboardSectors {
        sectors = List.copyOf(sectors);
    }
}
