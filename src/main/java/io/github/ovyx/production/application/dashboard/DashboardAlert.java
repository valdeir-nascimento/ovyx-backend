package io.github.ovyx.production.application.dashboard;

/**
 * Um alerta ou uma pendencia de hoje (FR-015 a FR-017 da 006): o que aconteceu, com os numeros que o
 * explicam, e o destino da tela onde se resolve. Derivado a cada consulta; nao e gravado nem dispensado.
 */
public record DashboardAlert(AlertKind kind, AlertTone tone, String title, String detail, AlertTarget target) {}
