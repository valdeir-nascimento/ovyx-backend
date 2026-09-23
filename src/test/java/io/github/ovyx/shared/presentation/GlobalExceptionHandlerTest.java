package io.github.ovyx.shared.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.ErrorCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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

    @Test
    @DisplayName("keeps the status of every framework error it translates")
    void keepsTheStatusOfFrameworkErrors() {
        ResponseEntity<ProblemDetail> methodNotAllowed =
                handler.onSpringMvcError(new HttpRequestMethodNotSupportedException("PATCH"));
        ResponseEntity<ProblemDetail> notAcceptable =
                handler.onSpringMvcError(new HttpMediaTypeNotAcceptableException("application/xml"));
        ResponseEntity<ProblemDetail> unsupportedMediaType =
                handler.onSpringMvcError(new HttpMediaTypeNotSupportedException(
                        MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON)));

        assertThat(methodNotAllowed.getStatusCode().value()).isEqualTo(405);
        assertThat(notAcceptable.getStatusCode().value()).isEqualTo(406);
        assertThat(unsupportedMediaType.getStatusCode().value()).isEqualTo(415);
        assertThat(List.of(methodNotAllowed.getBody(), notAcceptable.getBody(), unsupportedMediaType.getBody()))
                .allSatisfy(problem ->
                        assertThat(problem.getProperties()).containsEntry("code", "REQUEST_NOT_ACCEPTABLE"));
    }

    @Test
    @DisplayName("answers in Portuguese, never echoing what the client sent")
    void answersInPortugueseWithoutEchoingTheRequest() {
        ResponseEntity<ProblemDetail> response =
                handler.onSpringMvcError(new HttpRequestMethodNotSupportedException("PATCH"));

        assertThat(response.getBody().getDetail()).isEqualTo("A requisição não é suportada por este endereço.");
        assertThat(response.getBody().getTitle()).isEqualTo("Requisição não suportada");
    }

    @Test
    @DisplayName("translates a domain exception that escaped a handler, keeping its code")
    void translatesEscapedDomainException() {
        ResponseEntity<ProblemDetail> response =
                handler.onDomain(new DomainException(CODE, "Responsável inativo.", Map.of("status", "INACTIVE")));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getProperties())
                .containsEntry("code", "CARETAKER_INACTIVE")
                .containsEntry("details", Map.of("status", "INACTIVE"));
    }

    @Test
    @DisplayName("turns an unexpected failure into a generic 500 without leaking its cause")
    void turnsUnexpectedFailureIntoGeneric500() {
        ResponseEntity<ProblemDetail> response =
                handler.onUnexpected(new IllegalStateException("conexão recusada em postgres://interno:5432"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getProperties()).containsEntry("code", "INTERNAL_ERROR");
        assertThat(response.getBody().getDetail()).doesNotContain("postgres");
    }
}
