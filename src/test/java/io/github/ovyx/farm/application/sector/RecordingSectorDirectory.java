package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.domain.model.SectorId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dublê do diretório de setores: responde o que o teste preparou e anota o que lhe perguntaram.
 *
 * <p>O teste do tratador de consulta prova o que o tratador pede ao diretório, e não o que o SQL
 * devolve: isso fica para o {@code SectorDirectoryIT}.
 */
final class RecordingSectorDirectory implements SectorDirectory {

    private final List<SectorSummary> listed = new ArrayList<>();
    private final Map<SectorId, SectorDetail> details = new HashMap<>();
    private final List<StatusFilter> askedFilters = new ArrayList<>();
    private final List<SectorId> askedDetails = new ArrayList<>();

    RecordingSectorDirectory listing(SectorSummary summary) {
        listed.add(summary);
        return this;
    }

    RecordingSectorDirectory holding(SectorDetail detail) {
        details.put(detail.id(), detail);
        return this;
    }

    @Override
    public List<SectorSummary> list(StatusFilter status) {
        askedFilters.add(status);
        return List.copyOf(listed);
    }

    @Override
    public Optional<SectorDetail> findDetail(SectorId id) {
        askedDetails.add(id);
        return Optional.ofNullable(details.get(id));
    }

    List<StatusFilter> askedFilters() {
        return List.copyOf(askedFilters);
    }

    List<SectorId> askedDetails() {
        return List.copyOf(askedDetails);
    }
}
