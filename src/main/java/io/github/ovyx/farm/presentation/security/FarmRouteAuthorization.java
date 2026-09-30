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
 * Administrador, para setores, gaiolas e formulas de racao (R-010 da 004). Uma rota por vez, com o metodo:
 * o que nao esta aqui cai na negacao por omissao, ate para o administrador — nada e apagado, e o
 * {@code DELETE} nem existe.
 */
@Component
public class FarmRouteAuthorization implements RouteAuthorization {

    private static final String SECTORS = "/api/v1/sectors";
    private static final String SECTOR = SECTORS + "/*";
    private static final String CAGES = SECTOR + "/cages";
    private static final String CAGE = CAGES + "/*";
    private static final String FORMULAS = "/api/v1/feed-formulas";
    private static final String FORMULA = FORMULAS + "/*";
    private static final String WEIGHINGS = CAGE + "/weighings";
    private static final String WEIGHING = WEIGHINGS + "/*";
    private static final String DEACTIVATION = "/deactivation";
    private static final String REACTIVATION = "/reactivation";

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
            .requestMatchers(HttpMethod.POST, SECTOR + DEACTIVATION)
            .hasRole(AccessRoles.ADMINISTRATOR)
            .requestMatchers(HttpMethod.POST, SECTOR + REACTIVATION)
            .hasRole(AccessRoles.ADMINISTRATOR)
            // Gaiolas (US2)
            .requestMatchers(HttpMethod.GET, CAGES)
            .authenticated()
            // A planilha das gaiolas (US3 da 007): leitura, de qualquer responsavel, antes da regra da gaiola.
            .requestMatchers(HttpMethod.GET, CAGES + "/export")
            .authenticated()
            .requestMatchers(HttpMethod.GET, CAGE)
            .authenticated()
            .requestMatchers(HttpMethod.POST, CAGES)
            .hasRole(AccessRoles.ADMINISTRATOR)
            .requestMatchers(HttpMethod.PUT, CAGE)
            .hasRole(AccessRoles.ADMINISTRATOR)
            // Inativacao e reativacao de gaiola (US3)
            .requestMatchers(HttpMethod.POST, CAGE + DEACTIVATION)
            .hasRole(AccessRoles.ADMINISTRATOR)
            .requestMatchers(HttpMethod.POST, CAGE + REACTIVATION)
            .hasRole(AccessRoles.ADMINISTRATOR)
            // Formulas de racao (US1 da 004)
            .requestMatchers(HttpMethod.GET, FORMULAS)
            .authenticated()
            .requestMatchers(HttpMethod.GET, FORMULA)
            .authenticated()
            .requestMatchers(HttpMethod.POST, FORMULAS)
            .hasRole(AccessRoles.ADMINISTRATOR)
            .requestMatchers(HttpMethod.PUT, FORMULA)
            .hasRole(AccessRoles.ADMINISTRATOR)
            .requestMatchers(HttpMethod.POST, FORMULA + DEACTIVATION)
            .hasRole(AccessRoles.ADMINISTRATOR)
            .requestMatchers(HttpMethod.POST, FORMULA + REACTIVATION)
            .hasRole(AccessRoles.ADMINISTRATOR)
            // Pesagens (feature 005): as primeiras escritas do farm de qualquer responsavel (R-010 da 005)
            .requestMatchers(HttpMethod.GET, WEIGHINGS)
            .authenticated()
            .requestMatchers(HttpMethod.POST, WEIGHINGS)
            .authenticated()
            .requestMatchers(HttpMethod.GET, WEIGHING)
            .authenticated()
            .requestMatchers(HttpMethod.PUT, WEIGHING)
            .authenticated()
            .requestMatchers(HttpMethod.POST, WEIGHING + "/voiding")
            .authenticated();
    }
}
