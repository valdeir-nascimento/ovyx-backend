package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Os totais do dia, derivados das gaiolas do relatorio e das aves do inicio do dia (spec, Key Entities):
 * nunca informados a mao, sempre calculados na leitura (R-008). Na feature 004, tambem os da racao.
 */
public final class DailyReportTotals {

    private static final BigDecimal GRAMS_PER_KILOGRAM = BigDecimal.valueOf(1000);

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

    /**
     * Os totais de racao das gaiolas lancadas, e a situacao do lancamento (R-007 da 004): o custo do dia e
     * a soma exata das gaiolas, arredondada so no fim, e o custo por ovo sai dessa soma exata; o esperado do
     * dia e o de cada gaiola pesado pelas aves dela.
     *
     * @param collectedEggs os ovos coletados no dia, das gaiolas com producao lancada
     */
    public static FeedTotals feed(List<ReportCageDetail> cages, int collectedEggs) {
        List<ReportCageDetail> fed = cages.stream().filter(cage -> cage.feed() != null).toList();
        int pending = cages.size() - fed.size();
        int consumption = fed.stream().mapToInt(cage -> cage.feed().consumption()).sum();
        BigDecimal exactCost = fed.stream()
                .map(cage -> BigDecimal.valueOf(cage.feed().consumption()).multiply(cage.feed().pricePerKg()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(GRAMS_PER_KILOGRAM);
        int birds = fed.stream().mapToInt(ReportCageDetail::birdCount).sum();
        long expected = fed.stream()
                .mapToLong(cage -> (long) cage.birdCount() * cage.feed().expectedIntake())
                .sum();
        return new FeedTotals(
                FeedStatus.of(pending),
                pending,
                consumption,
                exactCost.setScale(2, RoundingMode.HALF_UP),
                collectedEggs == 0 || fed.isEmpty()
                        ? null
                        : exactCost.divide(BigDecimal.valueOf(collectedEggs), 3, RoundingMode.HALF_UP),
                birds == 0 ? null : oneDecimal(consumption, birds),
                birds == 0 ? null : oneDecimal(expected, birds));
    }

    /** A parte sobre o todo, com uma casa, arredondada para cima a partir da metade. */
    private static BigDecimal oneDecimal(long part, int whole) {
        return BigDecimal.valueOf(part).divide(BigDecimal.valueOf(whole), 1, RoundingMode.HALF_UP);
    }

    /** A parte sobre o todo, em porcentagem, com duas casas, arredondada para cima a partir da metade. */
    public static BigDecimal percent(int part, int whole) {
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }
}
