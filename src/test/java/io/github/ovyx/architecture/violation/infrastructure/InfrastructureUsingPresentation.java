package io.github.ovyx.architecture.violation.infrastructure;

import io.github.ovyx.architecture.violation.presentation.SomeController;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Adaptador de infraestrutura que depende da camada de entrega. Existe apenas para o autoteste da
 * suite de arquitetura.
 */
public class InfrastructureUsingPresentation {

    public String callBack(SomeController controller) {
        return controller.handle();
    }
}
