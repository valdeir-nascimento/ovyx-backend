package io.github.ovyx.architecture.violation.infrastructure;

/**
 * VIOLACAO DELIBERADA — parte do cenario de autoteste da suite de arquitetura.
 *
 * <p>Adaptador de infraestrutura usado por
 * {@link io.github.ovyx.architecture.violation.domain.DomainDependingOnInfrastructure} para criar
 * uma dependencia proibida do dominio para a infraestrutura.
 */
public class SomeAdapter {

    public String load() {
        return "dado vindo da infraestrutura";
    }
}
