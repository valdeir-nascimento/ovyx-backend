package io.github.ovyx.identity.domain.model;

import java.util.UUID;

/**
 * Identidade do responsavel.
 *
 * <p>UUID gerado pela aplicacao, e nao sequencia do banco: a identidade nasce com o agregado, antes
 * de qualquer ida ao banco, o que mantem o dominio independente da persistencia.
 */
public record CaretakerId(UUID value) {

    public CaretakerId {
        if (value == null) {
            throw new IllegalArgumentException("identidade do responsável é obrigatória");
        }
    }

    public static CaretakerId generate() {
        return new CaretakerId(UUID.randomUUID());
    }

    public static CaretakerId of(UUID value) {
        return new CaretakerId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
