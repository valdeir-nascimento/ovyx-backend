package io.github.ovyx.identity.application.administratorseeding;

/**
 * O que a semeadura do administrador inicial fez.
 *
 * <p>Existe para que "nada a fazer" seja um sucesso com significado, e nao um {@code Success(null)},
 * que o principio IV proibe.
 */
public enum SeedingOutcome {
    /** O administrador inicial foi criado. */
    CREATED,

    /**
     * Nao havia administrador ativo, e quem ja tinha o CPF configurado voltou a administrar, com a
     * senha provisoria e a troca obrigatoria no primeiro acesso.
     */
    RESTORED,

    /** Ja havia administrador ativo; nada foi criado. */
    NOT_NEEDED
}
