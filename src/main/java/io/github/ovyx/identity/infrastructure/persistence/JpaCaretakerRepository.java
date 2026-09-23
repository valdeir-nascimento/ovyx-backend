package io.github.ovyx.identity.infrastructure.persistence;

import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador da porta {@code CaretakerRepository} sobre JPA. */
@Repository
public class JpaCaretakerRepository implements CaretakerRepository {

    private final CaretakerJpaRepository jpaRepository;

    JpaCaretakerRepository(CaretakerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void save(Caretaker caretaker) {
        // Atualiza a linha existente em vez de substitui-la, para preservar created_at.
        jpaRepository
                .findById(caretaker.id().value())
                .ifPresentOrElse(
                        existing -> CaretakerRecordMapper.applyTo(existing, caretaker),
                        () -> jpaRepository.save(CaretakerRecordMapper.toRecord(caretaker)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Caretaker> findById(CaretakerId id) {
        return jpaRepository.findById(id.value()).map(CaretakerRecordMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Caretaker> findByEmailOrMobilePhone(String canonicalIdentifier) {
        // Igualdade exata, sem normalizacao propria. A versao anterior descartava todo caractere
        // nao numerico quando nao achava nada, e com isso "x11999999999" entrava na conta do
        // administrador com uma chave de contencao diferente da conta real.
        if (canonicalIdentifier == null || canonicalIdentifier.isEmpty()) {
            return Optional.empty();
        }
        return jpaRepository.findByEmailOrMobilePhone(canonicalIdentifier).stream()
                .findFirst()
                .map(CaretakerRecordMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveAdministrators() {
        return jpaRepository.countByRoleAndStatus(Role.ADMINISTRATOR, CaretakerStatus.ACTIVE);
    }
}
