package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.shared.domain.WeighingSchedule;
import java.util.List;

/**
 * O que a porta le para os alertas das gaiolas: as gaiolas ativas, na ordem da bateria e do numero, a faixa de
 * peso do setor, a mortalidade da semana anterior e a agenda de pesagem do setor.
 *
 * @param range a faixa de peso do setor; {@code null} sem faixa
 * @param schedule a agenda de pesagem do setor (feature 010); {@code null} em setor inativo, que nao tem aviso
 */
public record CageWatchReading(
        List<CageWatch> cages, ReferenceWeight range, MortalityBaseline baseline, WeighingSchedule schedule) {

    public CageWatchReading {
        cages = List.copyOf(cages);
    }
}
