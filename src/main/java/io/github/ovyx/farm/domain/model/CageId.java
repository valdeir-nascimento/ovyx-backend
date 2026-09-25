package io.github.ovyx.farm.domain.model;

import java.util.Optional;
import java.util.UUID;

/**
 * Identidade da gaiola: e o que as features seguintes usam para lancar producao, racao, mortalidade e
 * peso. UUID gerado pela aplicacao, imutavel.
 */
public record CageId(UUID value) {

    public CageId {
        if (value == null) {
            throw new IllegalArgumentException("identidade da gaiola é obrigatória");
        }
    }

    public static CageId generate() {
        return new CageId(UUID.randomUUID());
    }

    public static CageId of(UUID value) {
        return new CageId(value);
    }

    /** A gaiola que o endereco aponta, ou nenhuma: o malformado recebe a mesma resposta do inexistente. */
    public static Optional<CageId> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            UUID value = UUID.fromString(raw.strip());
            return value.toString().equalsIgnoreCase(raw.strip()) ? Optional.of(new CageId(value)) : Optional.empty();
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
