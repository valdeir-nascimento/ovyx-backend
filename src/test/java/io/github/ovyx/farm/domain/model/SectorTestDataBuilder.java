package io.github.ovyx.farm.domain.model;

import io.github.ovyx.farm.domain.port.SectorRoster;
import io.github.ovyx.farm.domain.valueobject.Battery;
import io.github.ovyx.farm.domain.valueobject.BirdCount;
import io.github.ovyx.farm.domain.valueobject.CageNumber;
import io.github.ovyx.farm.domain.valueobject.SectorDescription;
import io.github.ovyx.farm.domain.valueobject.SectorName;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Monta setores para os testes, com dados de granja válidos por padrão.
 *
 * <p>O setor ativo sem gaiolas nasce pela fábrica de cadastro, com as regras de verdade. Com gaiolas,
 * ou inativo, ele é reconstruído como se viesse do banco: é o único jeito de ter uma gaiola inativa, ou
 * um setor inativo, sem depender das operações que os inativam.
 */
public final class SectorTestDataBuilder {

    /** Ninguém no quadro: o nome nunca está em uso. */
    private static final SectorRoster EMPTY_ROSTER = (name, exceptId) -> false;

    /** Uma gaiola a reconstruir com o setor. */
    private record CageSpec(String battery, int number, int birdCount, Status status, boolean deactivatedWithSector) {}

    private String name = "Codornas — Galpão 1";
    private String description = "Codornas japonesas em postura, baterias A a D";
    private boolean inactive;
    private final List<CageSpec> cages = new ArrayList<>();
    private SectorRoster roster = EMPTY_ROSTER;
    private Clock clock = FixedClock.at("2026-09-20T10:15:00Z");

    private SectorTestDataBuilder() {}

    public static SectorTestDataBuilder aSector() {
        return new SectorTestDataBuilder();
    }

    /** Um setor com nome próprio, para testes que dividem o banco com outros. */
    public static SectorTestDataBuilder aUniqueSector() {
        return aSector().named("Galpão " + UUID.randomUUID().toString().substring(0, 8));
    }

    public SectorTestDataBuilder named(String name) {
        this.name = name;
        return this;
    }

    public SectorTestDataBuilder describedAs(String description) {
        this.description = description;
        return this;
    }

    public SectorTestDataBuilder inactive() {
        this.inactive = true;
        return this;
    }

    /** Uma gaiola ativa. */
    public SectorTestDataBuilder withCage(String battery, int number, int birdCount) {
        cages.add(new CageSpec(battery, number, birdCount, Status.ACTIVE, false));
        return this;
    }

    /** Uma gaiola inativada sozinha, antes: a reativação do setor não a traz de volta. */
    public SectorTestDataBuilder withInactiveCage(String battery, int number, int birdCount) {
        cages.add(new CageSpec(battery, number, birdCount, Status.INACTIVE, false));
        return this;
    }

    public SectorTestDataBuilder withRoster(SectorRoster roster) {
        this.roster = roster;
        return this;
    }

    public SectorTestDataBuilder withClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public Sector build() {
        if (!inactive && cages.isEmpty()) {
            return Sector.register(name, description, roster, clock);
        }
        return Sector.restore(
                SectorId.generate(),
                SectorName.of(name),
                SectorDescription.optionalOf(description).orElse(null),
                inactive ? Status.INACTIVE : Status.ACTIVE,
                cages.stream().map(this::restored).toList(),
                clock.instant(),
                clock.instant());
    }

    private Cage restored(CageSpec spec) {
        return Cage.restore(
                CageId.generate(),
                Battery.of(spec.battery()),
                CageNumber.of(String.valueOf(spec.number())),
                BirdCount.of(String.valueOf(spec.birdCount())),
                spec.status(),
                spec.deactivatedWithSector(),
                clock.instant(),
                clock.instant());
    }
}
