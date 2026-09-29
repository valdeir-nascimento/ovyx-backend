package io.github.ovyx.production.application.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * A classificacao dos ovos do periodo (FR-014 da 006): os padrao, que sao os coletados menos os
 * classificados, e cada classe fora do padrao, na ordem da tela de producao.
 */
public record EggGrading(int collected, EggGradeShare standard, List<EggGradeShare> shares) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public EggGrading {
        shares = List.copyOf(shares);
    }

    /** A classificacao dos dias dados; {@code null} sem ovo. */
    static EggGrading of(List<ReportDay> days) {
        int collected = sum(days, ReportDay::eggs);
        if (collected == 0) {
            return null;
        }
        int graded = sum(days, ReportDay::graded);
        return new EggGrading(
                collected,
                share("standard", collected - graded, collected),
                List.of(
                        share("small", sum(days, ReportDay::small), collected),
                        share("jumbo", sum(days, ReportDay::jumbo), collected),
                        share("dirty", sum(days, ReportDay::dirty), collected),
                        share("cracked", sum(days, ReportDay::cracked), collected),
                        share("bloodSpot", sum(days, ReportDay::bloodSpot), collected),
                        share("abnormal", sum(days, ReportDay::abnormal), collected)));
    }

    private static EggGradeShare share(String grade, int count, int collected) {
        BigDecimal percent = BigDecimal.valueOf(count)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(collected), 1, RoundingMode.HALF_UP);
        return new EggGradeShare(grade, count, percent);
    }

    private static int sum(List<ReportDay> days, ToIntFunction<ReportDay> field) {
        return days.stream().mapToInt(field).sum();
    }
}
