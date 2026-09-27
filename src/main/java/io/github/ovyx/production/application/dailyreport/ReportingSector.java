package io.github.ovyx.production.application.dailyreport;

import java.util.UUID;

/**
 * O setor do relatorio, como a tela precisa dele: o nome para o cabecalho e a situacao, que esconde as
 * acoes de escrita num setor inativo (FR-020).
 *
 * @param status {@code ACTIVE} ou {@code INACTIVE}
 */
public record ReportingSector(UUID id, String name, String status) {}
