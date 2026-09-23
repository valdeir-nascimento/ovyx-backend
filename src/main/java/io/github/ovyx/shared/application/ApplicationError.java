package io.github.ovyx.shared.application;

import io.github.ovyx.shared.domain.DomainException;

import java.util.Map;
import java.util.Objects;

/**
 * Erro devolvido por um caso de uso.
 *
 * <p>
 * Chama-se ApplicationError, e nao Error, para nao sombrear {@link java.lang.Error}
 * dentro do pacote.
 * </p>
 *
 * @param type    natureza da falha, usada na traducao para HTTP.
 * @param code    codigo estavel da regra violada.
 * @param message mensagem legivel; nao faz parte do contrato.
 * @param details contexto adicional; nas recusas de validacao, uma mensagem por campo.
 */
public record ApplicationError(
    ErrorType type,
    String code,
    String message,
    Map<String, String> details
) {

    public ApplicationError {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        details = Map.copyOf(Objects.requireNonNull(details, "details"));
    }

    public static ApplicationError of(
        final ErrorType type,
        final String code,
        final String message
    ) {
        return new ApplicationError(type, code, message, Map.of());
    }

    /**
     * Traduz uma excecao de dominio no erro equivalente da aplicacao.
     *
     * <p>
     * E aqui que o Result nasce: o dominio sinaliza por excecao, a aplicacao
     * converte no seu proprio limite.
     * </p>
     */
    public static ApplicationError from(
        final DomainException exception,
        final ErrorType type
    ) {
        return new ApplicationError(
            type,
            exception.errorCode().code(),
            exception.getMessage(),
            exception.details()
        );
    }
}
