package io.github.ovyx.identity.fixtures;

import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.valueobject.Cpf;
import io.github.ovyx.identity.domain.valueobject.Email;
import io.github.ovyx.identity.domain.valueobject.MobilePhone;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Dublê em memoria do repositorio, para os testes do dominio e dos casos de uso.
 *
 * <p>Mora num pacote neutro de fixtures, e nao em {@code application}: os testes do agregado
 * tambem o usam como {@code CaretakerRoster}, e o dominio nao depende da aplicacao nem nos testes.
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
    public Optional<Caretaker> findByCpf(Cpf cpf) {
        return stored.values().stream().filter(caretaker -> caretaker.cpf().equals(cpf)).findFirst();
    }

    @Override
    public boolean isCpfTakenByAnother(Cpf cpf, CaretakerId self) {
        return others(self).anyMatch(caretaker -> caretaker.cpf().equals(cpf));
    }

    @Override
    public boolean isEmailTakenByAnotherActive(Email email, CaretakerId self) {
        return others(self).filter(Caretaker::isActive).anyMatch(caretaker -> caretaker.email().equals(email));
    }

    @Override
    public boolean isMobilePhoneTakenByAnotherActive(MobilePhone mobilePhone, CaretakerId self) {
        return others(self)
            .filter(Caretaker::isActive)
            .anyMatch(caretaker -> caretaker.mobilePhone().equals(mobilePhone));
    }

    /** Quantos responsaveis estao gravados, ativos ou nao: e como o teste prova que nada foi gravado. */
    public int size() {
        return stored.size();
    }

    private Stream<Caretaker> others(CaretakerId self) {
        return stored.values().stream().filter(caretaker -> !caretaker.id().equals(self));
    }

    @Override
    public long countActiveAdministrators() {
        return stored.values().stream()
            .filter(Caretaker::isActive)
            .filter(caretaker -> caretaker.role() == Role.ADMINISTRATOR)
            .count();
    }
}
