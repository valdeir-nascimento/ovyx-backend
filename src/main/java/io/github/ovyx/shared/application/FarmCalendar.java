package io.github.ovyx.shared.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * "Hoje" e "agora" no fuso da granja (R-006 da 003; R-004 da 005).
 *
 * <p>O relatorio e a pesagem sao do dia da granja, e nao de um instante: a coleta das 23h30 e do dia local,
 * mesmo que em UTC ja seja o dia seguinte. O dominio recebe "hoje" daqui, e nao le relogio (principio I).
 *
 * <p>Fica no kernel porque o fuso e da granja, e nao de um contexto: o production e o farm decidem o dia do
 * mesmo jeito.
 */
public final class FarmCalendar {

    private final Clock clock;
    private final ZoneId zone;

    public FarmCalendar(Clock clock, ZoneId zone) {
        this.clock = clock;
        this.zone = zone;
    }

    /** O dia de hoje na granja. */
    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), zone);
    }

    /** A hora de agora na granja, sem os segundos: a hora da coleta e em horas e minutos. */
    public LocalTime now() {
        return LocalTime.ofInstant(clock.instant(), zone).truncatedTo(ChronoUnit.MINUTES);
    }
}
