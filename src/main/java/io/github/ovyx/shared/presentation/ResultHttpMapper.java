package io.github.ovyx.shared.presentation;

import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.function.Function;

/**
 * Traduz o Result da camada de aplicacao em resposta HTTP.
 *
 * <p>
 * E o unico ponto do sistema que conhece status HTTP e regra de negocio ao mesmo
 * tempo. Erro de dominio nunca vaza como stack trace: vira ProblemDetail com o
 * codigo estavel da regra violada.
 * </p>
 */
@Component
public class ResultHttpMapper {

    private static final String FAILURE_METRIC = "ovyx.result.failure";

    private final MeterRegistry meterRegistry;

    public ResultHttpMapper(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public <T> ResponseEntity<Object> ok(final Result<T> result) {
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.value());
        }
        return problem(result.error());
    }

    public <T> ResponseEntity<Object> created(
            final Result<T> result,
            final Function<T, URI> locationBuilder
    ) {
        if (result.isSuccess()) {
            final T value = result.value();
            return ResponseEntity.created(locationBuilder.apply(value)).body(value);
        }
        return problem(result.error());
    }

    public <T> ResponseEntity<Object> noContent(final Result<T> result) {
        if (result.isSuccess()) {
            return ResponseEntity.noContent().build();
        }
        return problem(result.error());
    }

    /**
     * Traduz um erro de aplicacao em resposta, para quem monta o sucesso a mao — como a entrada no
     * sistema, que precisa abrir a sessao antes de responder.
     */
    public ResponseEntity<Object> problem(final ApplicationError error) {
        // Contador por codigo de regra violada: e o que torna SC-002 e SC-008
        // verificaveis em producao, e nao apenas em CI.
        meterRegistry.counter(
            FAILURE_METRIC,
            "code", error.code(),
            "type", error.type().name()
        ).increment();

        final HttpStatus status = statusOf(error.type());
        final ProblemDetail body = ProblemDetail.forStatusAndDetail(status, error.message());
        // Titulo em portugues, e nao o reason phrase do HTTP: e texto que o usuario final le
        // (principio VII).
        body.setTitle(titleOf(error.type()));
        body.setProperty("code", error.code());
        if (!error.details().isEmpty()) {
            body.setProperty("details", error.details());
        }
        // O tipo vem daqui, e nao da negociacao: numa rota com produces = application/json, o Spring
        // rotulava o erro como JSON comum, contra o contrato de toda resposta de erro.
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    private static String titleOf(final ErrorType type) {
        return switch (type) {
            case VALIDATION -> "Dados inválidos";
            case UNAUTHENTICATED -> "Não autenticado";
            case FORBIDDEN -> "Acesso negado";
            case NOT_FOUND -> "Não encontrado";
            case CONFLICT, BUSINESS_RULE -> "Operação recusada";
        };
    }

    private static HttpStatus statusOf(final ErrorType type) {
        return switch (type) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case CONFLICT, BUSINESS_RULE -> HttpStatus.CONFLICT;
        };
    }
}
