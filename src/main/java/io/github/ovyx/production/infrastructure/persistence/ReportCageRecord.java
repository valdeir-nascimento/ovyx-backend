package io.github.ovyx.production.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representacao da gaiola do relatorio na tabela {@code report_cage}: a gaiola como estava na abertura,
 * com a producao, a mortalidade e a racao, nulas enquanto nao lancadas. Sem comportamento de negocio.
 */
@Entity
@Table(name = "report_cage")
public class ReportCageRecord {

    @EmbeddedId
    private ReportCageKey key;

    @Column(name = "battery", nullable = false, length = 3, updatable = false)
    private String battery;

    @Column(name = "number", nullable = false, updatable = false)
    private int number;

    @Column(name = "bird_count", nullable = false, updatable = false)
    private int birdCount;

    // A producao: as sete colunas nulas juntas enquanto nao lancada (ck_report_cage_production).
    @Column(name = "eggs")
    private Integer eggs;

    @Column(name = "small")
    private Integer small;

    @Column(name = "jumbo")
    private Integer jumbo;

    @Column(name = "dirty")
    private Integer dirty;

    @Column(name = "cracked")
    private Integer cracked;

    @Column(name = "blood_spot")
    private Integer bloodSpot;

    @Column(name = "abnormal")
    private Integer abnormal;

    // A mortalidade: mortes e descartes nulos juntos enquanto nao lancada (ck_report_cage_mortality).
    @Column(name = "deaths")
    private Integer deaths;

    @Column(name = "culls")
    private Integer culls;

    @Column(name = "mortality_note", length = 500)
    private String mortalityNote;

    // A racao (feature 004): as quatro colunas nulas juntas enquanto nao lancada (ck_report_cage_feed).
    @Column(name = "feed_formula_id")
    private UUID feedFormulaId;

    @Column(name = "feed_price_per_kg", precision = 7, scale = 2)
    private BigDecimal feedPricePerKg;

    @Column(name = "feed_expected_intake")
    private Integer feedExpectedIntake;

    @Column(name = "feed_consumption")
    private Integer feedConsumption;

    /** Exigido pelo Hibernate. */
    protected ReportCageRecord() {}

    ReportCageRecord(UUID reportId, UUID cageId, String battery, int number, int birdCount) {
        this.key = new ReportCageKey(reportId, cageId);
        this.battery = battery;
        this.number = number;
        this.birdCount = birdCount;
    }

    UUID getCageId() {
        return key.cageId();
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

    Integer getEggs() {
        return eggs;
    }

    Integer getSmall() {
        return small;
    }

    Integer getJumbo() {
        return jumbo;
    }

    Integer getDirty() {
        return dirty;
    }

    Integer getCracked() {
        return cracked;
    }

    Integer getBloodSpot() {
        return bloodSpot;
    }

    Integer getAbnormal() {
        return abnormal;
    }

    Integer getDeaths() {
        return deaths;
    }

    Integer getCulls() {
        return culls;
    }

    String getMortalityNote() {
        return mortalityNote;
    }

    UUID getFeedFormulaId() {
        return feedFormulaId;
    }

    BigDecimal getFeedPricePerKg() {
        return feedPricePerKg;
    }

    Integer getFeedExpectedIntake() {
        return feedExpectedIntake;
    }

    Integer getFeedConsumption() {
        return feedConsumption;
    }

    /** Grava a racao; as quatro nulas quando nao lancada. */
    void applyFeed(UUID formulaId, BigDecimal pricePerKg, Integer expectedIntake, Integer consumption) {
        this.feedFormulaId = formulaId;
        this.feedPricePerKg = pricePerKg;
        this.feedExpectedIntake = expectedIntake;
        this.feedConsumption = consumption;
    }

    /** Grava a mortalidade; mortes e descartes nulos quando nao lancada. */
    void applyMortality(Integer deaths, Integer culls, String mortalityNote) {
        this.deaths = deaths;
        this.culls = culls;
        this.mortalityNote = mortalityNote;
    }

    /** Grava a producao; todas nulas quando nao lancada. */
    void applyProduction(
            Integer eggs, Integer small, Integer jumbo, Integer dirty, Integer cracked, Integer bloodSpot, Integer abnormal) {
        this.eggs = eggs;
        this.small = small;
        this.jumbo = jumbo;
        this.dirty = dirty;
        this.cracked = cracked;
        this.bloodSpot = bloodSpot;
        this.abnormal = abnormal;
    }
}
