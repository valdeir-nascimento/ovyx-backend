package io.github.ovyx.architecture.violation.crosscontext.farm;

import io.github.ovyx.architecture.violation.crosscontext.identity.Keeper;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Classe de um contexto que depende de uma classe de outro contexto, sem passar pelo
 * {@code shared}. Existe apenas para o autoteste da suite de arquitetura.
 */
public class HouseUsingKeeper {

    private final Keeper keeper = new Keeper();

    public String keeperName() {
        return keeper.name();
    }
}
