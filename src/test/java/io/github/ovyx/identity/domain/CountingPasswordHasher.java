package io.github.ovyx.identity.domain;

import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.domain.valueobject.PasswordHash;

/**
 * Dublê que delega ao {@link FakePasswordHasher} e conta as chamadas.
 *
 * <p>Existe para testar o canal de tempo sem medir tempo. Medicao de tempo em teste e intermitente
 * por natureza; contar verificacoes de hash e deterministico e prova a mesma coisa: se todo caminho
 * executa exatamente uma verificacao, todo caminho paga o mesmo custo de Argon2.
 */
public final class CountingPasswordHasher implements PasswordHasher {

    private final FakePasswordHasher delegate = new FakePasswordHasher();
    private int hashCalls;
    private int matchesCalls;

    @Override
    public PasswordHash hash(String rawPassword) {
        hashCalls++;
        return delegate.hash(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
        matchesCalls++;
        return delegate.matches(rawPassword, hash);
    }

    public int hashCalls() {
        return hashCalls;
    }

    public int matchesCalls() {
        return matchesCalls;
    }

    /** Zera a contagem, para medir apenas a operacao sob teste. */
    public void reset() {
        hashCalls = 0;
        matchesCalls = 0;
    }
}
