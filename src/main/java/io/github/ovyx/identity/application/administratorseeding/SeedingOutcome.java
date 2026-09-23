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

    /** Ja havia administrador ativo; nada foi criado. */
    NOT_NEEDED
}
