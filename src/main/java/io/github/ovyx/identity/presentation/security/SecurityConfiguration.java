package io.github.ovyx.identity.presentation.security;

import io.github.ovyx.shared.presentation.ApiDocsProperties;
import io.github.ovyx.shared.presentation.RouteAuthorization;
import jakarta.servlet.DispatcherType;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final ProblemAuthenticationEntryPoint authenticationEntryPoint;
    private final ProblemAccessDeniedHandler accessDeniedHandler;
    private final PasswordChangeRequiredFilter passwordChangeRequiredFilter;
    private final ApiDocsProperties apiDocs;
    private final ObjectMapper objectMapper;
    private final List<RouteAuthorization> routeAuthorizations;

    /**
     * @param routeAuthorizations as rotas de cada contexto (R-008 da feature 002). Nenhum contexto
     *     conhece as rotas de outro; esta configuracao so as aplica, antes da negacao por omissao.
     */
    public SecurityConfiguration(
        ProblemAuthenticationEntryPoint authenticationEntryPoint,
        ProblemAccessDeniedHandler accessDeniedHandler,
        PasswordChangeRequiredFilter passwordChangeRequiredFilter,
        ApiDocsProperties apiDocs,
        ObjectMapper objectMapper,
        List<RouteAuthorization> routeAuthorizations
    ) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.passwordChangeRequiredFilter = passwordChangeRequiredFilter;
        this.apiDocs = apiDocs;
        this.objectMapper = objectMapper;
        this.routeAuthorizations = List.copyOf(routeAuthorizations);
    }

    /**
     * CORS so para a interface de documentacao, que chama a API de outra origem (FR-026, FR-028).
     *
     * <p>Com credenciais — o cookie da sessao precisa ir junto —, e por isso com as origens exatas
     * da configuracao, nunca curinga: uma origem qualquer liberada seria um site alheio agindo com a
     * sessao de quem o visita. So os metodos e os cabecalhos que a API usa, e o {@code Location} do
     * cadastro exposto, para a interface mostra-lo.
     */
    private CorsConfigurationSource apiDocsCors() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(apiDocs.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Accept", "X-XSRF-TOKEN"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofMinutes(30));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
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
        http.authorizeHttpRequests(requests -> {
                // Despacho interno de erro: sem esta liberacao, todo 500 era reenviado a /error,
                // barrado por falta de autenticacao e entregue ao cliente como 401.
                requests.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                // As rotas de cada contexto, declaradas por ele mesmo (R-008 da feature 002).
                routeAuthorizations.forEach(routes -> routes.authorize(requests));
                // Negacao por omissao para todos, inclusive autenticados (FR-012). Com
                // authenticated() aqui, uma rota administrativa esquecida nasceria aberta a
                // qualquer usuario comum.
                requests.anyRequest().denyAll();
            })
            // O CORS da documentacao entra como filtro proprio, abaixo, para recusar no formato de erro
            // do contrato; o configurador do Spring Security nao aceita outro processador.
            .cors(AbstractHttpConfigurer::disable)
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
            .addFilterAfter(passwordChangeRequiredFilter, AuthorizationFilter.class);
        // Sem origem configurada, nenhum CORS: a API so fala com a propria origem. Com ela, o filtro
        // fica onde o Spring Security o poria, antes do CSRF.
        if (!apiDocs.allowedOrigins().isEmpty()) {
            CorsFilter cors = new CorsFilter(apiDocsCors());
            cors.setCorsProcessor(new ProblemCorsProcessor(objectMapper));
            http.addFilterBefore(cors, CsrfFilter.class);
        }
        return http.build();
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
