package io.github.ovyx.production.application.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * "Hoje" e "agora" no fuso da granja (R-006).
 *
 * <p>O relatorio e do dia da granja, e nao de um instante: a coleta das 23h30 e do dia local, mesmo que
 * em UTC ja seja o dia seguinte. O dominio recebe "hoje" daqui, e nao le relogio (principio I).
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
