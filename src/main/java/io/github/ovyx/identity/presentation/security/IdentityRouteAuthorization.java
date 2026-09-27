package io.github.ovyx.identity.presentation.security;

import io.github.ovyx.shared.presentation.AccessRoles;
import io.github.ovyx.shared.presentation.RouteAuthorization;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * As rotas do contexto de identidade, uma a uma, cada uma com a sua regra (R-008 da feature 002).
 *
 * <p>Estavam na {@link SecurityConfiguration}, que aplica esta declaração, e a dos outros contextos,
 * antes da negação por omissão.
 */
@Component
public class IdentityRouteAuthorization implements RouteAuthorization {

    @Override
    public void authorize(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry routes) {
        routes
                // Rotas da Historia 1, uma a uma, cada uma com a sua regra.
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/sign-in")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/sign-out")
                .authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/me")
                .authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/me/password")
                .authenticated()
                // Rotas da Historia 2: a administracao de responsaveis e so do perfil Administrador
                // (FR-008), uma rota por vez, para que um metodo novo nasca negado.
                .requestMatchers(HttpMethod.POST, "/api/v1/caretakers")
                .hasRole(AccessRoles.ADMINISTRATOR)
                .requestMatchers(HttpMethod.GET, "/api/v1/caretakers")
                .hasRole(AccessRoles.ADMINISTRATOR)
                .requestMatchers(HttpMethod.GET, "/api/v1/caretakers/*")
                .hasRole(AccessRoles.ADMINISTRATOR)
                .requestMatchers(HttpMethod.PUT, "/api/v1/caretakers/*")
                .hasRole(AccessRoles.ADMINISTRATOR)
                .requestMatchers(HttpMethod.POST, "/api/v1/caretakers/*/deactivation")
                .hasRole(AccessRoles.ADMINISTRATOR);
    }
}
