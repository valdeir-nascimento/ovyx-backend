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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    void givenSuccess_whenAnsweringOk_thenReturn200WithTheValue() {
        // given
        Result<String> success = Result.success("ovyx");

        // when
        ResponseEntity<Object> response = mapper.ok(success);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("ovyx");
    }

    @Test
    @DisplayName("answers 204 without body on success")
    void givenSuccess_whenAnsweringNoContent_thenReturn204WithoutBody() {
        // given
        Result<String> success = Result.success("ovyx");

        // when
        ResponseEntity<Object> response = mapper.noContent(success);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("answers 201 with Location on success")
    void givenSuccess_whenAnsweringCreated_thenReturn201WithLocation() {
        // given
        Result<String> success = Result.success("ovyx");

        // when
        ResponseEntity<Object> response = mapper.created(success, value -> URI.create("/api/v1/" + value));

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/ovyx");
    }

    @Test
    @DisplayName("turns a validation failure into 400 carrying the code and every field")
    void givenValidationFailureWithTwoFields_whenAnswering_thenReturn400WithCodeAndEveryField() {
        // given
        ApplicationError error = new ApplicationError(
                ErrorType.VALIDATION,
                "VALIDATION_FAILED",
                "Dados inválidos.",
                Map.of("email", "Informe o e-mail.", "cpf", "CPF inválido."));

        // when
        ResponseEntity<Object> response = mapper.ok(Result.failure(error));

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(bodyOf(response).getProperties())
                .containsEntry("code", "VALIDATION_FAILED")
                .containsEntry("details", Map.of("email", "Informe o e-mail.", "cpf", "CPF inválido."));
    }

    @ParameterizedTest(name = "{0} answers {1}")
    @CsvSource({
        "VALIDATION, 400",
        "UNAUTHENTICATED, 401",
        "FORBIDDEN, 403",
        "NOT_FOUND, 404",
        "CONFLICT, 409",
        "BUSINESS_RULE, 409"
    })
    @DisplayName("maps each error type to the status the contract promises")
    void givenErrorType_whenAnsweringTheProblem_thenUseTheStatusTheContractPromises(ErrorType type, int status) {
        // given
        ApplicationError error = ApplicationError.of(type, "ANY_CODE", "mensagem");

        // when
        ResponseEntity<Object> response = mapper.problem(error);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(status);
    }

    @Test
    @DisplayName("counts each failure by code, so the rule can be watched in production")
    void givenTwoFailuresWithTheSameCode_whenAnswering_thenCountBothUnderThatCode() {
        // given
        ApplicationError invalidCredentials =
                ApplicationError.of(ErrorType.UNAUTHENTICATED, "INVALID_CREDENTIALS", "Inválidos.");

        // when
        mapper.problem(invalidCredentials);
        mapper.problem(invalidCredentials);

        // then
        assertThat(meterRegistry
                        .counter("ovyx.result.failure", "code", "INVALID_CREDENTIALS", "type", "UNAUTHENTICATED")
                        .count())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("omits details when the error carries none")
    void givenErrorWithoutDetails_whenAnswering_thenOmitTheDetailsProperty() {
        // given
        ApplicationError withoutDetails = ApplicationError.of(ErrorType.FORBIDDEN, "FORBIDDEN", "Sem permissão.");

        // when
        ResponseEntity<Object> response = mapper.problem(withoutDetails);

        // then
        assertThat(bodyOf(response).getProperties()).doesNotContainKey("details");
    }
}
