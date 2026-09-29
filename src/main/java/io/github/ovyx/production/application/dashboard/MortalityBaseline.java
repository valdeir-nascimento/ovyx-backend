package io.github.ovyx.production.application.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A mortalidade do setor nos 7 dias antes de hoje, para o alerta de mortalidade acima da media (R-007 da 006).
 *
 * @param removedBirds as aves removidas nos relatorios desses dias
 * @param cageDays as gaiolas desses relatorios, somadas dia a dia
 */
public record MortalityBaseline(int removedBirds, int cageDays) {

    /** As aves removidas por gaiola ao dia; zero sem relatorio anterior. */
    public BigDecimal dailyAverage() {
        return cageDays == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(removedBirds).divide(BigDecimal.valueOf(cageDays), 4, RoundingMode.HALF_UP);
    }
}
