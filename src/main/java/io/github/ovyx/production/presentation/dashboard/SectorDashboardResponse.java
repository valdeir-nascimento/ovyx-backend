package io.github.ovyx.production.presentation.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ovyx.production.application.dashboard.AlertKind;
import io.github.ovyx.production.application.dashboard.AlertTarget;
import io.github.ovyx.production.application.dashboard.AlertTone;
import io.github.ovyx.production.application.dashboard.DashboardAlert;
import io.github.ovyx.production.application.dashboard.DashboardDay;
import io.github.ovyx.production.application.dashboard.DashboardPeriod;
import io.github.ovyx.production.application.dashboard.EggGradeShare;
import io.github.ovyx.production.application.dashboard.EggGrading;
import io.github.ovyx.production.application.dashboard.GoodDirection;
import io.github.ovyx.production.application.dashboard.Indicator;
import io.github.ovyx.production.application.dashboard.Indicators;
import io.github.ovyx.production.application.dashboard.LatestReport;
import io.github.ovyx.production.application.dashboard.SectorDashboard;
import io.github.ovyx.production.application.dashboard.TargetStatus;
import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import io.github.ovyx.production.presentation.dailyreport.ActorResponse;
import io.github.ovyx.production.presentation.dailyreport.ReportingSectorResponse;
import io.github.ovyx.production.presentation.dailyreport.Times;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O painel de um setor num periodo (feature 006). O que nao tem valor fica fora do corpo, e nao zero: o dia
 * sem relatorio, o custo sem racao completa, a variacao sem o periodo anterior (FR-009).
 */
