package io.github.ovyx.shared.domain;

/** A pesagem da semana de uma gaiola ativa, diante da agenda do setor (feature 010). */
public enum WeighingSituation {
    /** Com a pesagem da semana. */
    UP_TO_DATE,
    /** Hoje e o dia da pesagem, e falta a da semana. */
    DUE_TODAY,
    /** O dia da pesagem passou sem ela; ou, sem dia, a ultima tem mais de 7 dias. */
    LATE,
    /** Sem nenhuma pesagem valida. */
    NEVER_WEIGHED
}
