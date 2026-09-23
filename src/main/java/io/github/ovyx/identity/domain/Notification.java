package io.github.ovyx.identity.domain;

import io.github.ovyx.shared.domain.DomainException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reune as violacoes de uma mesma operacao antes de recusa-la.
 *
 * <p>Existe porque FR-017 exige que a pessoa veja <strong>todas</strong> as violacoes de uma vez:
 * corrigir um campo, submeter, e descobrir o proximo erro e o comportamento que o requisito proibe.
 *
 * <p>Cada objeto de valor escreve aqui o que recusou e devolve {@code null}; o agregado recusa uma
 * vez so, no fim, lancando {@link DomainException} com o conjunto completo em {@code details} — a
 * excecao e o sinal para fora, como o kernel compartilhado define, e nao o mecanismo de acumulacao
 * aqui dentro.
 *
 * <p>O nome e o mesmo do lado do cliente (`Notification` em `shared/domain`), para que as duas
 * pontas chamem de igual o que e igual.
 */
public final class Notification {

    private final Map<String, String> errors = new LinkedHashMap<>();

    /** Acrescenta uma violacao. Duas regras do mesmo campo somam as mensagens, sem descartar nenhuma. */
    public Notification add(String field, String message) {
        errors.merge(field, message, (first, second) -> first + " " + second);
        return this;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /** Uma mensagem por campo, na ordem em que os campos foram recusados. */
    public Map<String, String> errors() {
        return Map.copyOf(errors);
    }

    /** Recusa o conjunto, se houver o que recusar. */
    public void throwIfAny() {
        if (hasErrors()) {
            throw new DomainException(IdentityErrorCode.VALIDATION_FAILED, "Dados inválidos.", errors());
        }
    }

    /** Recusa de imediato um unico campo, para quem nao tem o que acumular. */
    public static DomainException rejecting(String field, String message) {
        return new DomainException(IdentityErrorCode.VALIDATION_FAILED, message, Map.of(field, message));
    }
}
