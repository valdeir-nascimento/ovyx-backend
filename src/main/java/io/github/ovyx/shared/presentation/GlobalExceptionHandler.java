package io.github.ovyx.shared.presentation;

import io.github.ovyx.shared.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rede de seguranca da borda HTTP.
 *
 * <p>
 * O caminho normal de erro de negocio e o Result, tratado pelo
 * {@link ResultHttpMapper}. Este advice cobre o que nao passa por la: falha de
 * Bean Validation, excecao de dominio que escapou e falha inesperada.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> onValidation(final MethodArgumentNotValidException exception) {
        final Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(
            error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        final ProblemDetail body = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "A requisição contém campos inválidos."
        );
        body.setTitle("Dados inválidos");
        body.setProperty("code", "VALIDATION_FAILED");
        body.setProperty("details", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Valor de caminho ou de parametro que nao converte para o tipo esperado.
     *
     * <p>
     * Sem este tratamento a requisicao cai no {@code Exception} generico e vira
     * <b>500</b> — um erro de digitacao do cliente respondido como falha do
     * servidor. Vale para enum desconhecido no caminho, UUID malformado e data fora
     * do formato ISO.
     * </p>
     *
     * <p>
     * O nome do parametro entra em {@code details}; o valor recebido, nao. Ele veio
     * de fora e pode ser qualquer coisa, inclusive dado que nao deveria ser
     * refletido de volta.
     * </p>
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> onTypeMismatch(
        final MethodArgumentTypeMismatchException exception
    ) {
        final ProblemDetail body = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Valor inválido para o parâmetro '%s'.".formatted(exception.getName())
        );
        body.setTitle("Dados inválidos");
        body.setProperty("code", "VALIDATION_FAILED");
        body.setProperty("details", Map.of("parameter", exception.getName()));
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Excecao de dominio so chega aqui se algum handler esqueceu de traduzi-la em
     * Failure. Responde de forma util e registra para correcao.
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> onDomain(final DomainException exception) {
        LOG.warn(
            "Excecao de dominio nao traduzida em Result: code={}",
            exception.errorCode().code()
        );

        final ProblemDetail body = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            exception.getMessage()
        );
        body.setTitle("Operação recusada");
        body.setProperty("code", exception.errorCode().code());
        if (!exception.details().isEmpty()) {
            body.setProperty("details", exception.details());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Erros que o proprio Spring MVC ja classificou: rota inexistente, metodo nao
     * suportado, media type invalido, corpo ausente.
     *
     * <p>
     * Sem este tratamento eles caem no {@code Exception} generico e viram <b>500</b> —
     * um erro do cliente respondido como falha do servidor, e um alarme de producao
     * disparado por alguem que digitou a URL errada. O status correto vem do proprio
     * {@link ErrorResponse}.
     * </p>
     */
    @ExceptionHandler({
        NoResourceFoundException.class,
        HttpRequestMethodNotSupportedException.class,
        HttpMediaTypeNotSupportedException.class,
        HttpMediaTypeNotAcceptableException.class,
        MissingServletRequestParameterException.class
    })
    public ResponseEntity<ProblemDetail> onSpringMvcError(final ErrorResponse exception) {
        final HttpStatusCode status = exception.getStatusCode();

        final ProblemDetail body = ProblemDetail.forStatusAndDetail(
            status,
            "A requisição não é suportada por este endereço."
        );
        body.setTitle("Requisição não suportada");
        body.setProperty("code", status.value() == HttpStatus.NOT_FOUND.value()
            ? "ROUTE_NOT_FOUND"
            : "REQUEST_NOT_ACCEPTABLE");

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Corpo que o parser nao conseguiu ler.
     *
     * <p>Tem tratador proprio porque {@link HttpMessageNotReadableException} <b>nao</b> implementa
     * {@code ErrorResponse}: no grupo acima, o Spring nao conseguia injeta-la e a resposta saia com
     * 400 e nenhum corpo.
     *
     * <p>A mensagem e generica de proposito. O texto do parser carrega o trecho que ele leu, e um
     * corpo de login malformado traz a senha (FR-021).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> onUnreadableBody(final HttpMessageNotReadableException exception) {
        final ProblemDetail body = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "O corpo da requisição não pôde ser lido."
        );
        body.setTitle("Requisição inválida");
        body.setProperty("code", "REQUEST_NOT_ACCEPTABLE");

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> onUnexpected(final Exception exception) {
        LOG.error("Falha inesperada ao processar a requisicao", exception);

        final ProblemDetail body = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Não foi possível concluir a operação. Tente novamente em instantes."
        );
        body.setTitle("Erro inesperado");
        body.setProperty("code", "INTERNAL_ERROR");
        return ResponseEntity.internalServerError().body(body);
    }
}