@Schema(name = "SectorDashboard", description = "O painel de um setor num período")
public record SectorDashboardResponse(
        ReportingSectorResponse sector,
        @Schema(example = "TODAY") DashboardPeriod period,
        @Schema(description = "O primeiro dia do período", example = "2026-09-18") LocalDate from,
        @Schema(description = "O último dia do período", example = "2026-09-24") LocalDate to,
        @Schema(description = "O relatório de hoje; ausente se hoje ainda não foi aberto")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        TodayReport todayReport,
        IndicatorsResponse indicators,
        @Schema(description = "Os 7 dias de hoje − 6 até hoje, para as tendências e os gráficos") List<Day> trend,
        @Schema(description = "A meta de produtividade, em porcentagem", example = "85.00") BigDecimal target,
        @Schema(
                description = "O último dia com relatório diante da meta; ausente sem dia com relatório",
                example = "ABOVE")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        TargetStatus targetStatus,
        @Schema(description = "A classificação dos ovos do período; ausente sem ovo")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Grading grades,
        @Schema(description = "Os alertas e as pendências de hoje") List<Alert> alerts,
        @Schema(description = "Quantos alertas e pendências estão abertos", example = "3") int openAlerts,
        @Schema(description = "Os 4 relatórios mais recentes, do mais novo para o mais antigo")
        List<Latest> latestReports) {

    /** O relatorio de hoje e a situacao de cada lancamento. */
    @Schema(name = "TodayReport", description = "O relatório de hoje do setor e a situação de cada lançamento")
    public record TodayReport(
            @Schema(example = "6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55") UUID id,
            @Schema(example = "COMPLETE") ProductionStatus productionStatus,
            @Schema(example = "COMPLETE") FeedStatus feedStatus,
            @Schema(example = "RECORDED") MortalityStatus mortalityStatus) {

        static TodayReport from(io.github.ovyx.production.application.dashboard.TodayReport report) {
            return report == null
                    ? null
                    : new TodayReport(
                            report.id(), report.productionStatus(), report.feedStatus(), report.mortalityStatus());
        }
    }

    /** Os quatro indicadores. */
    @Schema(name = "Indicators", description = "Os quatro indicadores do painel")
    public record IndicatorsResponse(
            IndicatorResponse production,
            IndicatorResponse layingRate,
            IndicatorResponse feedCost,
            IndicatorResponse costPerEgg) {

        static IndicatorsResponse from(Indicators indicators) {
            return new IndicatorsResponse(
                    IndicatorResponse.from(indicators.production()),
                    IndicatorResponse.from(indicators.layingRate()),
                    IndicatorResponse.from(indicators.feedCost()),
                    IndicatorResponse.from(indicators.costPerEgg()));
        }
    }

    /** Um indicador do periodo, comparado com o anterior. */
    @Schema(
            name = "Indicator",
            description = "Um indicador do período, comparado com o período anterior. Sem dado, o valor fica"
                    + " ausente, e não zero.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IndicatorResponse(
            @Schema(description = "O valor do período", example = "1740") BigDecimal value,
            @Schema(description = "O valor do período anterior", example = "1700") BigDecimal previous,
            @Schema(
                    description = "A variação sobre o anterior, em porcentagem com uma casa (produção e custos) ou em"
                            + " pontos percentuais com duas casas (produtividade). Ausente sem os dois valores, ou"
                            + " com o anterior zero.",
                    example = "2.4")
            BigDecimal change,
            @Schema(description = "O sentido bom da variação", example = "UP") GoodDirection goodDirection,
            @Schema(
                    description = "Os dias do período com o lançamento pendente (a produção, na produção e na"
                            + " produtividade; a ração, nos custos)",
                    example = "1")
            int incompleteDays) {

        static IndicatorResponse from(Indicator indicator) {
            return new IndicatorResponse(
                    indicator.value(),
                    indicator.previous(),
                    indicator.change(),
                    indicator.goodDirection(),
                    indicator.incompleteDays());
        }
    }

    /** Um dia da serie dos ultimos 7 dias. */
    @Schema(
            name = "DashboardDay",
            description = "Um dia da série dos últimos 7 dias. Sem relatório, os valores ficam ausentes; com a ração"
                    + " pendente, os custos ficam ausentes.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Day(
            @Schema(example = "2026-09-24") LocalDate date,
            @Schema(description = "Ovos coletados", example = "1740") Integer production,
            @Schema(description = "Produtividade, em porcentagem com duas casas", example = "87.00") BigDecimal layingRate,
            @Schema(description = "Custo da ração, em reais com duas casas", example = "159.60") BigDecimal feedCost,
            @Schema(description = "Custo por ovo, em reais com três casas", example = "0.092") BigDecimal costPerEgg) {

        static Day from(DashboardDay day) {
            return new Day(day.date(), day.production(), day.layingRate(), day.feedCost(), day.costPerEgg());
        }
    }

    /** A classificacao dos ovos do periodo. */
    @Schema(name = "EggGrading", description = "A classificação dos ovos do período")
    public record Grading(
            @Schema(example = "1740") int collected,
            Share standard,
            @Schema(description = "As seis classes fora do padrão, nesta ordem") List<Share> shares) {

        static Grading from(EggGrading grading) {
            return grading == null
                    ? null
                    : new Grading(
                            grading.collected(),
                            Share.from(grading.standard()),
                            grading.shares().stream().map(Share::from).toList());
        }
    }

    /** Uma classe de ovos, com a quantidade e a porcentagem sobre os coletados. */
    @Schema(name = "EggGradeShare", description = "Uma classe de ovos, com a quantidade e a porcentagem sobre os coletados")
    public record Share(
            @Schema(example = "cracked") String grade,
            @Schema(example = "18") int count,
            @Schema(description = "A quantidade sobre os coletados, com uma casa", example = "1.0") BigDecimal percent) {

        static Share from(EggGradeShare share) {
            return new Share(share.grade(), share.count(), share.percent());
        }
    }

    /** Um alerta ou uma pendencia de hoje. */
    @Schema(name = "DashboardAlert", description = "Um alerta ou uma pendência de hoje, com o que aconteceu e para onde leva")
    public record Alert(
            @Schema(example = "HIGH_MORTALITY") AlertKind kind,
            @Schema(example = "WARNING") AlertTone tone,
            @Schema(example = "Mortalidade acima da média na gaiola B-07") String title,
            @Schema(example = "2 aves removidas hoje; a média do setor é 0,4 por gaiola ao dia.") String detail,
            Target target) {

        static Alert from(DashboardAlert alert) {
            return new Alert(alert.kind(), alert.tone(), alert.title(), alert.detail(), Target.from(alert.target()));
        }
    }

    /** Para onde o alerta leva; sem campo, a abertura do relatorio de hoje. */
    @Schema(name = "AlertTarget", description = "Para onde o alerta leva. Sem campo, é a abertura do relatório de hoje.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Target(
            @Schema(description = "O relatório de hoje, quando existe", example = "6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55")
            UUID reportId,
            @Schema(description = "A gaiola, nos alertas de gaiola", example = "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44")
            UUID cageId,
            @Schema(example = "B-07") String cageCode) {

        static Target from(AlertTarget target) {
            return new Target(target.reportId(), target.cageId(), target.cageCode());
        }
    }

    /** Um dos relatorios mais recentes do setor. */
    @Schema(name = "LatestReport", description = "Um dos relatórios mais recentes do setor")
    public record Latest(
            @Schema(example = "6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55") UUID id,
            @Schema(example = "2026-09-24") LocalDate collectionDate,
            @Schema(example = "06:30") String collectionTime,
            ActorResponse openedBy,
            @Schema(example = "1740") int collectedEggs,
            @Schema(example = "3") int removedBirds,
            @Schema(example = "COMPLETE") ProductionStatus productionStatus,
            @Schema(example = "COMPLETE") FeedStatus feedStatus,
            @Schema(example = "RECORDED") MortalityStatus mortalityStatus) {

        static Latest from(LatestReport report) {
            return new Latest(
                    report.id(),
                    report.collectionDate(),
                    Times.format(report.collectionTime()),
                    ActorResponse.from(report.openedBy()),
                    report.collectedEggs(),
                    report.removedBirds(),
                    report.productionStatus(),
                    report.feedStatus(),
                    report.mortalityStatus());
        }
    }

    public static SectorDashboardResponse from(SectorDashboard dashboard) {
        return new SectorDashboardResponse(
                ReportingSectorResponse.from(dashboard.sector()),
                dashboard.period(),
                dashboard.from(),
                dashboard.to(),
                TodayReport.from(dashboard.todayReport()),
                IndicatorsResponse.from(dashboard.indicators()),
                dashboard.trend().stream().map(Day::from).toList(),
                dashboard.target(),
                dashboard.targetStatus(),
                Grading.from(dashboard.grades()),
                dashboard.alerts().stream().map(Alert::from).toList(),
                dashboard.openAlerts(),
                dashboard.latestReports().stream().map(Latest::from).toList());
    }
}
