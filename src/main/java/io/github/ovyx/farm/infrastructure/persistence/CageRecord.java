package io.github.ovyx.farm.infrastructure.persistence;

import io.github.ovyx.farm.domain.model.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Representacao da gaiola na tabela {@code cage}, gravada junto com o setor que a contem (R-003).
 *
 * <p>Sem comportamento de negocio. O setor da gaiola e a coluna da relacao do {@link SectorRecord}, e
 * nao muda depois do cadastro.
 */
@Entity
@Table(name = "cage")
public class CageRecord {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "battery", nullable = false, length = 3)
    private String battery;

    @Column(name = "number", nullable = false)
    private int number;

    @Column(name = "bird_count", nullable = false)
    private int birdCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Status status;

    @Column(name = "deactivated_with_sector", nullable = false)
    private boolean deactivatedWithSector;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Exigido pelo Hibernate. */
    protected CageRecord() {}

    CageRecord(
            UUID id,
            String battery,
            int number,
            int birdCount,
            Status status,
            boolean deactivatedWithSector,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.battery = battery;
        this.number = number;
        this.birdCount = birdCount;
        this.status = status;
        this.deactivatedWithSector = deactivatedWithSector;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void apply(
            String battery,
            int number,
            int birdCount,
            Status status,
            boolean deactivatedWithSector,
            Instant updatedAt) {
        this.battery = battery;
        this.number = number;
        this.birdCount = birdCount;
        this.status = status;
        this.deactivatedWithSector = deactivatedWithSector;
        this.updatedAt = updatedAt;
    }

    UUID getId() {
        return id;
    }

    String getBattery() {
        return battery;
    }

    int getNumber() {
        return number;
    }

    int getBirdCount() {
        return birdCount;
    }

    Status getStatus() {
        return status;
    }

    boolean isDeactivatedWithSector() {
        return deactivatedWithSector;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
