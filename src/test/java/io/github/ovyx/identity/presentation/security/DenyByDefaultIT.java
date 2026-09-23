package io.github.ovyx.identity.presentation.security;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ovyx.CsrfHandshake;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import java.time.Clock;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
// No Spring Boot 4 esta anotacao mudou de pacote: saiu de
// org.springframework.boot.test.autoconfigure.web.servlet para ca, em spring-boot-webmvc-test.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Comprova a negacao por omissao (FR-012).
 *
 * <p>A pergunta que este teste responde nao e "os endpoints conhecidos estao protegidos?", e sim "o
 * que acontece com uma rota que ninguem declarou?" — para quem nao esta autenticado e,
 * principalmente, para quem esta. Com {@code authenticated()} no lugar de {@code denyAll()}, uma
 * rota administrativa esquecida nasceria aberta a qualquer usuario comum.
 */
@AutoConfigureMockMvc
@DisplayName("Deny by default")
class DenyByDefaultIT extends IntegrationTestSupport {

    private static final String PASSWORD = "GranjaNorte2026";

    @Autowired
    private MockMvc mockMvc;

    /** Handshake CSRF real; ver {@link CsrfHandshake} sobre por que nao se usa o atalho do spring-security-test. */
    private RequestPostProcessor csrf() {
        return CsrfHandshake.using(mockMvc);
    }

    @Autowired
    private CaretakerRepository caretakerRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private Clock clock;

    @Autowired
    private ApplicationContext context;

    private Cookie[] signInAsUser() throws Exception {
        Caretaker user = aUniqueCaretaker()
                .withPassword(PASSWORD)
                .withHasher(passwordHasher)
                .withClock(clock)
                .build();
        caretakerRepository.save(user);
        return mockMvc.perform(post("/api/v1/auth/sign-in")
                        .with(csrf())
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
    @DisplayName("an undeclared route requires authentication instead of answering 404")
    void givenAnonymousRequest_whenCallingAnUndeclaredRoute_thenRequireAuthenticationInsteadOf404() throws Exception {
        // given
        String undeclared = "/api/v1/rota-que-ninguem-declarou";

        // when
        ResultActions response = mockMvc.perform(get(undeclared));

        // then
        response.andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    @DisplayName("an undeclared route is forbidden even to an authenticated caretaker")
    void givenAuthenticatedCaretaker_whenCallingAnUndeclaredRoute_thenForbid() throws Exception {
        // given
        Cookie[] cookies = signInAsUser();

        // when
        ResultActions response = mockMvc.perform(get("/api/v1/caretakers").cookie(cookies));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("the unauthenticated response follows Problem Details without detailing anything")
    void givenAnonymousRequest_whenRefused_thenAnswerProblemDetailsWithoutDetailingAnything() throws Exception {
        // given
        String protectedRoute = "/api/v1/caretakers";

        // when
        ResultActions response = mockMvc.perform(get(protectedRoute));

        // then
        response.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andExpect(jsonPath("$.status").value(401))
                // O caminho vem preenchido também aqui, como no que o controller devolve.
                .andExpect(jsonPath("$.instance").value("/api/v1/caretakers"))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("sign-in is not public for GET")
    void givenAnonymousGetOnTheSignInRoute_whenCalling_thenRequireAuthentication() throws Exception {
        // given
        // A excecao publica e declarada por metodo, nao por caminho.
        String signIn = "/api/v1/auth/sign-in";

        // when
        ResultActions response = mockMvc.perform(get(signIn));

        // then
        response.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("sign-in is public for POST")
    void givenAnonymousPostOnTheSignInRoute_whenCalling_thenReachTheController() throws Exception {
        // given
        // Sem corpo nem content-type, a resposta e 415 — o que importa e que a autorizacao deixou
        // passar e o controller respondeu.
        String signIn = "/api/v1/auth/sign-in";

        // when
        ResultActions response = mockMvc.perform(post(signIn).with(csrf()));

        // then
        response.andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("no in-memory user with a generated password exists")
    void givenApplicationContext_whenLookingForUserDetailsServices_thenFindNone() {
        // given
        // Sem UserDetailsService proprio, o Spring Boot criava o usuario "user" com senha gerada e a
        // imprimia no log a cada subida — uma credencial valida fora do cadastro e em texto claro
        // no log (QA V-14, FR-021).
        Class<UserDetailsService> generatedUserSource = UserDetailsService.class;

        // when
        String[] beans = context.getBeanNamesForType(generatedUserSource);

        // then
        assertThat(beans).isEmpty();
    }

    @Test
    @DisplayName("the internal error dispatch is not turned into 401")
    void givenInternalErrorDispatch_whenReachingTheErrorPage_thenKeepThe500() throws Exception {
        // given
        // Defeito encontrado em execucao: o despacho interno para /error exigia autenticacao, e todo
        // 500 do sistema chegava ao cliente como 401.
        RequestPostProcessor errorDispatch = request -> {
            request.setDispatcherType(DispatcherType.ERROR);
            request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
            request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/auth/sign-in");
            return request;
        };

        // when
        ResultActions response = mockMvc.perform(get("/error").with(errorDispatch));

        // then
        response.andExpect(status().isInternalServerError());
    }
}
