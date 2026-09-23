package io.github.ovyx.identity.presentation.security;

import io.github.ovyx.shared.presentation.AuthenticatedUser;
import io.github.ovyx.shared.presentation.EdgeErrorCode;
import io.github.ovyx.shared.presentation.ProblemResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Bloqueia toda operacao enquanto a troca de senha for obrigatoria (FR-025, cenario V-05).
 *
 * <p>Sem este filtro, o administrador semeado poderia usar o sistema inteiro com a senha
 * provisoria — que e conhecida por quem fez a instalacao, e por isso nao serve como credencial
 * permanente.
 *
 * <p>Quatro operacoes continuam liberadas, e por motivo concreto: trocar a senha, porque e o que se
 * espera que a pessoa faca; sair, porque prender alguem numa sessao nao ajuda ninguem; consultar a
 * propria identidade, porque e assim que o cliente descobre que precisa mostrar a tela de troca; e
 * entrar de novo, porque o login substitui a sessao pendente — barra-lo impedia ate de entrar com
 * outra conta no mesmo navegador.
 */
@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_WHILE_PENDING =
        Set.of(
            "PUT /api/v1/me/password",
            "POST /api/v1/auth/sign-out",
            "GET /api/v1/auth/me",
            "POST /api/v1/auth/sign-in"
        );


    private final ObjectMapper objectMapper;

    public PasswordChangeRequiredFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        AuthenticatedUser user = SessionAuthenticator.currentUser();

        if (user == null || !user.mustChangePassword() || isAllowed(request)) {
            chain.doFilter(request, response);
            return;
        }

        ProblemDetail problem = ProblemResponses.of(
            HttpStatus.FORBIDDEN,
            EdgeErrorCode.PASSWORD_CHANGE_REQUIRED,
            "Troca de senha obrigatória",
            "É necessário trocar a senha antes de executar qualquer outra operação.",
            URI.create(request.getRequestURI()));

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }

    private boolean isAllowed(HttpServletRequest request) {
        return ALLOWED_WHILE_PENDING.contains(request.getMethod() + " " + request.getRequestURI());
    }
}
