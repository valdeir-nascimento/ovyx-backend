package io.github.ovyx.architecture.violation.web;

import io.github.ovyx.architecture.sample.application.Registrar;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Classe fora das quatro camadas que chama um colaborador de aplicacao que deixa a recusa subir.
 * Um pacote novo, fora de {@code presentation} e {@code infrastructure}, nao pode virar brecha.
 *
 * <p>Existe apenas para {@link io.github.ovyx.architecture.ArchitectureRulesSelfCheckTest} comprovar
 * que a regra das camadas externas vale para tudo o que nao e dominio nem aplicacao.
 */
public class RefusalLeakingFilter {

    private final Registrar registrar = new Registrar();

    public String filter(String raw) {
        return registrar.register(raw);
    }
}
