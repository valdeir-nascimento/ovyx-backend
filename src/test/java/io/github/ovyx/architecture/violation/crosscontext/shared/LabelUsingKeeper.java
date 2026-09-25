package io.github.ovyx.architecture.violation.crosscontext.shared;

import io.github.ovyx.architecture.violation.crosscontext.identity.Keeper;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Classe do nucleo compartilhado que depende de um contexto: por ela, um contexto alcancaria o outro
 * passando pelo {@code shared}. Existe apenas para o autoteste da suite de arquitetura.
 */
public class LabelUsingKeeper {

    public Label keeperLabel() {
        return new Label(new Keeper().name());
    }
}
