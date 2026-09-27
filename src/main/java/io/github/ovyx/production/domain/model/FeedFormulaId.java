package io.github.ovyx.production.domain.model;

import java.util.Optional;
import java.util.UUID;

/**
 * A formula de racao de um lancamento, pelo identificador que o farm publica (R-004 da 004). Tipo do
 * proprio production, como o {@link CageId}.
 */
public record FeedFormulaId(UUID value) {

    public FeedFormulaId {
        if (value == null) {
            throw new IllegalArgumentException("identidade da fórmula é obrigatória");
        }
    }

    public static FeedFormulaId of(UUID value) {
        return new FeedFormulaId(value);
    }

    /** A formula que o texto aponta, ou nenhuma: o malformado recebe a mesma resposta do inexistente. */
    public static Optional<FeedFormulaId> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            UUID value = UUID.fromString(raw.strip());
            return value.toString().equalsIgnoreCase(raw.strip())
                    ? Optional.of(new FeedFormulaId(value))
                    : Optional.empty();
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
