package io.github.ovyx.identity.presentation.security;

import io.github.ovyx.shared.presentation.EdgeErrorCode;
import io.github.ovyx.shared.presentation.ProblemResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class ProblemAccessDeniedHandler implements AccessDeniedHandler {

    /** Título do 403 genérico, o mesmo em toda recusa de permissão (FR-010). */
    static final String FORBIDDEN_TITLE = "Acesso negado";

    /** Frase do 403 genérico: não diz qual recurso foi negado, nem que ele existe (FR-010). */
    static final String FORBIDDEN_DETAIL = "Você não tem permissão para executar esta operação.";

    private final ObjectMapper objectMapper;

    public ProblemAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        // Token CSRF ausente tambem chega aqui como AccessDeniedException, mas nao e falta de
        // permissao: o cliente resolve obtendo o cookie XSRF-TOKEN e repetindo. Tratado como
        // "acesso negado", mandava o usuario para a tela de permissao na primeira tentativa de login.
        boolean csrf = accessDeniedException instanceof CsrfException;

        ProblemDetail problem = csrf
            ? ProblemResponses.of(
                HttpStatus.FORBIDDEN,
                EdgeErrorCode.CSRF_TOKEN_INVALID,
                "Proteção da requisição ausente",
                "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
            URI.create(request.getRequestURI()))
            : ProblemResponses.of(
                HttpStatus.FORBIDDEN,
                EdgeErrorCode.FORBIDDEN,
                FORBIDDEN_TITLE,
                FORBIDDEN_DETAIL,
                URI.create(request.getRequestURI()));

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
