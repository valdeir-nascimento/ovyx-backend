package io.github.ovyx.architecture.violation.domain;

import io.github.ovyx.architecture.violation.infrastructure.SomeAdapter;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Classe de dominio que depende de uma classe de infraestrutura, invertendo a regra de
 * dependencia do principio I. Existe apenas para o autoteste da suite de arquitetura.
 */
public class DomainDependingOnInfrastructure {

    private final SomeAdapter adapter = new SomeAdapter();

    public String describe() {
        return adapter.load();
    }
}
