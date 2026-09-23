package io.github.ovyx.identity.application;

import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dublê em memoria do repositorio, para os testes de caso de uso.
 *
 * <p>Fiel ao adaptador real: busca por igualdade exata sobre o identificador canonico, sem
 * normalizar nada por conta propria, e com precedencia do ativo sobre o inativo. Um dublê mais
 * permissivo que o adaptador esconderia exatamente o tipo de divergencia que abriu a brecha de
 * contencao encontrada pelo code-reviewer.
 */
public final class InMemoryCaretakerRepository implements CaretakerRepository {

    private final Map<CaretakerId, Caretaker> stored = new LinkedHashMap<>();

    @Override
    public void save(Caretaker caretaker) {
        stored.put(caretaker.id(), caretaker);
    }

    @Override
    public Optional<Caretaker> findById(CaretakerId id) {
        return Optional.ofNullable(stored.get(id));
    }

    @Override
    public Optional<Caretaker> findByEmailOrMobilePhone(String canonicalIdentifier) {
        return stored.values().stream()
                .filter(caretaker -> caretaker.email().value().equals(canonicalIdentifier)
                        || caretaker.mobilePhone().value().equals(canonicalIdentifier))
                .min(Comparator.comparing(caretaker -> !caretaker.isActive()));
    }

    @Override
    public long countActiveAdministrators() {
        return stored.values().stream()
                .filter(Caretaker::isActive)
                .filter(caretaker -> caretaker.role() == Role.ADMINISTRATOR)
                .count();
    }
}
