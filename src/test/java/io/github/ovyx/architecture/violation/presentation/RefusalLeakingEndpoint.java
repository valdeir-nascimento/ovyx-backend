package io.github.ovyx.architecture.violation.presentation;

import io.github.ovyx.architecture.sample.application.Registrar;
import io.github.ovyx.architecture.sample.domain.Registration;
import io.github.ovyx.shared.domain.DomainException;

/**
 * VIOLACAO DELIBERADA — nao imite este codigo.
 *
 * <p>Entrada que pula o caso de uso: chama direto o colaborador de aplicacao e o dominio, que deixam
 * a recusa subir. Ninguem a traduz em {@code Failure}; ela chegaria ao tratador global como 409.
 *
 * <p>Existe apenas para {@link io.github.ovyx.architecture.ArchitectureRulesSelfCheckTest} comprovar
 * que a suite reprova esta situacao.
 */
public class RefusalLeakingEndpoint {

    private final Registrar registrar = new Registrar();

    public String registerThroughTheCollaborator(String raw) {
        return registrar.register(raw);
    }

    public String registerThroughTheDomain(String raw) {
        return Registration.of(raw);
    }

    /** Capturar na borda nao conserta: quem traduz a recusa em {@code Failure} e o caso de uso. */
    public String registerCatchingTheRefusal(String raw) {
        try {
            return Registration.of(raw);
        } catch (DomainException refusal) {
            return "";
        }
    }
}
