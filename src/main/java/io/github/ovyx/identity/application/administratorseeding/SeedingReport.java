package io.github.ovyx.identity.application.administratorseeding;

import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.identity.domain.model.Role;

/**
 * O que a semeadura fez, com o que o operador precisa saber para entrar.
 *
 * <p>Na restauracao, o responsavel mantem o e-mail que ja tinha, que pode nao ser o configurado:
 * sem ele no aviso, quem seguia o guia com o e-mail configurado recebia a recusa generica.
 *
 * @param outcome        o que foi feito
 * @param email          e-mail com que o administrador entra; nulo quando nada foi preciso
 * @param previousRole   perfil de antes da restauracao; nulo fora dela
 * @param previousStatus situacao de antes da restauracao; nula fora dela
 */
public record SeedingReport(SeedingOutcome outcome, String email, Role previousRole, CaretakerStatus previousStatus) {

    static SeedingReport notNeeded() {
        return new SeedingReport(SeedingOutcome.NOT_NEEDED, null, null, null);
    }

    static SeedingReport created(String email) {
        return new SeedingReport(SeedingOutcome.CREATED, email, null, null);
    }

    static SeedingReport restored(String email, Role previousRole, CaretakerStatus previousStatus) {
        return new SeedingReport(SeedingOutcome.RESTORED, email, previousRole, previousStatus);
    }
}
