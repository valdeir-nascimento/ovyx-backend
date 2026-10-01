package io.github.ovyx.production.application.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * A meta da granja toda (R-005 da 009): a media das metas dos setores ponderada pelas aves do inicio do dia dos
 * relatorios. E a produtividade que a granja atinge quando cada setor atinge a sua; a media simples daria o mesmo peso a
 * um galpao de 200 aves e a um de 20.000.
 *
 * <p>Cada relatorio e uma parte: o setor com mais relatorios no recorte pesa mais, como pesa na produtividade somada.
 */
final class FarmTarget {

    private static final int SCALE = 2;

    private FarmTarget() {}

    /**
     * Uma parte da meta: a meta do setor e as aves do inicio do dia de um relatorio dele.
     *
     * @param birds as aves do inicio do dia do relatorio
     */
    record WeightedTarget(LayingRateTarget target, int birds) {}

    /**
     * A meta ponderada das partes, com duas casas, arredondada so no fim; {@code null} sem parte ou sem ave.
     */
    static LayingRateTarget of(List<WeightedTarget> parts) {
        long birds = parts.stream().mapToLong(WeightedTarget::birds).sum();
        if (birds == 0) {
            return null;
        }
        BigDecimal weighted = parts.stream()
                .map(part -> part.target().value().multiply(BigDecimal.valueOf(part.birds())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new LayingRateTarget(weighted.divide(BigDecimal.valueOf(birds), SCALE, RoundingMode.HALF_UP));
    }
}
