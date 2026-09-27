package io.github.ovyx.production.fixtures;

import io.github.ovyx.production.domain.model.FarmSector;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.production.domain.port.FarmStructure;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Dublê da estrutura da granja: os setores que o teste monta. */
public final class InMemoryFarmStructure implements FarmStructure {

    private final Map<SectorId, FarmSector> sectors = new LinkedHashMap<>();

    public FarmSector put(FarmSector sector) {
        sectors.put(sector.id(), sector);
        return sector;
    }

    @Override
    public Optional<FarmSector> sectorOf(SectorId sectorId) {
        return Optional.ofNullable(sectors.get(sectorId));
    }
}
