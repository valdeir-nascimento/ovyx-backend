package io.github.ovyx.shared.domain;

import java.util.Objects;

/**
 * Uma regra violada num campo.
 *
 * <p>O {@code code} identifica a regra; a {@code message} e o texto em portugues que a pessoa le.
 * Quem precisa reagir a recusa — um teste, um log, uma metrica — olha o codigo, porque o texto pode
 * mudar sem aviso (principio III).
 *
 * @param field nome do campo recusado, como o corpo da requisicao o chama
 * @param code codigo estavel da regra, em SCREAMING_SNAKE_CASE
 * @param message mensagem ao usuario final, em portugues
 */
public record Violation(String field, ErrorCode code, String message) {

    public Violation {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }
}
