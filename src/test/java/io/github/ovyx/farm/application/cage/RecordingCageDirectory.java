package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.PageResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Dublê do diretório de gaiolas: responde o que o teste preparou e anota o que lhe perguntaram.
 *
 * <p>O teste do tratador de consulta prova o que o tratador pede ao diretório; o que o SQL devolve
 * fica para o {@code CageDirectoryIT}.
 */
final class RecordingCageDirectory implements CageDirectory {

    /** Uma pesquisa, como o tratador a pediu. */
    record Search(SectorId sectorId, String code, String battery, StatusFilter status, int page, int size) {}

    private final Set<SectorId> sectors = new HashSet<>();
    private final Map<CageId, CageDetail> details = new HashMap<>();
    private final List<Search> searches = new ArrayList<>();

    RecordingCageDirectory withSector(SectorId sectorId) {
        sectors.add(sectorId);
        return this;
    }

    RecordingCageDirectory holding(CageDetail detail) {
        sectors.add(detail.sectorId());
        details.put(detail.id(), detail);
        return this;
    }

    @Override
    public boolean sectorExists(SectorId sectorId) {
        return sectors.contains(sectorId);
    }

    @Override
    public PageResponse<CageSummary> search(
            SectorId sectorId, String code, String battery, StatusFilter status, int page, int size) {
        searches.add(new Search(sectorId, code, battery, status, page, size));
        return PageResponse.of(List.of(), page, size, 0);
    }

    @Override
    public Optional<CageDetail> findDetail(SectorId sectorId, CageId cageId) {
        return Optional.ofNullable(details.get(cageId)).filter(detail -> detail.sectorId().equals(sectorId));
    }

    List<Search> searches() {
        return List.copyOf(searches);
    }
}
