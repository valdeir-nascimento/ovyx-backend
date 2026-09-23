package io.github.ovyx.shared.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Relogio controlado pelo teste.
 *
 * <p>Permite verificar expiracao de sessao e janela de contencao de tentativas sem esperar tempo
 * real — que produziria suite lenta e intermitente.
 */
public final class FixedClock extends Clock {

    private final ZoneId zone;

    private Instant current;

    public FixedClock(Instant start) {
        this(start, ZoneOffset.UTC);
    }

    private FixedClock(Instant start, ZoneId zone) {
        this.current = start;
        this.zone = zone;
    }

    public static FixedClock at(String isoInstant) {
        return new FixedClock(Instant.parse(isoInstant));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId other) {
        return new FixedClock(current, other);
    }

    @Override
    public Instant instant() {
        return current;
    }

    /** Avanca o relogio, para exercitar o que depende da passagem do tempo. */
    public void advance(Duration amount) {
        current = current.plus(amount);
    }
}
