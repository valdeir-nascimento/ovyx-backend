package io.github.ovyx.production.presentation.dailyreport;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** A hora como o contrato a publica: {@code HH:mm}, sem os segundos que o JSON poria. */
final class Times {

    private static final DateTimeFormatter HOURS_AND_MINUTES = DateTimeFormatter.ofPattern("HH:mm");

    private Times() {}

    static String format(LocalTime time) {
        return time == null ? null : HOURS_AND_MINUTES.format(time);
    }
}
