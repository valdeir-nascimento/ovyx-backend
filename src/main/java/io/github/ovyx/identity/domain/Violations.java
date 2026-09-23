package io.github.ovyx.identity.domain;

import io.github.ovyx.shared.domain.DomainException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Reune as violacoes de um mesmo cadastro antes de recusa-lo.
 *
 * <p>Existe porque FR-017 exige que a pessoa veja <strong>todas</strong> as violacoes de uma vez:
 * corrigir um campo, submeter, e descobrir o proximo erro e o comportamento que o requisito proibe.
 * O dominio continua sinalizando por {@link DomainException}, como o kernel compartilhado define —
 * so que uma unica vez, ja com o conjunto completo em {@code details}.
 *
 * <p>Cada objeto de valor valida o proprio campo e lanca; {@link #collect(Supplier)} apara essa
 * excecao, guarda os campos que ela trouxe e segue para o campo seguinte.
 */
public final class Violations {

    private final Map<String, String> details = new LinkedHashMap<>();

    /**
     * Executa a fabrica de um objeto de valor, guardando as violacoes em vez de interromper.
     *
     * @return o valor criado, ou {@code null} quando houve violacao
     */
    public <T> T collect(Supplier<T> factory) {
        try {
            return factory.get();
        } catch (DomainException violation) {
            merge(violation.details());
            return null;
        }
    }

    /** Acrescenta uma violacao. Duas regras do mesmo campo somam as mensagens, sem descartar nenhuma. */
    public Violations add(String field, String message) {
        details.merge(field, message, (first, second) -> first + " " + second);
        return this;
    }

    public void merge(Map<String, String> other) {
        other.forEach(this::add);
    }

    public boolean hasAny() {
        return !details.isEmpty();
    }

    public Map<String, String> details() {
        return Map.copyOf(details);
    }

    /** Recusa o conjunto, se houver o que recusar. */
    public void throwIfAny() {
        if (hasAny()) {
            throw new DomainException(IdentityErrorCode.VALIDATION_FAILED, "Dados inválidos.", details());
        }
    }

    /** Recusa de imediato um unico campo, para quem nao precisa acumular. */
    public static DomainException of(String field, String message) {
        return new DomainException(IdentityErrorCode.VALIDATION_FAILED, message, Map.of(field, message));
    }
}
