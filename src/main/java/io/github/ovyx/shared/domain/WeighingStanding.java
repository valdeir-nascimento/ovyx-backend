package io.github.ovyx.shared.domain;

import java.time.LocalDate;

/**
 * A situacao de uma gaiola ativa na agenda de pesagem do setor (feature 010). Derivada a cada consulta, com o
 * dia de hoje da granja; nao e gravada.
 *
 * <p>Nao e o {@code WeighingStatus} do farm, que diz se uma pesagem e valida ou anulada.
 *
 * @param lateSince so na atrasada: o ultimo dia de pesagem que passou sem ela; sem dia, o dia seguinte ao fim
 *     do prazo de 7 dias
 * @param nextOn so na em dia: o proximo dia de pesagem; sem dia, 7 dias depois da ultima
 */
public record WeighingStanding(WeighingSituation situation, LocalDate lateSince, LocalDate nextOn) {

    public WeighingStanding {
        if (situation == null) {
            throw new IllegalArgumentException("A situacao da pesagem e obrigatoria.");
        }
        if ((situation == WeighingSituation.LATE) != (lateSince != null)) {
            throw new IllegalArgumentException("A data do atraso vem so, e sempre, na pesagem atrasada.");
        }
        if ((situation == WeighingSituation.UP_TO_DATE) != (nextOn != null)) {
            throw new IllegalArgumentException("A proxima pesagem vem so, e sempre, na gaiola em dia.");
        }
    }

    /** Se a gaiola falta pesar: tudo, menos em dia. */
    public boolean isPending() {
        return situation != WeighingSituation.UP_TO_DATE;
    }
}
