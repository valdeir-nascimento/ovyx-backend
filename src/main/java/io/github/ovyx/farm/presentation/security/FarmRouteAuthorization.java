package io.github.ovyx.farm.presentation.security;

import io.github.ovyx.shared.presentation.AccessRoles;
import io.github.ovyx.shared.presentation.RouteAuthorization;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * As rotas do contexto farm, uma a uma, cada uma com a sua regra (FR-018; R-008).
 *
 * <p>Consultar e de qualquer responsavel autenticado; cadastrar, editar, inativar e reativar, so do
 * Administrador. Uma rota por vez, com o metodo: o que nao esta aqui cai na negacao por omissao, ate
 * para o administrador — nada e apagado, e o {@code DELETE} nem existe.
 */
@Component
public class FarmRouteAuthorization implements RouteAuthorization {

    private static final String SECTORS = "/api/v1/sectors";
    private static final String SECTOR = SECTORS + "/*";
    private static final String CAGES = SECTOR + "/cages";
    private static final String CAGE = CAGES + "/*";

    @Override
    public void authorize(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry routes) {
        routes
            // Setores (US1)
            .requestMatchers(HttpMethod.GET, SECTORS)
            .authenticated()
            .requestMatchers(HttpMethod.GET, SECTOR)
            .authenticated()
            .requestMatchers(HttpMethod.POST, SECTORS)
            .hasRole(AccessRoles.ADMINISTRATOR)
            .requestMatchers(HttpMethod.PUT, SECTOR)
            .hasRole(AccessRoles.ADMINISTRATOR)
            // Inativacao e reativacao de setor (US3)
            .requestMatchers(HttpMethod.POST, SECTOR + "/deactivation")
            .hasRole(AccessRoles.ADMINISTRATOR)
            .requestMatchers(HttpMethod.POST, SECTOR + "/reactivation")
            .hasRole(AccessRoles.ADMINISTRATOR)
            // Gaiolas (US2)
            .requestMatchers(HttpMethod.GET, CAGES)
            .authenticated()
            .requestMatchers(HttpMethod.GET, CAGE)
            .authenticated()
            .requestMatchers(HttpMethod.POST, CAGES)
            .hasRole(AccessRoles.ADMINISTRATOR)
            .requestMatchers(HttpMethod.PUT, CAGE)
            .hasRole(AccessRoles.ADMINISTRATOR)
            // Inativacao e reativacao de gaiola (US3)
            .requestMatchers(HttpMethod.POST, CAGE + "/deactivation")
            .hasRole(AccessRoles.ADMINISTRATOR)
            .requestMatchers(HttpMethod.POST, CAGE + "/reactivation")
            .hasRole(AccessRoles.ADMINISTRATOR);
    }
}
