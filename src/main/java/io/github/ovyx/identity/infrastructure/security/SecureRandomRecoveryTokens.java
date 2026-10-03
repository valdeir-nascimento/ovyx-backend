package io.github.ovyx.identity.infrastructure.security;

import io.github.ovyx.identity.domain.port.RecoveryTokens;
import io.github.ovyx.identity.domain.valueobject.RecoveryToken;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Os codigos dos links de recuperacao, de um gerador criptografico (R-002 da 012): 256 bits, que ninguem adivinha.
 */
@Component
public class SecureRandomRecoveryTokens implements RecoveryTokens {

    private static final int BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    @Override
    public RecoveryToken issue() {
        byte[] bytes = new byte[BYTES];
        random.nextBytes(bytes);
        return new RecoveryToken(encoder.encodeToString(bytes));
    }
}
