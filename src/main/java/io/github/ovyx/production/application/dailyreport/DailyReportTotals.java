package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Os totais do dia, derivados das gaiolas do relatorio e das aves do inicio do dia (spec, Key Entities):
 * nunca informados a mao, sempre calculados na leitura (R-008).
 */
public final class DailyReportTotals {

    private DailyReportTotals() {}

    /** Os totais de producao das gaiolas lancadas, e a situacao do lancamento. */
    public static ProductionTotals production(int openingBirdCount, List<ReportCageDetail> cages) {
        List<CageProduction> recorded =
                cages.stream().map(ReportCageDetail::production).filter(Objects::nonNull).toList();
        int collected = recorded.stream().mapToInt(CageProduction::eggs).sum();
        int graded = recorded.stream()
                .mapToInt(entry -> entry.small() + entry.jumbo() + entry.dirty() + entry.cracked() + entry.bloodSpot()
                        + entry.abnormal())
                .sum();
        int unsellable = recorded.stream()
                .mapToInt(entry -> entry.cracked() + entry.bloodSpot() + entry.abnormal())
                .sum();
        int pending = cages.size() - recorded.size();
        return new ProductionTotals(
                ProductionStatus.of(pending),
                pending,
                collected,
                collected - graded,
                unsellable,
                percent(collected, openingBirdCount));
    }

    /** Os totais de mortalidade, o saldo de aves e a situacao do lancamento. */
    public static MortalityTotals mortality(
            int openingBirdCount, boolean noMortalityConfirmed, List<ReportCageDetail> cages) {
        List<CageMortality> recorded =
                cages.stream().map(ReportCageDetail::mortality).filter(Objects::nonNull).toList();
        int deaths = recorded.stream().mapToInt(CageMortality::deaths).sum();
        int culls = recorded.stream().mapToInt(CageMortality::culls).sum();
        return new MortalityTotals(
                MortalityStatus.of(noMortalityConfirmed, deaths + culls),
                deaths,
                culls,
                percent(deaths + culls, openingBirdCount),
                openingBirdCount - deaths - culls);
    }

    /** A parte sobre o todo, em porcentagem, com duas casas, arredondada para cima a partir da metade. */
    public static BigDecimal percent(int part, int whole) {
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }
}
