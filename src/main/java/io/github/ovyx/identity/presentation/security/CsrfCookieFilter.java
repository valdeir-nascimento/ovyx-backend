package io.github.ovyx.identity.presentation.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Forca a emissao do cookie {@code XSRF-TOKEN} em toda resposta.
 *
 * <p>O token CSRF do Spring Security e diferido: so e gerado, e so vira cookie, quando alguem o
 * resolve — e numa requisicao GET ninguem resolve. Com isso, a primeira escrita de um cliente SPA
 * (o login) chegava sem token e recebia 403. O {@code csrf().spa()} do Spring Security 7.1 configura
 * o repositorio e o tratador, mas nao este passo; o proprio guia de SPA do Spring manda acrescenta-lo.
 *
 * <p>Nao e bean de proposito: instanciado direto na cadeia de seguranca, o Spring Boot nao o
 * registra tambem no servlet.
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            // Resolver o token e o que faz o repositorio grava-lo no cookie desta resposta.
            token.getToken();
        }
        chain.doFilter(request, response);
    }
}
