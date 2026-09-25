package io.github.ovyx.architecture.violation.crosscontext.identity;

/** Classe de um contexto qualquer, alvo da dependencia proibida de {@code HouseUsingKeeper}. */
public class Keeper {

    public String name() {
        return "Maria";
    }
}
