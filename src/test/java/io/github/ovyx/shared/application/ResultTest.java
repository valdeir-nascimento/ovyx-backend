package io.github.ovyx.shared.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.ErrorCode;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes do retorno dos casos de uso.
 *
 * <p>O {@code Result} e o que mantem a falha de negocio fora do fluxo de excecao na aplicacao: o
 * dominio sinaliza lancando, e a aplicacao converte no proprio limite.
 */
@DisplayName("Result")
class ResultTest {

    private static final ErrorCode CODE = () -> "RULE_VIOLATED";

    private static ApplicationError error() {
        return ApplicationError.of(ErrorType.BUSINESS_RULE, CODE.code(), "Regra violada.");
    }

    @Test
    @DisplayName("success carries the value")
    void successCarriesValue() {
        Result<String> result = Result.success("ovyx");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.value()).isEqualTo("ovyx");
    }

    @Test
    @DisplayName("failure carries the error with its stable code")
    void failureCarriesError() {
        Result<String> result = Result.failure(error());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("RULE_VIOLATED");
        assertThat(result.error().type()).isEqualTo(ErrorType.BUSINESS_RULE);
    }

    @Test
    @DisplayName("reading the value of a failure is a programming error, not business flow")
    void readingValueOfFailureIsProgrammingError() {
        Result<String> result = Result.failure(error());

        assertThatThrownBy(result::value).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("reading the error of a success is a programming error too")
    void readingErrorOfSuccessIsProgrammingError() {
        Result<String> result = Result.success("ovyx");

        assertThatThrownBy(result::error).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("map transforms the value on success")
    void mapTransformsSuccess() {
        Result<Integer> mapped = Result.success("ovyx").map(String::length);

        assertThat(mapped.value()).isEqualTo(4);
    }

    @Test
    @DisplayName("map skips the function on failure and preserves the error")
    void mapShortCircuitsOnFailure() {
        Result<String> failure = Result.failure(error());

        Result<Integer> mapped = failure.map(value -> {
            throw new AssertionError("a função não deveria rodar sobre uma falha");
        });

        assertThat(mapped.error().code()).isEqualTo("RULE_VIOLATED");
    }

    @Test
    @DisplayName("flatMap chains two fallible steps")
    void flatMapChainsFallibleSteps() {
        Result<Integer> chained = Result.success("ovyx").flatMap(value -> Result.success(value.length()));

        assertThat(chained.value()).isEqualTo(4);
    }

    @Test
    @DisplayName("flatMap propagates the first failure without running the second step")
    void flatMapShortCircuitsOnFailure() {
        Result<String> failure = Result.failure(error());

        Result<Integer> chained = failure.flatMap(value -> {
            throw new AssertionError("o segundo passo não deveria rodar");
        });

        assertThat(chained.isFailure()).isTrue();
    }

    @Test
    @DisplayName("translates a domain exception into the equivalent application error")
    void translatesDomainException() {
        DomainException violation =
                new DomainException(CODE, "Regra violada.", Map.of("email", "Informe o e-mail."));

        ApplicationError translated = ApplicationError.from(violation, ErrorType.VALIDATION);

        assertThat(translated.code()).isEqualTo("RULE_VIOLATED");
        assertThat(translated.type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(translated.details()).containsEntry("email", "Informe o e-mail.");
    }

    @Test
    @DisplayName("a switch over Result is exhaustive without a default branch")
    void switchIsExhaustiveWithoutDefault() {
        Result<String> result = Result.success("ovyx");

        String described = switch (result) {
            case Result.Success<String> success -> success.value();
            case Result.Failure<String> failure -> failure.error().code();
        };

        assertThat(described).isEqualTo("ovyx");
    }
}
