package io.github.ovyx.architecture.violation.presentation;

import io.github.ovyx.architecture.violation.infrastructure.SomeAdapter;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Controller que chama um adaptador de infraestrutura diretamente, pulando os casos de uso.
 * Existe apenas para o autoteste da suite de arquitetura.
 */
public class SomeController {

    private final SomeAdapter adapter = new SomeAdapter();

    public String handle() {
        return adapter.load();
    }
}
