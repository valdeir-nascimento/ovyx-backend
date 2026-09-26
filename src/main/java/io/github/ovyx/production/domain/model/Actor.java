package io.github.ovyx.production.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Quem fez uma operacao no relatorio, como estava na sessao naquele momento (R-007).
 *
 * <p>O nome fica gravado junto do identificador: o historico continua legivel sem consultar o identity,
 * e um responsavel renomeado nao reescreve quem lancou.
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
