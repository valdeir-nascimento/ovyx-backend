package io.github.ovyx.farm.infrastructure.persistence;

import io.github.ovyx.farm.domain.model.WeighingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A linha da tabela {@code weighing} (R-012 da 005). */
@Entity
@Table(name = "weighing")
public class WeighingRecord {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "sector_id", nullable = false)
    private UUID sectorId;

    @Column(name = "cage_id", nullable = false)
    private UUID cageId;

    @Column(name = "weighed_on", nullable = false)
    private LocalDate weighedOn;

    @Column(name = "average_weight", nullable = false, precision = 6, scale = 1)
    private BigDecimal averageWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private WeighingStatus status;

    @Column(name = "recorded_by_id", nullable = false)
    private UUID recordedById;

    @Column(name = "recorded_by_name", nullable = false, length = 120)
    private String recordedByName;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "last_corrected_by_id")
    private UUID lastCorrectedById;

    @Column(name = "last_corrected_by_name", length = 120)
    private String lastCorrectedByName;

    @Column(name = "last_corrected_at")
    private Instant lastCorrectedAt;

    @Column(name = "voided_by_id")
    private UUID voidedById;

    @Column(name = "voided_by_name", length = 120)
    private String voidedByName;

    @Column(name = "voided_at")
    private Instant voidedAt;

    /** Versao da linha, para controle otimista de concorrencia, como no setor e na formula. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Exigido pelo Hibernate. */
    protected WeighingRecord() {}

    WeighingRecord(
            UUID id,
            UUID sectorId,
            UUID cageId,
            UUID recordedById,
            String recordedByName,
            Instant recordedAt) {
        this.id = id;
        this.sectorId = sectorId;
        this.cageId = cageId;
        this.recordedById = recordedById;
        this.recordedByName = recordedByName;
        this.recordedAt = recordedAt;
    }

    /** Aplica o estado do agregado sobre a linha, preservando a identidade, a gaiola e quem registrou. */
    void apply(
            LocalDate weighedOn,
            BigDecimal averageWeight,
            WeighingStatus status,
            UUID lastCorrectedById,
            String lastCorrectedByName,
            Instant lastCorrectedAt,
            UUID voidedById,
            String voidedByName,
            Instant voidedAt) {
        this.weighedOn = weighedOn;
        this.averageWeight = averageWeight;
        this.status = status;
        this.lastCorrectedById = lastCorrectedById;
        this.lastCorrectedByName = lastCorrectedByName;
        this.lastCorrectedAt = lastCorrectedAt;
        this.voidedById = voidedById;
        this.voidedByName = voidedByName;
        this.voidedAt = voidedAt;
    }

    UUID getId() {
        return id;
    }

    UUID getSectorId() {
        return sectorId;
    }

    UUID getCageId() {
        return cageId;
    }

    LocalDate getWeighedOn() {
        return weighedOn;
    }

    BigDecimal getAverageWeight() {
        return averageWeight;
    }

    WeighingStatus getStatus() {
        return status;
    }

    UUID getRecordedById() {
        return recordedById;
    }

    String getRecordedByName() {
        return recordedByName;
    }

    Instant getRecordedAt() {
        return recordedAt;
    }

    UUID getLastCorrectedById() {
        return lastCorrectedById;
    }

    String getLastCorrectedByName() {
        return lastCorrectedByName;
    }

    Instant getLastCorrectedAt() {
        return lastCorrectedAt;
    }

    UUID getVoidedById() {
        return voidedById;
    }

    String getVoidedByName() {
        return voidedByName;
    }

    Instant getVoidedAt() {
        return voidedAt;
    }
}
