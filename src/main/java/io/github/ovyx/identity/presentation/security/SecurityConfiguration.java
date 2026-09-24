package io.github.ovyx.identity.presentation.security;

import io.github.ovyx.identity.domain.model.Role;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    /** Perfil que administra responsaveis; o nome vem de {@code Role}, sem o prefixo ROLE_. */
    private static final String ADMINISTRATOR = Role.ADMINISTRATOR.name();

    private final ProblemAuthenticationEntryPoint authenticationEntryPoint;
    private final ProblemAccessDeniedHandler accessDeniedHandler;
    private final PasswordChangeRequiredFilter passwordChangeRequiredFilter;

    public SecurityConfiguration(
        ProblemAuthenticationEntryPoint authenticationEntryPoint,
        ProblemAccessDeniedHandler accessDeniedHandler,
        PasswordChangeRequiredFilter passwordChangeRequiredFilter
    ) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.passwordChangeRequiredFilter = passwordChangeRequiredFilter;
    }

    /**
     * Repositorio do token CSRF: cookie {@code XSRF-TOKEN} legivel pelo cliente, como no
     * {@code csrf().spa()}.
     *
     * <p>E bean, e nao so configuracao da cadeia, porque o {@link SessionAuthenticator} precisa do
     * mesmo repositorio para trocar o token no login.
     */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    /**
     * Porta de gerenciamento: libera health, info e a documentacao, e nega todo o resto.
     *
     * <p>A lista e explicita, e nao "todo endpoint exposto". Com a liberacao generica, bastava
     * alguem acrescentar {@code beans} ou {@code env} a {@code management.endpoints.web.exposure}
     * para publica-lo sem autenticacao; agora expor nao basta, e preciso liberar aqui tambem.
     */
    @Bean
    @Order(1)
    SecurityFilterChain managementSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeHttpRequests(requests -> requests
                .requestMatchers(EndpointRequest.to("health", "info", "openapi", "swagger-ui"))
                .permitAll()
                .anyRequest()
                .denyAll())
            .csrf(AbstractHttpConfigurer::disable)
            .requestCache(cache -> cache.requestCache(new NullRequestCache()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, SessionRevalidationFilter sessionRevalidationFilter)
        throws Exception {
        return http.authorizeHttpRequests(requests -> requests
                // Despacho interno de erro: sem esta liberacao, todo 500 era reenviado a /error,
                // barrado por falta de autenticacao e entregue ao cliente como 401.
                .dispatcherTypeMatchers(DispatcherType.ERROR)
                .permitAll()
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
                .hasRole(ADMINISTRATOR)
                .requestMatchers(HttpMethod.GET, "/api/v1/caretakers")
                .hasRole(ADMINISTRATOR)
                .requestMatchers(HttpMethod.GET, "/api/v1/caretakers/*")
                .hasRole(ADMINISTRATOR)
                .requestMatchers(HttpMethod.PUT, "/api/v1/caretakers/*")
                .hasRole(ADMINISTRATOR)
                .requestMatchers(HttpMethod.POST, "/api/v1/caretakers/*/deactivation")
                .hasRole(ADMINISTRATOR)
                // Negacao por omissao para todos, inclusive autenticados (FR-012). Com
                // authenticated() aqui, uma rota administrativa esquecida nasceria aberta a
                // qualquer usuario comum.
                .anyRequest()
                .denyAll())
            // Padrao do Spring Security para SPA: cookie XSRF-TOKEN legivel pelo cliente,
            // emitido ja na primeira resposta, e devolvido no cabecalho X-XSRF-TOKEN. Somado a
            // SameSite=Lax, fecha o vetor de CSRF sem travar o primeiro login.
            .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository()))
            // O spa() nao emite o token antecipadamente: sem este filtro, o cookie so aparecia
            // na primeira escrita, que por isso mesmo era recusada.
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
            // Sem cache de requisicao: o padrao guardava na sessao cada requisicao anonima
            // recusada, para repeti-la depois de um login por formulario que esta API nao tem.
            // O efeito era uma sessao gravada no banco a cada 401 (A2). O NullRequestCache e
            // explicito porque desligar o configurador faria o tratamento de excecoes voltar ao
            // cache em sessao.
            .requestCache(cache -> cache.requestCache(new NullRequestCache()))
            // A rotacao do identificador de sessao no login fica no SessionAuthenticator: a
            // configuracao de session fixation daqui so vale para os filtros de login do proprio
            // Spring Security, que esta API nao usa.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            // A API e consumida por um cliente proprio; nao ha formulario nem Basic.
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            // Antes da autorizacao: ela precisa decidir pela situacao e pelo perfil de agora, e nao
            // pelos gravados na sessao no login (FR-005, FR-008).
            .addFilterBefore(sessionRevalidationFilter, AuthorizationFilter.class)
            // Depois da autorizacao: so faz sentido cobrar a troca de senha de quem ja passou por
            // ela. Antes, o filtro nem teria identidade para inspecionar.
            .addFilterAfter(passwordChangeRequiredFilter, AuthorizationFilter.class)
            .build();
    }

    /** Pelo mesmo motivo do filtro de troca de senha: ele ja entra na cadeia de seguranca. */
    @Bean
    FilterRegistrationBean<SessionRevalidationFilter> sessionRevalidationFilterRegistration(
        SessionRevalidationFilter filter) {
        FilterRegistrationBean<SessionRevalidationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Impede que o Spring Boot registre o filtro tambem no servlet.
     *
     * <p>Um {@code Filter} exposto como bean e registrado automaticamente na cadeia do servlet. Como
     * este ja entra na cadeia de seguranca, sem esta desativacao ele rodaria duas vezes por
     * requisicao — a primeira antes da autenticacao, sem identidade para inspecionar.
     */
    @Bean
    FilterRegistrationBean<PasswordChangeRequiredFilter> passwordChangeRequiredFilterRegistration(
        PasswordChangeRequiredFilter filter) {
        FilterRegistrationBean<PasswordChangeRequiredFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
