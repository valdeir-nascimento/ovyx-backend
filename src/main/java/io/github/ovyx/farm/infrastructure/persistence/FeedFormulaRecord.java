package io.github.ovyx.farm.infrastructure.persistence;

import io.github.ovyx.farm.domain.model.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Representacao da formula de racao na tabela {@code feed_formula}.
 *
 * <p>Separada do agregado {@code FeedFormula} de proposito, como a do setor. Esta classe nao tem
 * comportamento de negocio.
 */
@Entity
@Table(name = "feed_formula")
public class FeedFormulaRecord {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "price_per_kg", nullable = false, precision = 7, scale = 2)
    private BigDecimal pricePerKg;

    @Column(name = "expected_intake", nullable = false)
    private int expectedIntake;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Versao da linha, para controle otimista de concorrencia, como no setor. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Exigido pelo Hibernate. */
    protected FeedFormulaRecord() {}

    FeedFormulaRecord(
            UUID id,
            String name,
            BigDecimal pricePerKg,
            int expectedIntake,
            String description,
            Status status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.pricePerKg = pricePerKg;
        this.expectedIntake = expectedIntake;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Aplica o estado do agregado sobre a linha carregada, preservando a identidade e o cadastro. */
    void apply(
            String name,
            BigDecimal pricePerKg,
            int expectedIntake,
            String description,
            Status status,
            Instant updatedAt) {
        this.name = name;
        this.pricePerKg = pricePerKg;
        this.expectedIntake = expectedIntake;
        this.description = description;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }

    BigDecimal getPricePerKg() {
        return pricePerKg;
    }

    int getExpectedIntake() {
        return expectedIntake;
    }

    String getDescription() {
        return description;
    }

    Status getStatus() {
        return status;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
