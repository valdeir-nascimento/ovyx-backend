package io.github.ovyx.identity.presentation.security;

import io.github.ovyx.shared.presentation.EdgeErrorCode;
import io.github.ovyx.shared.presentation.ProblemResponses;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;
import tools.jackson.databind.ObjectMapper;

/**
 * Recusa de CORS no formato de erro do contrato (RFC 9457).
 *
 * <p>O Spring respondia 403 com o texto "Invalid CORS request", fora do Problem Details que o documento
 * promete a toda resposta de erro. A recusa é a mesma do acesso negado ({@code FORBIDDEN}, frase
 * genérica): origem, método ou cabeçalho fora do permitido não revelam qual das três coisas falhou
 * (FR-010).
 */
class ProblemCorsProcessor extends DefaultCorsProcessor {

    /**
     * O caminho da requisição em avaliação, para o {@code instance}. A recusa só recebe a resposta; o
     * caminho vem de {@link #processRequest}, na mesma thread, e não de um filtro de fora.
     */
    private static final ThreadLocal<URI> REQUEST_PATH = new ThreadLocal<>();

    private final ObjectMapper objectMapper;

    ProblemCorsProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean processRequest(CorsConfiguration config, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        REQUEST_PATH.set(URI.create(request.getRequestURI()));
        try {
            return super.processRequest(config, request, response);
        } finally {
            REQUEST_PATH.remove();
        }
    }

    @Override
    protected void rejectRequest(ServerHttpResponse response) throws IOException {
        ProblemDetail problem = ProblemResponses.of(
                HttpStatus.FORBIDDEN,
                EdgeErrorCode.FORBIDDEN,
                ProblemAccessDeniedHandler.FORBIDDEN_TITLE,
                ProblemAccessDeniedHandler.FORBIDDEN_DETAIL,
                REQUEST_PATH.get());
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.getBody().write(objectMapper.writeValueAsBytes(problem));
        response.flush();
    }

}
