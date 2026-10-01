package io.github.ovyx.production.presentation.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ovyx.production.application.dashboard.DashboardPeriod;
import io.github.ovyx.production.application.dashboard.FarmDashboard;
import io.github.ovyx.production.application.dashboard.FarmDay;
import io.github.ovyx.production.application.dashboard.FarmSectorRow;
import io.github.ovyx.production.application.dashboard.TargetStatus;
import io.github.ovyx.production.presentation.dashboard.DashboardOverviewResponse.Tab;
import io.github.ovyx.production.presentation.dashboard.SectorDashboardResponse.Grading;
import io.github.ovyx.production.presentation.dashboard.SectorDashboardResponse.IndicatorsResponse;
import io.github.ovyx.production.presentation.dashboard.SectorDashboardResponse.TodayReport;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * O painel da granja toda num periodo (feature 009). Reusa as respostas do indicador, da classificacao e do relatorio
 * de hoje do painel do setor. O que nao tem valor fica fora do corpo, e nao zero.
 */
@Schema(name = "FarmDashboard", description = "O painel da granja toda, os setores ativos somados no período")
public record FarmDashboardResponse(
        @Schema(example = "TODAY") DashboardPeriod period,
        @Schema(description = "O primeiro dia do período", example = "2026-09-24") LocalDate from,
        @Schema(description = "O último dia do período", example = "2026-09-24") LocalDate to,
        @Schema(description = "Quantos setores ativos a granja tem", example = "3") int activeSectors,
        @Schema(description = "Quantos deles têm relatório no período", example = "2") int reportingSectors,
        IndicatorsResponse indicators,
        @Schema(description = "Os 7 dias de hoje − 6 até hoje, para as tendências e os gráficos") List<Day> trend,
        @Schema(
                description = "A meta da granja nos 7 dias, a média das metas dos setores ponderada pelas aves dos"
                        + " relatórios, com duas casas; ausente sem relatório nos 7 dias",
                example = "80.58")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal target,
        @Schema(
                description = "O último dia com relatório diante da meta da granja naquele dia; ausente sem dia com"
                        + " relatório",
                example = "ABOVE")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        TargetStatus targetStatus,
        @Schema(description = "A classificação dos ovos do período, somada; ausente sem ovo")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Grading grades,
        @Schema(description = "Os setores ativos, por nome, como as abas do painel") List<SectorRow> sectors) {

    /** Um dia da serie da granja. */
    @Schema(
            name = "FarmDay",
            description = "Um dia da série da granja. Os valores somam os setores ativos com relatório no dia; sem"
                    + " nenhum, ficam ausentes, e com a ração pendente em todos, os custos ficam ausentes.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Day(
            @Schema(example = "2026-09-24") LocalDate date,
            @Schema(description = "Ovos coletados, somados", example = "2900") Integer production,
            @Schema(
                    description = "Produtividade, a soma dos ovos sobre a soma das aves, em porcentagem com duas casas",
                    example = "90.63")
            BigDecimal layingRate,
            @Schema(
                    description = "Custo da ração dos relatórios com a ração completa, em reais com duas casas",
                    example = "280.00")
            BigDecimal feedCost,
            @Schema(description = "Custo por ovo desses relatórios, em reais com três casas", example = "0.097")
            BigDecimal costPerEgg,
            @Schema(
                    description = "A meta da granja no dia, a média das metas dos setores com relatório ponderada pelas"
                            + " aves, com duas casas",
                    example = "80.13")
            BigDecimal target,
            @Schema(description = "Quantos setores ativos têm relatório no dia", example = "2") int reportingSectors) {

        static Day from(FarmDay day) {
            return new Day(
                    day.date(),
                    day.production(),
                    day.layingRate(),
                    day.feedCost(),
                    day.costPerEgg(),
                    day.target(),
                    day.reportingSectors());
        }
    }

    /** Um setor ativo na comparacao. */
    @Schema(
            name = "FarmSectorRow",
            description = "Um setor ativo na comparação da granja, no período. Sem relatório no período, os números"
                    + " ficam ausentes, e não zero.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SectorRow(
            Tab sector,
            @Schema(description = "Ovos coletados no período", example = "1740") Integer production,
            @Schema(description = "Produtividade do período, em porcentagem com duas casas", example = "87.00")
            BigDecimal layingRate,
            @Schema(description = "A meta de produtividade atual do setor, com duas casas", example = "85.00")
            BigDecimal target,
            @Schema(
                    description = "A produtividade do período diante da meta do setor, a igualdade contando como acima;"
                            + " ausente sem relatório no período",
                    example = "ABOVE")
            TargetStatus targetStatus,
            @Schema(
                    description = "Custo por ovo do período, só com os relatórios de ração completa, em reais com três"
                            + " casas",
                    example = "0.092")
            BigDecimal costPerEgg,
            @Schema(description = "O relatório de hoje; ausente se hoje ainda não foi aberto") TodayReport todayReport,
            @Schema(
                    description = "Os alertas e as pendências abertos hoje no setor, os mesmos do painel dele",
                    example = "3")
            int openAlerts) {

        static SectorRow from(FarmSectorRow row) {
            return new SectorRow(
                    Tab.from(row.sector()),
                    row.production(),
                    row.layingRate(),
                    row.target(),
                    row.targetStatus(),
                    row.costPerEgg(),
                    TodayReport.from(row.todayReport()),
                    row.openAlerts());
        }
    }

    public static FarmDashboardResponse from(FarmDashboard farm) {
        return new FarmDashboardResponse(
                farm.period(),
                farm.from(),
                farm.to(),
                farm.activeSectors(),
                farm.reportingSectors(),
                IndicatorsResponse.from(farm.indicators()),
                farm.trend().stream().map(Day::from).toList(),
                farm.target(),
                farm.targetStatus(),
                farm.grades() == null ? null : Grading.from(farm.grades()),
                farm.sectors().stream().map(SectorRow::from).toList());
    }
}
