package io.github.ovyx.shared.application;

import java.util.Objects;
import java.util.function.Function;

/**
 * Retorno de todo caso de uso da camada de aplicacao: sucesso ou erro tipado.
 *
 * <p>
 * O dominio nao conhece este tipo. A constituicao proibe Result dentro de
 * {@code domain}, para nao poluir os construtores dos agregados.
 * </p>
 *
 * @param <T> tipo do valor produzido em caso de sucesso.
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {

    record Success<T>(T value) implements Result<T> {
    }

    record Failure<T>(ApplicationError error) implements Result<T> {

        public Failure {
            Objects.requireNonNull(error, "error");
        }
    }

    static <T> Result<T> success(final T value) {
        return new Success<>(value);
    }

    static <T> Result<T> failure(final ApplicationError error) {
        return new Failure<>(error);
    }

    static <T> Result<T> failure(
        final ErrorType type,
        final String code,
        final String message
    ) {
        return new Failure<>(ApplicationError.of(type, code, message));
    }

    default boolean isSuccess() {
        return this instanceof Success<T>;
    }

    /** Oposto de {@link #isSuccess()}; existe para que a recusa seja lida sem negacao. */
    default boolean isFailure() {
        return this instanceof Failure<T>;
    }

    default T value() {
        return switch (this) {
            case Success<T> success -> success.value();
            case Failure<T> failure -> throw new IllegalStateException(
                "Result e Failure (" + failure.error().code() + "); nao ha valor"
            );
        };
    }

    default ApplicationError error() {
        return switch (this) {
            case Success<T> ignored -> throw new IllegalStateException("Result e Success; nao ha erro");
            case Failure<T> failure -> failure.error();
        };
    }

    /**
     * Aplica a funcao ao valor de sucesso, preservando o erro quando ha falha.
     */
    default <R> Result<R> map(final Function<T, R> mapper) {
        return switch (this) {
            case Success<T> success -> Result.success(mapper.apply(success.value()));
            case Failure<T> failure -> Result.failure(failure.error());
        };
    }

    /**
     * Encadeia outro passo que tambem pode falhar, sem aninhar Result dentro de Result.
     */
    default <R> Result<R> flatMap(final Function<T, Result<R>> mapper) {
        return switch (this) {
            case Success<T> success -> mapper.apply(success.value());
            case Failure<T> failure -> Result.failure(failure.error());
        };
    }
}
