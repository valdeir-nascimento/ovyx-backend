package io.github.ovyx.production.domain.model;

import java.util.Optional;
import java.util.UUID;

/**
 * O setor do relatorio, pelo identificador que o farm publica (R-004).
 *
 * <p>Tipo do proprio production, e nao o {@code SectorId} do farm: os contextos nao dependem um do outro
 * (R-008 da 002), e o que eles dividem e o valor do identificador.
 */
public record SectorId(UUID value) {

    public SectorId {
        if (value == null) {
            throw new IllegalArgumentException("identidade do setor é obrigatória");
        }
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
