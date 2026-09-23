package io.github.ovyx.identity.domain;

import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.domain.valueobject.PasswordHash;

/**
 * Dublê de {@link PasswordHasher} para os testes de dominio.
 *
 * <p>E um dublê fiel, nao um mock: aplica uma transformacao reversivel e verificavel, entao
 * {@code matches} responde de verdade em vez de devolver o que o teste programou. Isso evita o
 * teste que passa porque o mock foi ensinado a concordar.
 *
 * <p>Nao e criptografia: serve so para os testes de dominio, onde o algoritmo real de hash e
 * irrelevante e custaria tempo de suite. O Argon2 de verdade e exercitado em
 * {@code Argon2PasswordHasherTest}.
 */
public final class FakePasswordHasher implements PasswordHasher {

    private static final String PREFIX = "{fake}";

    @Override
    public PasswordHash hash(String rawPassword) {
        return PasswordHash.of(PREFIX + new StringBuilder(rawPassword).reverse());
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
        if (rawPassword == null || hash == null) {
            return false;
        }
        return hash.value().equals(PREFIX + new StringBuilder(rawPassword).reverse());
    }
}
