package io.github.ovyx.shared.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Sinaliza violacao de invariante ou de regra de dominio.
 *
 * <p>
 * O dominio nunca usa Result: ele lanca esta excecao, e a camada de aplicacao a
 * traduz para um Failure no proprio limite dela.
 * </p>
 */
public class DomainException extends RuntimeException {

    private final transient ErrorCode errorCode;

    private final transient Map<String, String> details;

    public DomainException(
            final ErrorCode errorCode,
            final String message
    ) {
        this(errorCode, message, Map.of());
    }

    public DomainException(
            final ErrorCode errorCode,
            final String message,
            final Map<String, String> details
    ) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
        this.details = Map.copyOf(Objects.requireNonNull(details, "details"));
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Map<String, String> details() {
        return details;
    }
}
