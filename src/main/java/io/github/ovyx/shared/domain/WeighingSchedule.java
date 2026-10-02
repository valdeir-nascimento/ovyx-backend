package io.github.ovyx.shared.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * A agenda de pesagem de um setor (R-001 e R-002 da feature 010).
 *
 * <p>Com o dia da pesagem, a semana sao os 7 dias que terminam no ultimo dia de pesagem que ja chegou: a gaiola
 * com uma pesagem valida desde o primeiro deles esta em dia. A pesagem feita depois do dia, antes do proximo,
 * tambem vale para a semana, e assim quem pesa no sabado em vez da sexta resolve o atraso. Sem dia, vale o
 * prazo de 7 dias desde a ultima pesagem.
 *
 * <p>Fica no kernel porque o farm (a lista de gaiolas, o filtro, a planilha e a tela Peso medio) e o production
 * (os avisos do painel) precisam da mesma resposta, e um contexto nao depende do outro. A regra nao sabe o que e
 * setor ou gaiola: recebe o dia da semana e as datas.
 *
 * @param weighingDay o dia da semana da pesagem; {@code null} quando o setor segue o prazo de 7 dias
 */
public record WeighingSchedule(DayOfWeek weighingDay) {

    /** Os dias da semana da pesagem antes do dia dela. */
    private static final int DAYS_BEFORE_THE_WEIGHING_DAY = 6;
    /** O prazo sem dia definido, e a distancia entre dois dias de pesagem. */
    private static final int WEEK = 7;

    /** Se hoje e o dia da pesagem; nunca, sem dia definido. */
    public boolean isWeighingDay(LocalDate today) {
        return weighingDay != null && today.getDayOfWeek() == weighingDay;
    }

    /** O ultimo dia de pesagem que ja chegou, hoje incluido; {@code null} sem dia definido. */
    public LocalDate dueOn(LocalDate today) {
        return weighingDay == null ? null : today.with(TemporalAdjusters.previousOrSame(weighingDay));
    }

    /** O primeiro dia em que uma pesagem conta para a semana. */
    public LocalDate requiredSince(LocalDate today) {
        return weighingDay == null
                ? today.minusDays(WEEK)
                : dueOn(today).minusDays(DAYS_BEFORE_THE_WEIGHING_DAY);
    }

    /**
     * A situacao de uma gaiola ativa.
     *
     * @param lastWeighedOn o dia da ultima pesagem valida; {@code null} se a gaiola nunca foi pesada
     */
    public WeighingStanding standingOf(LocalDate today, LocalDate lastWeighedOn) {
        if (lastWeighedOn == null) {
            return new WeighingStanding(WeighingSituation.NEVER_WEIGHED, null, null);
        }
        if (!lastWeighedOn.isBefore(requiredSince(today))) {
            LocalDate nextOn = weighingDay == null ? lastWeighedOn.plusDays(WEEK) : dueOn(today).plusDays(WEEK);
            return new WeighingStanding(WeighingSituation.UP_TO_DATE, null, nextOn);
        }
        if (isWeighingDay(today)) {
            return new WeighingStanding(WeighingSituation.DUE_TODAY, null, null);
        }
        LocalDate lateSince = weighingDay == null ? lastWeighedOn.plusDays(WEEK + 1) : dueOn(today);
        return new WeighingStanding(WeighingSituation.LATE, lateSince, null);
    }
}
