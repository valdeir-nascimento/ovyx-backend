package io.github.ovyx.farm.fixtures;

import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.farm.domain.valueobject.SectorName;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dublê em memória do repositório de setores, para os testes do domínio e dos casos de uso.
 *
 * <p>Mora num pacote neutro de fixtures, e não em {@code application}: os testes do agregado também o
 * usam como {@code SectorRoster}, e o domínio não depende da aplicação nem nos testes.
 *
 * <p>Fiel ao adaptador real: o nome em uso é o de outro setor ativo, comparado sem maiúsculas.
 */
public final class InMemorySectorRepository implements SectorRepository {

    private final Map<SectorId, Sector> stored = new LinkedHashMap<>();
    private int saves;

    @Override
    public void save(Sector sector) {
        stored.put(sector.id(), sector);
        saves++;
    }

    @Override
    public Optional<Sector> findById(SectorId id) {
        return Optional.ofNullable(stored.get(id));
    }

    @Override
    public boolean anotherActiveSectorNamed(SectorName name, SectorId exceptId) {
        return stored.values().stream()
                .filter(sector -> !sector.id().equals(exceptId))
                .filter(Sector::isActive)
                .anyMatch(sector -> sector.name().sameAs(name));
    }

    /** Quantas gravações houve: é como o teste prova que uma recusa não gravou nada. */
    public int saves() {
        return saves;
    }
}
