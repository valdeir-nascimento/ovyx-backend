package io.github.ovyx.farm.domain.model;

import java.util.Optional;
import java.util.UUID;

/**
 * Identidade do setor.
 *
 * <p>UUID gerado pela aplicacao, e nao sequencia do banco: a identidade nasce com o agregado, antes de
 * qualquer ida ao banco.
 */
public record SectorId(UUID value) {

    public SectorId {
        if (value == null) {
            throw new IllegalArgumentException("identidade do setor é obrigatória");
        }
    }

    public static SectorId generate() {
        return new SectorId(UUID.randomUUID());
    }

    public static SectorId of(UUID value) {
        return new SectorId(value);
    }

    /**
     * O setor que o endereco aponta, ou nenhum.
     *
     * <p>O identificador malformado vira "nenhum", e nao excecao: quem pede um setor que nao existe e
     * quem digita um endereco torto recebem a mesma resposta, "setor nao encontrado".
     */
    public static Optional<SectorId> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            UUID value = UUID.fromString(raw.strip());
            // O fromString aceita formas abreviadas ("1-2-3-4-5"); so a forma canonica aponta um setor.
            return value.toString().equalsIgnoreCase(raw.strip()) ? Optional.of(new SectorId(value)) : Optional.empty();
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
