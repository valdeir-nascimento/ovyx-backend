package io.github.ovyx.shared.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

/**
 * Testes da rede de seguranca da borda HTTP.
 *
 * <p>O caminho normal de recusa e o {@code Result}. Este advice cobre o que nao passa por la — e o
 * que ele nao cobrir vira 500, isto e, erro do cliente respondido como falha do servidor.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private static final ErrorCode CODE = () -> "CARETAKER_INACTIVE";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static Stream<Arguments> frameworkErrors() {
        return Stream.of(
                Arguments.of(new HttpRequestMethodNotSupportedException("PATCH"), 405),
                Arguments.of(new HttpMediaTypeNotAcceptableException("application/xml"), 406),
                Arguments.of(
                        new HttpMediaTypeNotSupportedException(
                                MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON)),
                        415));
    }

    @ParameterizedTest(name = "{0} keeps {1}")
    @MethodSource("frameworkErrors")
    @DisplayName("keeps the status of every framework error it translates")
    void givenFrameworkError_whenTranslating_thenKeepItsStatusUnderOneStableCode(
            ErrorResponse error, int status) {
        // given — error and its status from @MethodSource

        // when
        ResponseEntity<ProblemDetail> response = handler.onSpringMvcError(error);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getBody().getProperties()).containsEntry("code", "REQUEST_NOT_ACCEPTABLE");
    }

    @Test
    @DisplayName("answers in Portuguese, never echoing what the client sent")
    void givenMethodNotSupported_whenTranslating_thenAnswerInPortugueseWithoutEchoingTheRequest() {
        // given
        HttpRequestMethodNotSupportedException patchNotSupported = new HttpRequestMethodNotSupportedException("PATCH");

        // when
        ResponseEntity<ProblemDetail> response = handler.onSpringMvcError(patchNotSupported);

        // then
        assertThat(response.getBody().getDetail()).isEqualTo("A requisição não é suportada por este endereço.");
        assertThat(response.getBody().getTitle()).isEqualTo("Requisição não suportada");
    }

    @Test
    @DisplayName("translates a domain exception that escaped a handler, keeping its code")
    void givenDomainExceptionThatEscapedAHandler_whenTranslating_thenAnswer409KeepingItsCode() {
        // given
        DomainException escaped = new DomainException(CODE, "Responsável inativo.", Map.of("status", "INACTIVE"));

        // when
        ResponseEntity<ProblemDetail> response = handler.onDomain(escaped);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getProperties())
                .containsEntry("code", "CARETAKER_INACTIVE")
                .containsEntry("details", Map.of("status", "INACTIVE"));
    }

    @Test
    @DisplayName("turns an unexpected failure into a generic 500 without leaking its cause")
    void givenUnexpectedFailureWithInternalDetails_whenTranslating_thenAnswerGeneric500WithoutLeakingThem() {
        // given
        IllegalStateException unexpected = new IllegalStateException("conexão recusada em postgres://interno:5432");

        // when
        ResponseEntity<ProblemDetail> response = handler.onUnexpected(unexpected);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getProperties()).containsEntry("code", "INTERNAL_ERROR");
        assertThat(response.getBody().getDetail()).doesNotContain("postgres");
    }
}
