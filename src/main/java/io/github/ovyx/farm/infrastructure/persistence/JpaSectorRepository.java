package io.github.ovyx.farm.infrastructure.persistence;

import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.farm.domain.valueobject.SectorName;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador da porta {@code SectorRepository} sobre JPA. */
@Repository
public class JpaSectorRepository implements SectorRepository {

    private final SectorJpaRepository jpaRepository;
    private final EntityManager entityManager;

    JpaSectorRepository(SectorJpaRepository jpaRepository, EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void save(Sector sector) {
        // Atualiza a linha existente em vez de substitui-la, para preservar created_at e conferir a
        // versao da linha carregada.
        jpaRepository
                .findById(sector.id().value())
                .ifPresentOrElse(
                        existing -> {
                            // A versao e do setor inteiro, gaiolas inclusive (R-003). Mudar so uma gaiola
                            // nao muda a linha do setor, e a versao nao subiria sozinha.
                            if (!SectorRecordMapper.applyTo(existing, sector)) {
                                entityManager.lock(existing, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                            }
                        },
                        () -> jpaRepository.save(SectorRecordMapper.toRecord(sector)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sector> findById(SectorId id) {
        return jpaRepository.findById(id.value()).map(SectorRecordMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean anotherActiveSectorNamed(SectorName name, SectorId exceptId) {
        return jpaRepository.existsActiveNamed(name.value(), exceptId.value());
    }
}
