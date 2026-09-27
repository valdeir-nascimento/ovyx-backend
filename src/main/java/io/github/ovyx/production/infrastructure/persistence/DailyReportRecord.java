package io.github.ovyx.production.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representacao do relatorio na tabela {@code daily_report}.
 *
 * <p>Separada do agregado {@code DailyReport} de proposito, como no identity e no farm: anotar o agregado
 * com JPA faria o ORM ditar o modelo de dominio. Esta classe nao tem comportamento de negocio.
 */
@Entity
@Table(name = "daily_report")
public class DailyReportRecord {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "sector_id", nullable = false, updatable = false)
    private UUID sectorId;

    /**
     * O dia e a hora da granja, gravados como estao (R-006). Direto pelo JDBC: com o fuso do JDBC em UTC
     * ({@code hibernate.jdbc.time_zone}), o Hibernate convertia a hora local pelo fuso da JVM, e 06:30
     * virava 09:30 no banco.
     */
    @JdbcType(LocalDateJdbcType.class)
    @Column(name = "collection_date", nullable = false)
    private LocalDate collectionDate;

    @JdbcType(LocalTimeJdbcType.class)
    @Column(name = "collection_time", nullable = false)
    private LocalTime collectionTime;

    @Column(name = "opening_bird_count", nullable = false)
    private int openingBirdCount;

    @Column(name = "flock_age", nullable = false)
    private int flockAge;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "no_mortality_confirmed", nullable = false)
    private boolean noMortalityConfirmed;

    @Column(name = "opened_by_id", nullable = false, updatable = false)
    private UUID openedById;

    @Column(name = "opened_by_name", nullable = false, length = 120, updatable = false)
    private String openedByName;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "last_corrected_by_id")
    private UUID lastCorrectedById;

    @Column(name = "last_corrected_by_name", length = 120)
    private String lastCorrectedByName;

    @Column(name = "last_corrected_at")
    private Instant lastCorrectedAt;

    /**
     * Versao da linha, para controle otimista de concorrencia (R-003). A versao e do relatorio inteiro,
     * gaiolas inclusive: quem grava so uma gaiola precisa sobe-la (ver {@code JpaDailyReportRepository}).
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /**
     * As gaiolas do relatorio, gravadas com ele (R-003). A chave da gaiola ja traz o relatorio, e por isso
     * a coluna de juncao nao e escrita por aqui. Sem remocao de orfaos: nada e apagado (FR-021).
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", referencedColumnName = "id", insertable = false, updatable = false)
    @OrderBy("battery ASC, number ASC")
    private List<ReportCageRecord> cages = new ArrayList<>();

    /** Exigido pelo Hibernate. */
    protected DailyReportRecord() {}

    DailyReportRecord(
            UUID id,
            UUID sectorId,
            LocalDate collectionDate,
            LocalTime collectionTime,
            int openingBirdCount,
            int flockAge,
            String note,
            boolean noMortalityConfirmed,
            UUID openedById,
            String openedByName,
            Instant openedAt,
            UUID lastCorrectedById,
            String lastCorrectedByName,
            Instant lastCorrectedAt) {
        this.id = id;
        this.sectorId = sectorId;
        this.collectionDate = collectionDate;
        this.collectionTime = collectionTime;
        this.openingBirdCount = openingBirdCount;
        this.flockAge = flockAge;
        this.note = note;
        this.noMortalityConfirmed = noMortalityConfirmed;
        this.openedById = openedById;
        this.openedByName = openedByName;
        this.openedAt = openedAt;
        this.lastCorrectedById = lastCorrectedById;
        this.lastCorrectedByName = lastCorrectedByName;
        this.lastCorrectedAt = lastCorrectedAt;
    }

    /** Aplica os dados gerais do agregado sobre a linha carregada, preservando a identidade e a abertura. */
    void apply(
            LocalDate collectionDate,
            LocalTime collectionTime,
            int openingBirdCount,
            int flockAge,
            String note,
            boolean noMortalityConfirmed,
            UUID lastCorrectedById,
            String lastCorrectedByName,
            Instant lastCorrectedAt) {
        this.collectionDate = collectionDate;
        this.collectionTime = collectionTime;
        this.openingBirdCount = openingBirdCount;
        this.flockAge = flockAge;
        this.note = note;
        this.noMortalityConfirmed = noMortalityConfirmed;
        this.lastCorrectedById = lastCorrectedById;
        this.lastCorrectedByName = lastCorrectedByName;
        this.lastCorrectedAt = lastCorrectedAt;
    }

    List<ReportCageRecord> getCages() {
        return cages;
    }

    UUID getId() {
        return id;
    }

    UUID getSectorId() {
        return sectorId;
    }

    LocalDate getCollectionDate() {
        return collectionDate;
    }

    LocalTime getCollectionTime() {
        return collectionTime;
    }

    int getOpeningBirdCount() {
        return openingBirdCount;
    }

    int getFlockAge() {
        return flockAge;
    }

    String getNote() {
        return note;
    }

    boolean isNoMortalityConfirmed() {
        return noMortalityConfirmed;
    }

    UUID getOpenedById() {
        return openedById;
    }

    String getOpenedByName() {
        return openedByName;
    }

    Instant getOpenedAt() {
        return openedAt;
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
}
