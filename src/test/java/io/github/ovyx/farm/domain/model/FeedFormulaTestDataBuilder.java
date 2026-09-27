package io.github.ovyx.farm.domain.model;

import io.github.ovyx.farm.domain.port.FeedFormulaRoster;
import io.github.ovyx.farm.domain.valueobject.ExpectedIntake;
import io.github.ovyx.farm.domain.valueobject.FeedFormulaDescription;
import io.github.ovyx.farm.domain.valueobject.FeedFormulaName;
import io.github.ovyx.farm.domain.valueobject.PricePerKg;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.Clock;
import java.util.UUID;

/**
 * Monta fórmulas para os testes, com os dados da Postura Plus por padrão (spec da 004, exemplos).
 *
 * <p>A fórmula ativa nasce pela fábrica de cadastro, com as regras de verdade. A inativa é reconstruída
 * como se viesse do banco, sem depender da operação que a inativa.
 */
public final class FeedFormulaTestDataBuilder {

    /** Nenhuma outra fórmula: o nome nunca está em uso. */
    private static final FeedFormulaRoster EMPTY_ROSTER = (name, exceptId) -> false;

    private String name = "Postura Plus";
    private String pricePerKg = "2,85";
    private String expectedIntake = "28";
    private String description = "Milho, farelo de soja, calcário e premix vitamínico; para codornas em postura";
    private boolean inactive;
    private FeedFormulaRoster roster = EMPTY_ROSTER;
    private Clock clock = FixedClock.at("2026-09-20T10:15:00Z");

    private FeedFormulaTestDataBuilder() {}

    public static FeedFormulaTestDataBuilder aFormula() {
        return new FeedFormulaTestDataBuilder();
    }

    /** Uma fórmula com nome próprio, para testes que dividem o banco com outros. */
    public static FeedFormulaTestDataBuilder aUniqueFormula() {
        return aFormula().named("Fórmula " + UUID.randomUUID().toString().substring(0, 8));
    }

    public FeedFormulaTestDataBuilder named(String name) {
        this.name = name;
        return this;
    }

    public FeedFormulaTestDataBuilder pricedAt(String pricePerKg) {
        this.pricePerKg = pricePerKg;
        return this;
    }

    public FeedFormulaTestDataBuilder expecting(String expectedIntake) {
        this.expectedIntake = expectedIntake;
        return this;
    }

    public FeedFormulaTestDataBuilder describedAs(String description) {
        this.description = description;
        return this;
    }

    public FeedFormulaTestDataBuilder inactive() {
        this.inactive = true;
        return this;
    }

    public FeedFormulaTestDataBuilder withRoster(FeedFormulaRoster roster) {
        this.roster = roster;
        return this;
    }

    public FeedFormulaTestDataBuilder withClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public FeedFormula build() {
        if (!inactive) {
            return FeedFormula.register(name, pricePerKg, expectedIntake, description, roster, clock);
        }
        return FeedFormula.restore(
                FeedFormulaId.generate(),
                FeedFormulaName.of(name),
                PricePerKg.of(pricePerKg),
                ExpectedIntake.of(expectedIntake),
                FeedFormulaDescription.optionalOf(description).orElse(null),
                Status.INACTIVE,
                clock.instant(),
                clock.instant());
    }
}
