package io.github.ovyx.farm.domain.model;

import java.util.Optional;
import java.util.UUID;

/**
 * Identidade da formula de racao.
 *
 * <p>UUID gerado pela aplicacao, e nao sequencia do banco: a identidade nasce com o agregado, antes de
 * qualquer ida ao banco.
 */
public record FeedFormulaId(UUID value) {

    public FeedFormulaId {
        if (value == null) {
            throw new IllegalArgumentException("identidade da fórmula é obrigatória");
        }
    }

    public static FeedFormulaId generate() {
        return new FeedFormulaId(UUID.randomUUID());
    }

    public static FeedFormulaId of(UUID value) {
        return new FeedFormulaId(value);
    }

    /**
     * A formula que o endereco aponta, ou nenhum.
     *
     * <p>O identificador malformado vira "nenhum", e nao excecao: quem pede uma formula que nao existe e
     * quem digita um endereco torto recebem a mesma resposta, "formula nao encontrada".
     */
    public static Optional<FeedFormulaId> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            UUID value = UUID.fromString(raw.strip());
            // O fromString aceita formas abreviadas ("1-2-3-4-5"); so a forma canonica aponta uma formula.
            return value.toString().equalsIgnoreCase(raw.strip()) ? Optional.of(new FeedFormulaId(value)) : Optional.empty();
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
