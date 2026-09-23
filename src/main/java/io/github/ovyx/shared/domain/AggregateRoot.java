package io.github.ovyx.shared.domain;

import java.util.Objects;

/**
 * Raiz de agregado com identidade propria.
 *
 * <p>
 * A igualdade e por identificador, nunca por valor dos campos: dois carregamentos
 * do mesmo agregado sao o mesmo agregado, ainda que um deles esteja desatualizado.
 * </p>
 *
 * @param <I> tipo do identificador do agregado.
 */
public abstract class AggregateRoot<I> {

    private final I id;

    protected AggregateRoot(final I id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public I id() {
        return id;
    }

    @Override
    public final boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return id.equals(((AggregateRoot<?>) other).id);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }
}
