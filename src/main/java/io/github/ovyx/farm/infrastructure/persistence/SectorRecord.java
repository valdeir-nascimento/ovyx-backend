package io.github.ovyx.farm.infrastructure.persistence;

import io.github.ovyx.farm.domain.model.Status;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representacao do setor na tabela {@code sector}.
 *
 * <p>Separada do agregado {@code Sector} de proposito, como no identity: anotar o agregado com JPA
 * faria o ORM ditar o modelo de dominio. Esta classe nao tem comportamento de negocio.
 */
@Entity
@Table(name = "sector")
public class SectorRecord {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Versao da linha, para controle otimista de concorrencia (R-003).
     *
     * <p>O tratador carrega o setor, o dominio decide e o {@code save} aplica o resultado sobre a mesma
     * linha carregada, na mesma transacao. Se outro comando gravou nesse meio-tempo, a versao nao
     * confere, o banco recusa, e o despachante repete o comando sobre o estado atual.
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /**
     * As gaiolas do setor, gravadas com ele (R-003). Sem remocao de orfaos, de proposito: nada e
     * apagado (FR-012), e uma gaiola que sumisse da lista continuaria na tabela.
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false, updatable = false)
    private List<CageRecord> cages = new ArrayList<>();

    /** Exigido pelo Hibernate. */
    protected SectorRecord() {}

    SectorRecord(
            UUID id, String name, String description, Status status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Aplica o estado do agregado sobre a linha carregada, preservando a identidade e o cadastro. */
    void apply(String name, String description, Status status, Instant updatedAt) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    List<CageRecord> getCages() {
        return cages;
    }

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
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
