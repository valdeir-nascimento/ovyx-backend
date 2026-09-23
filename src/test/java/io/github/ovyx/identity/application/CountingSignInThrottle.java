package io.github.ovyx.identity.application;

import io.github.ovyx.identity.domain.port.SignInThrottle;
import java.util.HashMap;
import java.util.Map;

/**
 * Dublê fiel da contencao de tentativas: conta falhas de verdade e bloqueia ao atingir o limite.
 *
 * <p>O comportamento com janela de tempo e persistencia e verificado contra o PostgreSQL real em
 * {@code SignInThrottleIT}; aqui interessa apenas a decisao do caso de uso diante de um bloqueio.
 */
public final class CountingSignInThrottle implements SignInThrottle {

    private final int maxFailures;
    private final Map<String, Integer> failures = new HashMap<>();

    public CountingSignInThrottle(int maxFailures) {
        this.maxFailures = maxFailures;
    }

    private static String key(String identifier, String origin) {
        return identifier + '|' + origin;
    }

    @Override
    public boolean isBlocked(String attemptedIdentifier, String origin) {
        return failures.getOrDefault(key(attemptedIdentifier, origin), 0) >= maxFailures;
    }

    @Override
    public void registerFailure(String attemptedIdentifier, String origin) {
        failures.merge(key(attemptedIdentifier, origin), 1, Integer::sum);
    }

    @Override
    public void clear(String attemptedIdentifier, String origin) {
        failures.remove(key(attemptedIdentifier, origin));
    }

    public int failureCount(String attemptedIdentifier, String origin) {
        return failures.getOrDefault(key(attemptedIdentifier, origin), 0);
    }
}
