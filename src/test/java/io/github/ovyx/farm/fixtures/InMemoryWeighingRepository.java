package io.github.ovyx.farm.fixtures;

import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.Weighing;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.farm.domain.port.WeighingRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** As pesagens em memória: o repositório e o quadro das datas, como o banco responderia. */
public final class InMemoryWeighingRepository implements WeighingRepository {

    private final Map<WeighingId, Weighing> stored = new LinkedHashMap<>();
    private int saves;

    @Override
    public void save(Weighing weighing) {
        stored.put(weighing.id(), weighing);
        saves++;
    }

    @Override
    public Optional<Weighing> findById(WeighingId id) {
        return Optional.ofNullable(stored.get(id));
    }

    @Override
    public boolean isDayTaken(CageId cageId, LocalDate day, WeighingId exceptId) {
        return stored.values().stream()
                .filter(weighing -> !weighing.id().equals(exceptId))
                .filter(Weighing::isValid)
                .anyMatch(weighing -> weighing.cageId().equals(cageId)
                        && weighing.weighedOn().value().equals(day));
    }

    /** As pesagens gravadas, na ordem em que foram gravadas pela primeira vez. */
    public List<Weighing> all() {
        return List.copyOf(stored.values());
    }

    /** Quantas gravações houve: é como o teste prova que uma recusa não gravou nada. */
    public int saves() {
        return saves;
    }
}
