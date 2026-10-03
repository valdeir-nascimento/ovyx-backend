package io.github.ovyx.identity.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parametros da contencao da recuperacao de senha por origem (FR-015 da 012).
 *
 * @param maxAttempts tentativas toleradas na janela; a seguinte bloqueia a origem
 * @param window duracao da janela
 * @param blockDuration por quanto tempo a origem fica bloqueada depois de estourar o limite
 */
@ConfigurationProperties(prefix = "ovyx.security.recovery-throttle")
public record RecoveryThrottleProperties(
        @DefaultValue("10") int maxAttempts,
        @DefaultValue("15m") Duration window,
        @DefaultValue("15m") Duration blockDuration) {}
