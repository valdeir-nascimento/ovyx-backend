package io.github.ovyx.farm.domain.model;

import java.util.Optional;
import java.util.UUID;

/**
 * Identidade da pesagem (feature 005).
 *
 * <p>UUID gerado pela aplicacao, e nao sequencia do banco: a identidade nasce com o agregado, antes de
 * qualquer ida ao banco.
 */
public record WeighingId(UUID value) {

    public WeighingId {
        if (value == null) {
            throw new IllegalArgumentException("identidade da pesagem é obrigatória");
        }
    }

    public static WeighingId generate() {
        return new WeighingId(UUID.randomUUID());
    }

    public static WeighingId of(UUID value) {
        return new WeighingId(value);
    }

    /**
     * A pesagem que o endereco aponta, ou nenhum.
     *
     * <p>O identificador malformado vira "nenhum", e nao excecao: quem pede uma pesagem que nao existe e
     * quem digita um endereco torto recebem a mesma resposta, "pesagem nao encontrada".
     */
    public static Optional<WeighingId> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            UUID value = UUID.fromString(raw.strip());
            // O fromString aceita formas abreviadas ("1-2-3-4-5"); so a forma canonica aponta uma pesagem.
            return value.toString().equalsIgnoreCase(raw.strip()) ? Optional.of(new WeighingId(value)) : Optional.empty();
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
