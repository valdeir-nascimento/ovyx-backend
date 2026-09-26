package io.github.ovyx.production.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Setor da granja como a porta {@code FarmStructure} o entrega ao production, para os testes: por padrão,
 * ativo, com as gaiolas A-01 (48 aves) e B-07 (50 aves).
 */
public final class FarmSectorTestDataBuilder {

    public static final CageId A01 = CageId.of(UUID.fromString("2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66"));
    public static final CageId B07 = CageId.of(UUID.fromString("9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44"));

    private SectorId id = SectorId.of(UUID.randomUUID());
    private String name = "Codornas — Galpão 4";
    private boolean active = true;
    private List<FarmCage> cages = new ArrayList<>(List.of(
            new FarmCage(A01, "A", 1, 48), new FarmCage(B07, "B", 7, 50)));

    private FarmSectorTestDataBuilder() {}

    public static FarmSectorTestDataBuilder aFarmSector() {
        return new FarmSectorTestDataBuilder();
    }

    public FarmSectorTestDataBuilder withId(SectorId id) {
        this.id = id;
        return this;
    }

    public FarmSectorTestDataBuilder named(String name) {
        this.name = name;
        return this;
    }

    public FarmSectorTestDataBuilder inactive() {
        this.active = false;
        this.cages = new ArrayList<>();
        return this;
    }

    public FarmSectorTestDataBuilder withCages(FarmCage... cages) {
        this.cages = new ArrayList<>(List.of(cages));
        return this;
    }

    public FarmSectorTestDataBuilder withoutCages() {
        this.cages = new ArrayList<>();
        return this;
    }

    public FarmSector build() {
        return new FarmSector(id, name, active, cages);
    }
}
