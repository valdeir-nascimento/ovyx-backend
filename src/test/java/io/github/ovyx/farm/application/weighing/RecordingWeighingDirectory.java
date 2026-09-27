package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.WeighingId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** O dublê da porta de leitura das pesagens: responde o que o teste lhe ensinou. */
final class RecordingWeighingDirectory implements WeighingDirectory {

    private final Map<UUID, WeighedSector> sectors = new HashMap<>();
    private final Map<UUID, Map<UUID, WeighedCage>> cages = new HashMap<>();
    private final Map<UUID, List<WeighingEntry>> weighings = new HashMap<>();
    private final Map<UUID, Map<WeighingId, WeighingDetail>> details = new HashMap<>();

    void knowSector(WeighedSector sector) {
        sectors.put(sector.id().value(), sector);
    }

    void knowCage(UUID sectorId, WeighedCage cage) {
        cages.computeIfAbsent(sectorId, key -> new HashMap<>()).put(cage.id().value(), cage);
    }

    void knowWeighings(UUID cageId, List<WeighingEntry> entries) {
        weighings.put(cageId, entries);
    }

    void knowWeighing(UUID cageId, WeighingDetail detail) {
        details.computeIfAbsent(cageId, key -> new HashMap<>()).put(detail.id(), detail);
    }

    @Override
    public Optional<WeighedSector> sectorOf(SectorId sectorId) {
        return Optional.ofNullable(sectors.get(sectorId.value()));
    }

    @Override
    public Optional<WeighedCage> cageOf(SectorId sectorId, CageId cageId) {
        return Optional.ofNullable(cages.getOrDefault(sectorId.value(), Map.of()).get(cageId.value()));
    }

    @Override
    public List<WeighingEntry> weighingsOf(CageId cageId) {
        return weighings.getOrDefault(cageId.value(), List.of());
    }

    @Override
    public Optional<WeighingDetail> findWeighing(CageId cageId, WeighingId weighingId) {
        return Optional.ofNullable(details.getOrDefault(cageId.value(), Map.of()).get(weighingId));
    }
}
