package io.github.ovyx.architecture.violation.application;

import io.github.ovyx.architecture.violation.infrastructure.SomeAdapter;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Caso de uso que chama um adaptador de infraestrutura diretamente, em vez de uma porta. Existe
 * apenas para o autoteste da suite de arquitetura.
 */
public class ApplicationUsingInfrastructure {

    private final SomeAdapter adapter = new SomeAdapter();

    public String execute() {
        return adapter.load();
    }
}
