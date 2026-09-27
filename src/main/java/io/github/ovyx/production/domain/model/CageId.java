package io.github.ovyx.production.domain.model;

import java.util.Optional;
import java.util.UUID;

/**
 * A gaiola do relatorio, pelo identificador que o farm publica (R-004). Tipo do proprio production,
 * como o {@link SectorId}.
 */
public record CageId(UUID value) {

    public CageId {
        if (value == null) {
            throw new IllegalArgumentException("identidade da gaiola é obrigatória");
        }
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
