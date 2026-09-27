package io.github.ovyx.farm.application.formula;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dublê do diretório de fórmulas: responde o que o teste preparou e anota o que lhe perguntaram.
 *
 * <p>O teste do tratador de consulta prova o que o tratador pede ao diretório, e não o que o SQL
 * devolve: isso fica para o {@code FeedFormulaDirectoryIT}.
 */
final class RecordingFeedFormulaDirectory implements FeedFormulaDirectory {

    private final List<FeedFormulaSummary> listed = new ArrayList<>();
    private final Map<FeedFormulaId, FeedFormulaSummary> held = new HashMap<>();
    private final List<StatusFilter> askedFilters = new ArrayList<>();
    private final List<FeedFormulaId> askedIds = new ArrayList<>();

    RecordingFeedFormulaDirectory listing(FeedFormulaSummary summary) {
        listed.add(summary);
        return this;
    }

    RecordingFeedFormulaDirectory holding(FeedFormulaSummary summary) {
        held.put(summary.id(), summary);
        return this;
    }

    @Override
    public List<FeedFormulaSummary> list(StatusFilter status) {
        askedFilters.add(status);
        return List.copyOf(listed);
    }

    @Override
    public Optional<FeedFormulaSummary> findById(FeedFormulaId id) {
        askedIds.add(id);
        return Optional.ofNullable(held.get(id));
    }

    List<StatusFilter> askedFilters() {
        return List.copyOf(askedFilters);
    }

    List<FeedFormulaId> askedIds() {
        return List.copyOf(askedIds);
    }
}
