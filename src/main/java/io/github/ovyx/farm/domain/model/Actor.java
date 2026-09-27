package io.github.ovyx.farm.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Quem fez uma operacao na pesagem, como estava na sessao naquele momento (R-004 da 005).
 *
 * <p>O nome fica gravado junto do identificador: o historico continua legivel sem consultar o identity, e um
 * responsavel renomeado nao reescreve quem pesou. O farm tem o seu, como o production: cada contexto tem o
 * proprio modelo, e nenhum importa o outro.
 *
 * @param id o responsavel
 * @param name o nome, como estava na sessao
 */
public record Actor(UUID id, String name) {

    public Actor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
