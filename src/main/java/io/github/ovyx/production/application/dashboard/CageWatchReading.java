package io.github.ovyx.production.application.dashboard;

import java.util.List;

/**
 * O que a porta le para os alertas das gaiolas: as gaiolas ativas, na ordem da bateria e do numero, a faixa de
 * peso do setor e a mortalidade da semana anterior.
 *
 * @param range a faixa de peso do setor; {@code null} sem faixa
 */
public record CageWatchReading(List<CageWatch> cages, ReferenceWeight range, MortalityBaseline baseline) {

    public CageWatchReading {
        cages = List.copyOf(cages);
    }
}
