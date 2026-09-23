package io.github.ovyx.shared.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.ErrorCode;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
    void givenValue_whenWrappingAsSuccess_thenCarryTheValue() {
        // given
        String value = "ovyx";

        // when
        Result<String> result = Result.success(value);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.value()).isEqualTo("ovyx");
    }

    @Test
    @DisplayName("failure carries the error with its stable code")
    void givenApplicationError_whenWrappingAsFailure_thenCarryTheErrorWithItsCode() {
        // given
        ApplicationError error = error();

        // when
        Result<String> result = Result.failure(error);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("RULE_VIOLATED");
        assertThat(result.error().type()).isEqualTo(ErrorType.BUSINESS_RULE);
    }

    @Test
    @DisplayName("reading the value of a failure is a programming error, not business flow")
    void givenFailure_whenReadingTheValue_thenThrowIllegalState() {
        // given
        Result<String> result = Result.failure(error());

        // when
        ThrowingCallable reading = result::value;

        // then
        assertThatThrownBy(reading).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("reading the error of a success is a programming error too")
    void givenSuccess_whenReadingTheError_thenThrowIllegalState() {
        // given
        Result<String> result = Result.success("ovyx");

        // when
        ThrowingCallable reading = result::error;

        // then
        assertThatThrownBy(reading).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("map transforms the value on success")
    void givenSuccess_whenMapping_thenTransformTheValue() {
        // given
        Result<String> success = Result.success("ovyx");

        // when
        Result<Integer> mapped = success.map(String::length);

        // then
        assertThat(mapped.value()).isEqualTo(4);
    }

    @Test
    @DisplayName("map skips the function on failure and preserves the error")
    void givenFailure_whenMapping_thenSkipTheFunctionAndKeepTheError() {
        // given
        Result<String> failure = Result.failure(error());

        // when
        Result<Integer> mapped = failure.map(value -> {
            throw new AssertionError("a função não deveria rodar sobre uma falha");
        });

        // then
        assertThat(mapped.error().code()).isEqualTo("RULE_VIOLATED");
    }

    @Test
    @DisplayName("flatMap chains two fallible steps")
    void givenSuccess_whenFlatMappingToAnotherSuccess_thenChainTheSteps() {
        // given
        Result<String> success = Result.success("ovyx");

        // when
        Result<Integer> chained = success.flatMap(value -> Result.success(value.length()));

        // then
        assertThat(chained.value()).isEqualTo(4);
    }

    @Test
    @DisplayName("flatMap propagates the first failure without running the second step")
    void givenFailure_whenFlatMapping_thenSkipTheSecondStepAndStayFailed() {
        // given
        Result<String> failure = Result.failure(error());

        // when
        Result<Integer> chained = failure.flatMap(value -> {
            throw new AssertionError("o segundo passo não deveria rodar");
        });

        // then
        assertThat(chained.isFailure()).isTrue();
    }

    @Test
    @DisplayName("translates a domain exception into the equivalent application error")
    void givenDomainException_whenTranslating_thenKeepCodeDetailsAndAssignTheType() {
        // given
        DomainException violation =
                new DomainException(CODE, "Regra violada.", Map.of("email", "Informe o e-mail."));

        // when
        ApplicationError translated = ApplicationError.from(violation, ErrorType.VALIDATION);

        // then
        assertThat(translated.code()).isEqualTo("RULE_VIOLATED");
        assertThat(translated.type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(translated.details()).containsEntry("email", "Informe o e-mail.");
    }

    @Test
    @DisplayName("a switch over Result is exhaustive without a default branch")
    void givenSealedResult_whenSwitchingWithoutDefault_thenCoverBothCases() {
        // given
        Result<String> result = Result.success("ovyx");

        // when
        String described = switch (result) {
            case Result.Success<String> success -> success.value();
            case Result.Failure<String> failure -> failure.error().code();
        };

        // then
        assertThat(described).isEqualTo("ovyx");
    }
}
