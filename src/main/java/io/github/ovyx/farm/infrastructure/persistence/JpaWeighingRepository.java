package io.github.ovyx.farm.infrastructure.persistence;

import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Weighing;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.farm.domain.port.WeighingRepository;
import io.github.ovyx.farm.domain.valueobject.AverageWeight;
import io.github.ovyx.farm.domain.valueobject.WeighingDate;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** O repositorio da pesagem sobre a tabela {@code weighing} (R-003 e R-005 da 005). */
@Repository
public class JpaWeighingRepository implements WeighingRepository {

    private final WeighingJpaRepository jpaRepository;

    JpaWeighingRepository(WeighingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void save(Weighing weighing) {
        // Atualiza a linha existente em vez de substitui-la, para conferir a versao da linha carregada.
        WeighingRecord record = jpaRepository
                .findById(weighing.id().value())
                .orElseGet(() -> new WeighingRecord(
                        weighing.id().value(),
                        weighing.sectorId().value(),
                        weighing.cageId().value(),
                        weighing.recordedBy().id(),
                        weighing.recordedBy().name(),
                        weighing.recordedAt()));
        record.apply(
                weighing.weighedOn().value(),
                weighing.averageWeight().value(),
                weighing.status(),
                weighing.lastCorrectedBy().map(Actor::id).orElse(null),
                weighing.lastCorrectedBy().map(Actor::name).orElse(null),
                weighing.lastCorrectedAt().orElse(null),
                weighing.voidedBy().map(Actor::id).orElse(null),
                weighing.voidedBy().map(Actor::name).orElse(null),
                weighing.voidedAt().orElse(null));
        jpaRepository.save(record);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Weighing> findById(WeighingId id) {
        return jpaRepository.findById(id.value()).map(JpaWeighingRepository::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDayTaken(CageId cageId, LocalDate day, WeighingId exceptId) {
        return jpaRepository.existsValidOn(cageId.value(), day, exceptId.value());
    }

    private static Weighing toDomain(WeighingRecord record) {
        return Weighing.restore(
                WeighingId.of(record.getId()),
                SectorId.of(record.getSectorId()),
                CageId.of(record.getCageId()),
                new WeighingDate(record.getWeighedOn()),
                new AverageWeight(record.getAverageWeight()),
                record.getStatus(),
                new Actor(record.getRecordedById(), record.getRecordedByName()),
                record.getRecordedAt(),
                actorOf(record.getLastCorrectedById(), record.getLastCorrectedByName()),
                record.getLastCorrectedAt(),
                actorOf(record.getVoidedById(), record.getVoidedByName()),
                record.getVoidedAt());
    }

    private static Actor actorOf(UUID id, String name) {
        return id == null ? null : new Actor(id, name);
    }
}
