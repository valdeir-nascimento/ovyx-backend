package io.github.ovyx.production.presentation.security;

import io.github.ovyx.shared.presentation.RouteAuthorization;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * As rotas do contexto production, uma a uma, cada uma com a sua regra (FR-019; R-008 da 002).
 *
 * <p>Qualquer responsavel autenticado abre, corrige, lanca e consulta relatorios, e ve o painel (006): toda rota e
 * {@code authenticated()}. Uma rota por vez, com o metodo: o que nao esta aqui cai na negacao por
 * omissao — nada e apagado, e o {@code DELETE} nem existe (FR-021).
 */
@Component
public class ProductionRouteAuthorization implements RouteAuthorization {

    private static final String REPORTS = "/api/v1/sectors/*/daily-reports";

    @Override
    public void authorize(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry routes) {
        routes
            // Abertura e consulta (US1)
            .requestMatchers(HttpMethod.GET, REPORTS)
            .authenticated()
            .requestMatchers(HttpMethod.GET, REPORTS + "/suggestion")
            .authenticated()
            // A planilha dos relatorios de um intervalo (US1 da 007): leitura, de qualquer responsavel.
            .requestMatchers(HttpMethod.GET, REPORTS + "/export")
            .authenticated()
            .requestMatchers(HttpMethod.POST, REPORTS)
            .authenticated()
            // Consulta e lancamento da producao (US2)
            .requestMatchers(HttpMethod.GET, REPORTS + "/*")
            .authenticated()
            // Correcao dos dados gerais (US4)
            .requestMatchers(HttpMethod.PUT, REPORTS + "/*")
            .authenticated()
            .requestMatchers(HttpMethod.GET, REPORTS + "/*/cages/*")
            .authenticated()
            .requestMatchers(HttpMethod.PUT, REPORTS + "/*/cages/*/production")
            .authenticated()
            // Lancamento e confirmacao da mortalidade (US3)
            .requestMatchers(HttpMethod.PUT, REPORTS + "/*/cages/*/mortality")
            .authenticated()
            .requestMatchers(HttpMethod.POST, REPORTS + "/*/mortality-confirmation")
            .authenticated()
            // Racao do setor pela sugestao (US2 da 004)
            .requestMatchers(HttpMethod.GET, REPORTS + "/*/feed-suggestion")
            .authenticated()
            .requestMatchers(HttpMethod.POST, REPORTS + "/*/feed")
            .authenticated()
            // Racao de uma gaiola (US3 da 004)
            .requestMatchers(HttpMethod.PUT, REPORTS + "/*/cages/*/feed")
            .authenticated()
            // Painel do Inicio (006): o cabecalho e o painel de um setor, os mesmos para os dois perfis
            .requestMatchers(HttpMethod.GET, "/api/v1/dashboard")
            .authenticated()
            // A planilha do painel (US2 da 007): leitura, de qualquer responsavel.
            .requestMatchers(HttpMethod.GET, "/api/v1/sectors/*/dashboard/export")
            .authenticated()
            .requestMatchers(HttpMethod.GET, "/api/v1/sectors/*/dashboard")
            .authenticated();
    }
}
