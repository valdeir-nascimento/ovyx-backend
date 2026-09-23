package io.github.ovyx.shared.domain;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Uma regra de validacao: o padrao Specification, com o codigo e a mensagem da violacao.
 *
 * <p>Cada regra e um objeto, e nao um {@code if} dentro da fabrica: o objeto de valor declara a
 * lista das suas regras, e uma regra nova entra como uma linha nessa lista, sem tocar nas outras. A
 * mesma lista serve a quem acumula violacoes e a quem recusa na hora, entao as duas entradas nao
 * divergem.
 *
 * @param <T> tipo do valor verificado
 */
public final class Rule<T> {

    private final Predicate<T> specification;
    private final ErrorCode code;
    private final String message;

    private Rule(Predicate<T> specification, ErrorCode code, String message) {
        this.specification = Objects.requireNonNull(specification, "specification");
        this.code = Objects.requireNonNull(code, "code");
        this.message = Objects.requireNonNull(message, "message");
    }

    /**
     * @param specification o que o valor precisa satisfazer
     * @param code codigo da regra, quando nao satisfeita
     * @param message mensagem ao usuario final, quando nao satisfeita
     */
    public static <T> Rule<T> of(Predicate<T> specification, ErrorCode code, String message) {
        return new Rule<>(specification, code, message);
    }

    public boolean isSatisfiedBy(T value) {
        return specification.test(value);
    }

    Violation violationOf(String field) {
        return new Violation(field, code, message);
    }
}
