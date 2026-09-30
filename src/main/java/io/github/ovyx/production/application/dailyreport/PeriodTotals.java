package io.github.ovyx.production.application.dailyreport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * As contas de um periodo com varios dias (R-007 da 007), as mesmas do painel e da linha de totais da planilha
 * dos relatorios.
 *
 * <p>A produtividade e a soma dos ovos sobre a soma das aves do inicio de cada dia, e nao a media das
 * produtividades: cada dia pesa pelo tamanho dele. O custo e o custo por ovo contam so os dias com a racao
 * completa; o custo e a soma exata, arredondada so no fim, e o custo por ovo sai dessa soma exata.
 *
 * @param days os dias com relatorio
 * @param eggs os ovos coletados nos dias
 * @param layingRate a produtividade, com duas casas; {@code null} sem dia ou sem ave
 * @param feedCost o custo de racao dos dias com a racao completa, com duas casas; {@code null} sem esses dias
 * @param costPerEgg o custo exato sobre os ovos desses dias, com tres casas; {@code null} sem eles ou sem ovo
 * @param incompleteDays os dias com relatorio que ficaram fora do custo, por a racao estar pendente
 */
public record PeriodTotals(
        int days, int eggs, BigDecimal layingRate, BigDecimal feedCost, BigDecimal costPerEgg, int incompleteDays) {

    private static final BigDecimal GRAMS_PER_KILOGRAM = BigDecimal.valueOf(1000);

    public static PeriodTotals of(List<PeriodDay> days) {
        int eggs = eggs(days);
        int birds = days.stream().mapToInt(PeriodDay::openingBirdCount).sum();
        List<PeriodDay> fed = days.stream().filter(PeriodDay::feedComplete).toList();
        BigDecimal exactCost = fed.stream()
                .map(PeriodDay::exactFeedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(GRAMS_PER_KILOGRAM);
        int fedEggs = eggs(fed);
        return new PeriodTotals(
                days.size(),
                eggs,
                days.isEmpty() || birds == 0 ? null : DailyReportTotals.percent(eggs, birds),
                fed.isEmpty() ? null : exactCost.setScale(2, RoundingMode.HALF_UP),
                fed.isEmpty() || fedEggs == 0 ? null : exactCost.divide(BigDecimal.valueOf(fedEggs), 3, RoundingMode.HALF_UP),
                days.size() - fed.size());
    }

    /** Os ovos do periodo; {@code null} sem dia com relatorio, e nao zero. */
    public BigDecimal production() {
        return days == 0 ? null : BigDecimal.valueOf(eggs);
    }

    private static int eggs(List<PeriodDay> days) {
        return days.stream().mapToInt(PeriodDay::eggs).sum();
    }
}
