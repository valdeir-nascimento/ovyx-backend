package io.github.ovyx.architecture.violation.crosscontext.farm;

import io.github.ovyx.architecture.violation.crosscontext.shared.Label;

/** Uso permitido: um contexto depende do nucleo compartilhado, e de nenhum outro contexto. */
public class HouseUsingShared {

    public Label label() {
        return new Label("Galpao 1");
    }
}
