package io.github.ovyx.shared.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    private final transient List<Violation> violations;

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
        this.violations = List.of();
    }

    /**
     * Recusa com as violacoes regra a regra, como o {@link Notification} as reuniu.
     *
     * <p>O {@code details} publicado continua sendo campo -> mensagem: as mensagens de um mesmo campo
     * sao unidas, sem repetir texto, na ordem em que os campos foram recusados.
     */
    public DomainException(
            final ErrorCode errorCode,
            final String message,
            final List<Violation> violations
    ) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
        this.violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
        this.details = detailsOf(this.violations);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Map<String, String> details() {
        return details;
    }

    /** As violacoes regra a regra; vazio quando a recusa nao veio de um {@link Notification}. */
    public List<Violation> violations() {
        return violations;
    }

    private static Map<String, String> detailsOf(List<Violation> violations) {
        Map<String, String> details = violations.stream()
                .collect(Collectors.groupingBy(
                        Violation::field,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                Violation::message,
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(LinkedHashSet::new),
                                        messages -> String.join(" ", messages)))));
        return Collections.unmodifiableMap(details);
    }
}
