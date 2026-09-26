package io.github.ovyx.production.domain.model;

import io.github.ovyx.production.domain.valueobject.MortalityEntry;
import io.github.ovyx.production.domain.valueobject.ProductionEntry;
import java.util.Objects;
import java.util.Optional;

/**
 * Uma gaiola do relatorio, como estava na abertura (R-003): o relatorio de ontem nao muda quando o
 * administrador corrige a gaiola hoje. Entidade dentro do agregado {@link DailyReport}, identificada pela
 * gaiola do farm.
 *
 * <p>Os lancamentos do dia ficam nela: a producao e a mortalidade, cada uma ausente enquanto nao
 * lancada. Quem os grava e o relatorio, que checa as invariantes antes. A mortalidade nao muda as aves
 * da gaiola (FR-015).
 */
public final class ReportCage {

    private final CageId cageId;
    private final String battery;
    private final int number;
    private final int birdCount;
    private ProductionEntry production;
    private MortalityEntry mortality;

    private ReportCage(
            CageId cageId,
            String battery,
            int number,
            int birdCount,
            ProductionEntry production,
            MortalityEntry mortality) {
        this.cageId = Objects.requireNonNull(cageId, "cageId");
        this.battery = Objects.requireNonNull(battery, "battery");
        this.number = number;
        this.birdCount = birdCount;
        this.production = production;
        this.mortality = mortality;
    }

    /** A gaiola do farm, fixada no relatorio que se abre. */
    static ReportCage fixed(FarmCage cage) {
        return new ReportCage(cage.id(), cage.battery(), cage.number(), cage.birdCount(), null, null);
    }

    /**
     * Reconstroi a gaiola do relatorio a partir do que foi gravado.
     *
     * @param production a producao lancada, ou {@code null}
     * @param mortality a mortalidade lancada, ou {@code null}
     */
    public static ReportCage restore(
            CageId cageId,
            String battery,
            int number,
            int birdCount,
            ProductionEntry production,
            MortalityEntry mortality) {
        return new ReportCage(cageId, battery, number, birdCount, production, mortality);
    }

    public CageId cageId() {
        return cageId;
    }

    public String battery() {
        return battery;
    }

    public int number() {
        return number;
    }

    /** As aves da gaiola na abertura do relatorio. */
    public int birdCount() {
        return birdCount;
    }

    /** A producao lancada, ou nenhuma. */
    public Optional<ProductionEntry> production() {
        return Optional.ofNullable(production);
    }

    /** Se a producao da gaiola foi lancada; zero ovos e lancamento. */
    public boolean hasProduction() {
        return production != null;
    }

    /** Grava ou troca a producao; quem checa as invariantes e o relatorio. */
    void recordProduction(ProductionEntry entry) {
        this.production = Objects.requireNonNull(entry, "entry");
    }

    /** A mortalidade lancada, ou nenhuma. */
    public Optional<MortalityEntry> mortality() {
        return Optional.ofNullable(mortality);
    }

    /** Se a gaiola tem morte ou descarte lancado; zero e zero e lancamento sem ocorrencia. */
    public boolean hasOccurrence() {
        return mortality != null && mortality.hasOccurrence();
    }

    /** As aves que sairam da gaiola no dia: mortes e descartes, ou nenhuma sem lancamento. */
    int removals() {
        return mortality == null ? 0 : mortality.removals();
    }

    /** Grava ou troca a mortalidade; quem checa as invariantes e o relatorio. */
    void recordMortality(MortalityEntry entry) {
        this.mortality = Objects.requireNonNull(entry, "entry");
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ReportCage cage && cageId.equals(cage.cageId);
    }

    @Override
    public int hashCode() {
        return cageId.hashCode();
    }
}
