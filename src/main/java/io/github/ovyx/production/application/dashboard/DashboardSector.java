package io.github.ovyx.production.application.dashboard;

import java.util.UUID;

/** Uma aba do painel: um setor ativo com ao menos um relatorio. */
public record DashboardSector(UUID id, String name) {}
