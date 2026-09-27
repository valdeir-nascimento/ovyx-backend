package io.github.ovyx.identity.presentation.security;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ovyx.CsrfHandshake;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.presentation.RouteAuthorization;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Cada contexto declara as próprias rotas, e a negação por omissão continua num lugar só (R-008).
 *
 * <p>Um {@link RouteAuthorization} de teste declara uma rota que nenhum controller atende. Passar pela
 * autorização aparece como 404 do roteamento; ser barrado, como 401 ou 403. A rota vizinha, que
 * ninguém declarou, precisa continuar negada até para quem está autenticado.
 */
@AutoConfigureMockMvc
@Import(RouteAuthorizationIT.ProbeRoutes.class)
@DisplayName("Route authorization declared by each context")
class RouteAuthorizationIT extends IntegrationTestSupport {

    private static final String PASSWORD = "GranjaNorte2026";
    private static final String DECLARED = "/api/v1/probe";
    private static final String UNDECLARED = "/api/v1/probe-undeclared";

    /** Um contexto qualquer, com uma única rota de consulta liberada a quem está autenticado. */
    @TestConfiguration
    static class ProbeRoutes {

        @Bean
        RouteAuthorization probeRouteAuthorization() {
            return routes -> routes.requestMatchers(HttpMethod.GET, DECLARED).authenticated();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaretakerRepository caretakerRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private Clock clock;

    private Cookie[] signInAsUser() throws Exception {
        Caretaker user = aUniqueCaretaker()
                .withRole(Role.USER)
                .withPassword(PASSWORD)
                .withHasher(passwordHasher)
                .withClock(clock)
                .build();
        caretakerRepository.save(user);
        return mockMvc.perform(post("/api/v1/auth/sign-in")
                        .with(CsrfHandshake.using(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"identifier": "%s", "password": "%s"}
                                 """.formatted(user.email().value(), PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }

    @Test
    @DisplayName("a route declared by a context still requires a session")
    void givenAnonymousVisitor_whenCallingARouteDeclaredByAContext_thenRequireAuthentication() throws Exception {
        // given
        String declared = DECLARED;

        // when
        ResultActions response = mockMvc.perform(get(declared));

        // then
        response.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("a route declared by a context lets an authenticated caretaker through")
    void givenAuthenticatedCaretaker_whenCallingARouteDeclaredByAContext_thenPassTheAuthorization() throws Exception {
        // given
        // Nenhum controller atende a rota: o 404 do roteamento só aparece depois de a autorização
        // deixar passar.
        Cookie[] cookies = signInAsUser();

        // when
        ResultActions response = mockMvc.perform(get(DECLARED).cookie(cookies));

        // then
        response.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a route no context declared stays forbidden to an authenticated caretaker")
    void givenAuthenticatedCaretaker_whenCallingARouteNoContextDeclared_thenForbid() throws Exception {
        // given
        Cookie[] cookies = signInAsUser();

        // when
        ResultActions response = mockMvc.perform(get(UNDECLARED).cookie(cookies));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
