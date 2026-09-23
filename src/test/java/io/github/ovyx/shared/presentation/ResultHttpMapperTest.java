package io.github.ovyx.shared.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/**
 * Testes da traducao do {@code Result} em resposta HTTP.
 *
 * <p>E o unico ponto do sistema que conhece status HTTP e regra de negocio ao mesmo tempo: se o
 * mapeamento sair errado, o cliente recebe o status errado para a regra certa.
 */
@DisplayName("ResultHttpMapper")
class ResultHttpMapperTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ResultHttpMapper mapper = new ResultHttpMapper(meterRegistry);

    private static ProblemDetail bodyOf(ResponseEntity<Object> response) {
        return (ProblemDetail) response.getBody();
    }

    @Test
    @DisplayName("answers 200 with the value on success")
    void answersOkOnSuccess() {
        ResponseEntity<Object> response = mapper.ok(Result.success("ovyx"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("ovyx");
    }

    @Test
    @DisplayName("answers 204 without body on success")
    void answersNoContentOnSuccess() {
        ResponseEntity<Object> response = mapper.noContent(Result.success("ovyx"));

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("answers 201 with Location on success")
    void answersCreatedOnSuccess() {
        ResponseEntity<Object> response =
                mapper.created(Result.success("ovyx"), value -> URI.create("/api/v1/" + value));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/ovyx");
    }

    @Test
    @DisplayName("turns a validation failure into 400 carrying the code and every field")
    void turnsValidationFailureIntoBadRequest() {
        ApplicationError error = new ApplicationError(
                ErrorType.VALIDATION,
                "VALIDATION_FAILED",
                "Dados inválidos.",
                Map.of("email", "Informe o e-mail.", "cpf", "CPF inválido."));

        ResponseEntity<Object> response = mapper.ok(Result.failure(error));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(bodyOf(response).getProperties())
                .containsEntry("code", "VALIDATION_FAILED")
                .containsEntry("details", Map.of("email", "Informe o e-mail.", "cpf", "CPF inválido."));
    }

    @Test
    @DisplayName("maps each error type to the status the contract promises")
    void mapsEveryErrorTypeToItsStatus() {
        assertThat(statusOf(ErrorType.VALIDATION)).isEqualTo(400);
        assertThat(statusOf(ErrorType.UNAUTHENTICATED)).isEqualTo(401);
        assertThat(statusOf(ErrorType.FORBIDDEN)).isEqualTo(403);
        assertThat(statusOf(ErrorType.NOT_FOUND)).isEqualTo(404);
        assertThat(statusOf(ErrorType.CONFLICT)).isEqualTo(409);
        assertThat(statusOf(ErrorType.BUSINESS_RULE)).isEqualTo(409);
    }

    @Test
    @DisplayName("counts each failure by code, so the rule can be watched in production")
    void countsEachFailureByCode() {
        mapper.problem(ApplicationError.of(ErrorType.UNAUTHENTICATED, "INVALID_CREDENTIALS", "Inválidos."));
        mapper.problem(ApplicationError.of(ErrorType.UNAUTHENTICATED, "INVALID_CREDENTIALS", "Inválidos."));

        assertThat(meterRegistry
                        .counter("ovyx.result.failure", "code", "INVALID_CREDENTIALS", "type", "UNAUTHENTICATED")
                        .count())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("omits details when the error carries none")
    void omitsEmptyDetails() {
        ResponseEntity<Object> response =
                mapper.problem(ApplicationError.of(ErrorType.FORBIDDEN, "FORBIDDEN", "Sem permissão."));

        assertThat(bodyOf(response).getProperties()).doesNotContainKey("details");
    }

    private int statusOf(ErrorType type) {
        return mapper.problem(ApplicationError.of(type, "ANY_CODE", "mensagem"))
                .getStatusCode()
                .value();
    }
}
