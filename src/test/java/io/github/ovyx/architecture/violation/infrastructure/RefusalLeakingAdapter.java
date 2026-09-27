package io.github.ovyx.architecture.violation.infrastructure;

import io.github.ovyx.architecture.sample.domain.Registration;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Adaptador que chama a fabrica do dominio, que pode recusar. A recusa subiria por quem chamou o
 * adaptador sem passar por nenhum caso de uso. Reidratar e com {@code restore}, que nao valida.
 *
 * <p>Existe apenas para {@link io.github.ovyx.architecture.ArchitectureRulesSelfCheckTest} comprovar
 * que a regra das camadas externas avalia tambem a infraestrutura.
 */
public class RefusalLeakingAdapter {

    public String restore(String raw) {
        return Registration.of(raw);
    }
}
