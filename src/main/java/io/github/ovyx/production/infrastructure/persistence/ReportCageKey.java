package io.github.ovyx.production.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

/** Chave da gaiola no relatorio: o relatorio e a gaiola do farm, como a chave primaria da tabela. */
@Embeddable
public record ReportCageKey(
        @Column(name = "report_id", nullable = false, updatable = false) UUID reportId,
        @Column(name = "cage_id", nullable = false, updatable = false) UUID cageId)
        implements Serializable {}
