package io.github.ovyx.shared.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Reune as violacoes de uma operacao antes de recusa-la (FR-017).
 *
 * <p>O padrao Notification: em vez de parar na primeira regra quebrada, cada objeto de valor
 * registra aqui o que recusou, e o agregado recusa uma vez so, no fim, com o conjunto completo. A
 * {@link DomainException} e o sinal para fora do dominio, como o principio III define; ela nao e o
 * mecanismo de acumulacao aqui dentro.
 *
 * <p>Fica no kernel porque cada contexto delimitado precisa dele, e o cliente usa o mesmo nome para
 * o mesmo papel.
 */
public final class Notification {

    private static final String REFUSAL_MESSAGE = "Dados inválidos.";

    private final List<Violation> violations = new ArrayList<>();

    /** Registra uma violacao que nao cabe numa regra declarativa, como a senha atual incorreta. */
    public Notification add(String field, ErrorCode code, String message) {
        violations.add(new Violation(field, code, message));
        return this;
    }

    /**
     * Registra a ausencia do campo, se for o caso, e diz se as demais regras devem ser avaliadas.
     *
     * <p>Sem valor, as outras regras so repetiriam, com outras palavras, que o campo esta vazio.
     *
     * @return {@code true} quando o valor veio preenchido
     */
    public boolean requirePresent(String field, String value, ErrorCode code, String message) {
        boolean present = value != null && !value.isBlank();
        if (!present) {
            add(field, code, message);
        }
        return present;
    }

    /** Avalia as regras do campo e registra uma violacao para cada regra nao satisfeita. */
    public <T> Notification check(String field, T value, List<Rule<T>> rules) {
        rules.stream()
                .filter(rule -> !rule.isSatisfiedBy(value))
                .map(rule -> rule.violationOf(field))
                .forEach(violations::add);
        return this;
    }

    public boolean hasErrors() {
        return !violations.isEmpty();
    }

    /** As violacoes na ordem em que foram registradas. */
    public List<Violation> violations() {
        return List.copyOf(violations);
    }

    /**
     * Recusa o conjunto, se houver o que recusar.
     *
     * @param code codigo da recusa como um todo; cada violacao carrega o codigo da propria regra
     */
    public void throwIfAny(ErrorCode code) {
        if (hasErrors()) {
            throw new DomainException(code, REFUSAL_MESSAGE, violations);
        }
    }
}
