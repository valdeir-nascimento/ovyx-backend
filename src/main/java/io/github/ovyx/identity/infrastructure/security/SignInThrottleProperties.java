package io.github.ovyx.identity.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parametros da contencao de tentativas de acesso (FR-023).
 *
 * @param maxFailures falhas toleradas dentro da janela antes do bloqueio
 * @param window duracao da janela deslizante
 * @param blockDuration por quanto tempo a combinacao fica bloqueada depois de estourar o limite
 */
@ConfigurationProperties(prefix = "ovyx.security.sign-in-throttle")
public record SignInThrottleProperties(
        @DefaultValue("5") int maxFailures,
        @DefaultValue("15m") Duration window,
        @DefaultValue("15m") Duration blockDuration) {}
