package io.github.ovyx.production.application.dashboard;

/** O tipo de um alerta ou de uma pendencia do painel (R-007 da 006), na ordem em que aparecem. */
public enum AlertKind {
    REPORT_NOT_OPENED,
    PRODUCTION_PENDING,
    FEED_PENDING,
    MORTALITY_PENDING,
    HIGH_MORTALITY,
    LOW_LAYING,
    WEIGHT_OUT_OF_RANGE
}
