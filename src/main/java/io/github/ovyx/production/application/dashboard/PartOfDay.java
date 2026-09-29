package io.github.ovyx.production.application.dashboard;

import java.time.LocalTime;

/** A parte do dia no relogio da granja, para a saudacao do painel (R-003 da 006). */
public enum PartOfDay {
    /** Das 5h as 11h59. */
    MORNING,
    /** Do meio-dia as 17h59. */
    AFTERNOON,
    /** Das 18h as 4h59. */
    EVENING;

    private static final LocalTime DAWN = LocalTime.of(5, 0);
    private static final LocalTime NOON = LocalTime.NOON;
    private static final LocalTime DUSK = LocalTime.of(18, 0);

    public static PartOfDay of(LocalTime time) {
        if (time.isBefore(DAWN) || !time.isBefore(DUSK)) {
            return EVENING;
        }
        return time.isBefore(NOON) ? MORNING : AFTERNOON;
    }
}
