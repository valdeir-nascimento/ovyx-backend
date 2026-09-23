package io.github.ovyx.identity.presentation.security;

import io.github.ovyx.shared.presentation.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

/**
 * Estabelece e encerra a sessao autenticada.
 *
 * <p>Concentra tres cuidados que, espalhados pelos controllers, cedo ou tarde ficariam
 * inconsistentes:
 *
 * <ul>
 *   <li><strong>Rotacao do identificador de sessao no login</strong>: sem ela, um identificador
 *       obtido antes da autenticacao continuaria valido depois dela — que e como funciona o ataque
 *       de session fixation.
 *   <li><strong>Gravacao explicita do contexto</strong>: desde o Spring Security 6 o contexto nao e
 *       mais salvo automaticamente. Esquecer isso produz o sintoma classico de "o login funciona
 *       mas a proxima requisicao volta 401".
 *   <li><strong>Invalidacao no logout</strong>: a sessao e destruida, e nao apenas esvaziada, o que
 *       atende FR-004.
 * </ul>
 *
 * <p>No login, tambem troca o token CSRF, como o Spring Security faz no fluxo padrao.
 */
@Component
public class SessionAuthenticator {

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    /**
     * A mesma estrategia que a configuracao {@code sessionFixation().changeSessionId()} usaria. Ela
     * so troca o identificador quando ja existe sessao valida — que e o caso do ataque: session
     * fixation depende de a pessoa chegar ao login com um identificador plantado por outro. Sem
     * sessao, nao ha o que trocar, e {@code saveContext} cria uma nova.
     */
    private final SessionAuthenticationStrategy sessionFixationProtection = new ChangeSessionIdAuthenticationStrategy();

    private final CsrfTokenRepository csrfTokenRepository;

    public SessionAuthenticator(CsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;
    }

    /**
     * Autentica a sessao com a identidade informada, trocando o identificador e o token CSRF.
     */
    public void authenticate(AuthenticatedUser principal, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority(principal.authority())));

        sessionFixationProtection.onAuthentication(authentication, request, response);

        // Um token novo substitui, no navegador, o obtido antes do login — possivelmente plantado por
        // outro, como o identificador de sessao. O servidor nao guarda o token (fica em cookie), entao
        // uma copia do antigo feita antes do login nao e revogada; o que muda e o cookie (N-015).
        csrfTokenRepository.saveToken(csrfTokenRepository.generateToken(request), request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    /**
     * Atualiza a identidade da sessao sem trocar o identificador.
     *
     * <p>Usada apos a troca de senha, para encerrar a obrigacao de trocar sem obrigar a pessoa a
     * entrar de novo.
     */
    public void refresh(AuthenticatedUser principal, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority(principal.authority())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    /**
     * Encerra a sessao. O cookie anterior deixa de ser aceito imediatamente (FR-004).
     */
    public void invalidate(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    /**
     * Identidade autenticada da requisicao atual, ou {@code null} quando nao ha.
     */
    public static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user;
    }
}
