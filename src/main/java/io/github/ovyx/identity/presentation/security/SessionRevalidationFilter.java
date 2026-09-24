package io.github.ovyx.identity.presentation.security;

import io.github.ovyx.identity.application.authentication.AuthenticatedCaretaker;
import io.github.ovyx.identity.application.authentication.GetAuthenticatedCaretakerQuery;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.presentation.AuthenticatedUser;
import io.github.ovyx.shared.presentation.ProblemResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Confere, a cada requisicao autenticada, a situacao e o perfil atuais de quem esta na sessao
 * (FR-005, FR-008).
 *
 * <p>A autoridade fica gravada na sessao no login. Sem esta conferencia, um administrador inativado
 * continuava administrando pela sessao que ja tinha — inclusive cadastrando outro administrador para
 * continuar dentro —, e o rebaixado continuava administrador ate sair. Inativar e o meio de revogar
 * acesso, e precisa valer na requisicao seguinte.
 *
 * <p>Roda antes da autorizacao, para que ela decida pelo perfil atual:
 *
 * <ul>
 *   <li>responsavel inativo ou inexistente: a sessao e encerrada e a resposta e 401
 *       {@code CARETAKER_UNAVAILABLE}, a mesma da consulta da propria identidade;
 *   <li>perfil, nome ou obrigacao de troca de senha diferentes dos da sessao: a sessao e atualizada,
 *       e a requisicao segue com o que vale agora (cenario 4 da US3).
 * </ul>
 *
 * <p>A saida nao passa por aqui: encerrar a sessao de quem ja foi inativado so antecipa o que ela
 * mesma faria, e manter a saida registra o evento na auditoria. O custo e uma consulta por
 * requisicao autenticada, aceitavel para as dezenas de pessoas que usam o sistema.
 */
@Component
public class SessionRevalidationFilter extends OncePerRequestFilter {

    private static final Set<String> NOT_REVALIDATED =
        Set.of("POST /api/v1/auth/sign-in", "POST /api/v1/auth/sign-out");

    private final Dispatcher dispatcher;
    private final SessionAuthenticator sessionAuthenticator;
    private final ObjectMapper objectMapper;

    public SessionRevalidationFilter(
        Dispatcher dispatcher, SessionAuthenticator sessionAuthenticator, ObjectMapper objectMapper) {
        this.dispatcher = dispatcher;
        this.sessionAuthenticator = sessionAuthenticator;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        AuthenticatedUser user = SessionAuthenticator.currentUser();
        if (user == null || NOT_REVALIDATED.contains(request.getMethod() + " " + request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        Result<AuthenticatedCaretaker> current =
            dispatcher.ask(new GetAuthenticatedCaretakerQuery(CaretakerId.of(user.id())));

        if (current.isFailure()) {
            // A sessao sobreviveu ao responsavel: e encerrada, e nao usada.
            sessionAuthenticator.invalidate(request);
            refuse(request, response, current.error().code(), current.error().message());
            return;
        }

        AuthenticatedUser now = new AuthenticatedUser(
            user.id(),
            current.value().fullName(),
            current.value().role().name(),
            current.value().mustChangePassword());
        if (!now.equals(user)) {
            sessionAuthenticator.refresh(now, request, response);
        }
        chain.doFilter(request, response);
    }

    private void refuse(HttpServletRequest request, HttpServletResponse response, String code, String detail)
        throws IOException {
        ProblemDetail problem = ProblemResponses.of(
            HttpStatus.UNAUTHORIZED, code, "Não autenticado", detail, URI.create(request.getRequestURI()));

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
