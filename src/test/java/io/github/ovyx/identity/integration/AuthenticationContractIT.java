package io.github.ovyx.identity.integration;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ovyx.CsrfHandshake;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import jakarta.servlet.http.Cookie;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Contrato dos endpoints de acesso, contra a aplicacao em execucao e PostgreSQL real.
 *
 * <p>Cobre os cenarios V-01, V-02, V-04, V-05, V-12, V-13 e V-14 do quickstart, e cada defeito que
 * os portoes de qualidade encontraram na primeira rodada. Os testes que alteram senha ou situacao
 * usam um responsavel proprio, criado por {@link CaretakerTestDataBuilder#aUniqueCaretaker()}, para
 * nao depender da ordem.
 */
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("Authentication contract")
class AuthenticationContractIT extends IntegrationTestSupport {

    private static final String EMAIL = "maria.silva@ovyx.com.br";
    private static final String MOBILE = "91988887777";
    private static final String PASSWORD = "GranjaNorte2026";
    private static final String WRONG_PASSWORD = "SenhaErrada2026";
    private static final String NEW_PASSWORD = "PosturaAviario2027";
    private static final String WEB_LOGGER = "org.springframework.web";

    /**
     * Administrador semeado pelo perfil de teste (application-test.yml).
     */
    private static final String SEEDED_ADMIN_EMAIL = "admin.teste@ovyx.com.br";

    private static final String SEEDED_ADMIN_PASSWORD = "SenhaDeTesteOvyx2026";

    private static final String FAILURES_FROM_ORIGIN = """
        SELECT failure_count
          FROM sign_in_attempt
         WHERE attempted_identifier = ?
           AND origin = ?
        """;

    private static final String ATTEMPTS_FROM_LOCALHOST = """
        SELECT count(*)
          FROM sign_in_attempt
         WHERE attempted_identifier = ?
           AND origin = '127.0.0.1'
        """;

    private static final String AUDITED_ORIGINS_TOO_LONG = """
        SELECT count(*)
          FROM access_event
         WHERE length(origin) > 45
        """;

    private static final String SESSIONS = """
        SELECT count(*)
          FROM spring_session
        """;

    private static final String GRANTED_FOR_IDENTIFIER = """
        SELECT count(*)
          FROM access_event
         WHERE outcome = 'GRANTED'
           AND attempted_identifier = ?
        """;

    private static final String EVENTS_FOR_IDENTIFIER = """
        SELECT count(*)
          FROM access_event
         WHERE attempted_identifier = ?
        """;

    private static final String INVALID_CREDENTIALS_FOR_IDENTIFIER = """
        SELECT count(*)
          FROM access_event
         WHERE outcome = 'INVALID_CREDENTIALS'
           AND attempted_identifier = ?
        """;

    private static final String AUDIT_ROWS_CONTAINING = """
        SELECT count(*)
          FROM access_event
         WHERE attempted_identifier LIKE ?
            OR origin LIKE ?
        """;

    private static final String HASHES_CONTAINING = """
        SELECT count(*)
          FROM caretaker
         WHERE password_hash LIKE ?
        """;

    private static final String INVALID_CREDENTIALS_FOR_CARETAKER = """
        SELECT count(*)
          FROM access_event
         WHERE outcome = 'INVALID_CREDENTIALS'
           AND caretaker_id = ?
        """;

    private static final String SIGN_OUTS_BY_EMAIL = """
        SELECT count(*)
          FROM access_event
         WHERE outcome = 'SIGNED_OUT'
           AND attempted_identifier = ?
           AND caretaker_id = ?
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaretakerRepository caretakerRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private Clock clock;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LoggingSystem loggingSystem;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        // O banco e compartilhado: a contagem de falhas de um teste bloquearia o seguinte sem motivo.
        jdbc.update("DELETE FROM sign_in_attempt");

        // Maria e a mesma em todas as classes; so e cadastrada na primeira vez.
        if (caretakerRepository.findByEmailOrMobilePhone(EMAIL).isEmpty()) {
            caretakerRepository.save(aCaretaker()
                .withEmail(EMAIL)
                .withMobilePhone(MOBILE)
                .withPassword(PASSWORD)
                .withHasher(passwordHasher)
                .withClock(clock)
                .build());
        }
    }

    @AfterEach
    void restoreWebLogLevel() {
        loggingSystem.setLogLevel(WEB_LOGGER, null);
    }

    // ---------------------------------------------------------------------------------- apoio

    /**
     * Handshake CSRF real; ver {@link CsrfHandshake} sobre por que nao se usa o atalho do spring-security-test.
     */
    private RequestPostProcessor csrf() {
        return CsrfHandshake.using(mockMvc);
    }

    private static String credentials(String identifier, String password) {
        return """
            {"identifier": "%s", "password": "%s"}
            """.formatted(identifier, password);
    }

    private static String passwordChange(String current, String updated) {
        return """
            {"currentPassword": "%s", "newPassword": "%s"}
            """.formatted(current, updated);
    }

    private MockHttpServletRequestBuilder signInRequest(String identifier, String password) {
        return post("/api/v1/auth/sign-in")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(credentials(identifier, password));
    }

    private MockHttpServletRequestBuilder passwordChangeRequest(Cookie[] cookies, String current, String updated) {
        return put("/api/v1/me/password")
            .with(csrf())
            .cookie(cookies)
            .contentType(MediaType.APPLICATION_JSON)
            .content(passwordChange(current, updated));
    }

    private MvcResult signIn(String identifier, String password) throws Exception {
        return mockMvc.perform(signInRequest(identifier, password)).andReturn();
    }

    private int signInStatus(String identifier, String password) throws Exception {
        return signIn(identifier, password).getResponse().getStatus();
    }

    /**
     * Entra e devolve os cookies. Com o Spring Session JDBC, a sessao vive na tabela e e resolvida
     * pelo cookie {@code SESSION} — e o cookie, e nao um objeto de sessao do teste, que a carrega.
     */
    private Cookie[] signInAndKeepSession(String identifier, String password) throws Exception {
        return mockMvc.perform(signInRequest(identifier, password))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getCookies();
    }

    private static Cookie sessionCookie(Cookie[] cookies) {
        return Arrays.stream(cookies)
            .filter(cookie -> "SESSION".equals(cookie.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("a resposta não trouxe o cookie SESSION"));
    }

    /**
     * Responsavel que nenhum outro teste usa, gravado com o hasher e o relogio da aplicacao.
     */
    private CaretakerTestDataBuilder aCaretakerForThisDatabase() {
        return aUniqueCaretaker().withPassword(PASSWORD).withHasher(passwordHasher).withClock(clock);
    }

    private Caretaker saved(CaretakerTestDataBuilder builder) {
        Caretaker caretaker = builder.build();
        caretakerRepository.save(caretaker);
        return caretaker;
    }

    private void deactivate(Caretaker caretaker) {
        caretaker.deactivate(caretakerRepository, clock);
        caretakerRepository.save(caretaker);
    }

    private void reactivate(Caretaker caretaker) {
        caretaker.reactivate(caretakerRepository, clock);
        caretakerRepository.save(caretaker);
    }

    private long count(String sql, Object... args) {
        Long result = jdbc.queryForObject(sql, Long.class, args);
        return result == null ? 0 : result;
    }

    /**
     * Cinco senhas erradas contra Maria, cada uma com um X-Forwarded-For diferente.
     */
    private void failFiveTimesWithForgedOrigins() throws Exception {
        for (int attempt = 1; attempt <= 5; attempt++) {
            mockMvc.perform(signInRequest(EMAIL, "Errada2026" + attempt).header("X-Forwarded-For", "198.51.100." + attempt));
        }
    }

    // ------------------------------------------------------------------------------- entrada

    @Test
    @DisplayName("signs in by email and returns the authenticated identity without any credential")
    void givenCorrectEmailAndPassword_whenSigningIn_thenReturnTheIdentityWithoutAnyCredential() throws Exception {
        // given — Maria foi cadastrada no setUp

        // when
        ResultActions response = mockMvc.perform(signInRequest(EMAIL, PASSWORD));

        // then
        response.andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("Maria Silva"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.mustChangePassword").value(false))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {MOBILE, "(91) 98888-7777"})
    @DisplayName("signs in by mobile phone, plain or formatted")
    void givenMobilePhoneInAnySpelling_whenSigningIn_thenAccept(String mobilePhone) throws Exception {
        // given — celular vindo do @ValueSource

        // when
        int status = signInStatus(mobilePhone, PASSWORD);

        // then
        assertThat(status).isEqualTo(200);
    }

    @Test
    @DisplayName("does not reach an account through an identifier with extra characters")
    void givenMobilePhoneWithAStrayLetter_whenSigningInWithTheRightPassword_thenRefuse() throws Exception {
        // given
        // Defeito confirmado em execucao: "x11999999999" com a senha certa entrava na conta do
        // administrador, com uma chave de contencao diferente da conta real.
        String garbled = "x" + MOBILE;

        // when
        int status = signInStatus(garbled, PASSWORD);

        // then
        assertThat(status).isEqualTo(401);
    }

    @Test
    @DisplayName("every credential failure produces a byte-identical response")
    void givenThreeKindsOfCredentialFailure_whenSigningIn_thenAnswerByteIdenticalBodies() throws Exception {
        // given
        // FR-002 e SC-002, verificados na borda HTTP, onde o vazamento aconteceria.
        Caretaker inactive = saved(aCaretakerForThisDatabase());
        deactivate(inactive);

        // when
        String unknown = signIn("nao.existe@ovyx.com.br", PASSWORD).getResponse().getContentAsString();
        String wrongPassword = signIn(EMAIL, WRONG_PASSWORD).getResponse().getContentAsString();
        String inactiveCaretaker = signIn(inactive.email().value(), PASSWORD).getResponse().getContentAsString();

        // then
        assertThat(wrongPassword).isEqualTo(unknown);
        assertThat(inactiveCaretaker).isEqualTo(unknown);
        assertThat(unknown).contains("E-mail, celular ou senha inválidos.").doesNotContain("\"errors\"");
    }

    @Test
    @DisplayName("responds 401 as problem+json on invalid credentials")
    void givenWrongPassword_whenSigningIn_thenAnswer401WithTheGenericMessage() throws Exception {
        // given — Maria foi cadastrada no setUp

        // when
        ResultActions response = mockMvc.perform(signInRequest(EMAIL, WRONG_PASSWORD));

        // then
        response.andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
            // O titulo descreve a natureza da falha; o code carrega a regra violada.
            .andExpect(jsonPath("$.title").value("Não autenticado"))
            .andExpect(jsonPath("$.detail").value("E-mail, celular ou senha inválidos."));
    }

    @Test
    @DisplayName("responds 400 with every missing field at once")
    void givenBothFieldsBlank_whenSigningIn_thenAnswer400WithBothFields() throws Exception {
        // given
        String blank = "";

        // when
        ResultActions response = mockMvc.perform(signInRequest(blank, blank));

        // then
        response.andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.details.identifier").exists())
            .andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    @DisplayName("responds 400, never 500, to an identifier longer than the audit column")
    void givenIdentifierLongerThanTheAuditColumn_whenSigningIn_thenAnswer400() throws Exception {
        // given
        // Sem limite, 255 caracteres estouravam a coluna da auditoria, a tentativa nao era contada e
        // a resposta virava um erro distinguivel das demais.
        String oversized = "a".repeat(255);

        // when
        ResultActions response = mockMvc.perform(signInRequest(oversized, PASSWORD));

        // then
        response.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.identifier").exists());
    }

    @Test
    @DisplayName("answers an unsupported media type in the project problem format, in Portuguese")
    void givenPlainTextBody_whenSigningIn_thenAnswer415InTheProjectFormat() throws Exception {
        // given
        // Antes, o corpo do proprio framework passava adiante: type about:blank e texto em ingles.
        MockHttpServletRequestBuilder plainText = post("/api/v1/auth/sign-in")
            .with(csrf())
            .contentType(MediaType.TEXT_PLAIN)
            .content("maria.silva@ovyx.com.br");

        // when
        ResultActions response = mockMvc.perform(plainText);

        // then
        response.andExpect(status().isUnsupportedMediaType())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.code").value("REQUEST_NOT_ACCEPTABLE"))
            .andExpect(jsonPath("$.title").value("Requisição não suportada"))
            .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    @DisplayName("refuses an unavailable response format before checking the credential")
    void givenValidCredentialsAskingForXml_whenSigningIn_thenAnswer406WithoutOpeningASession() throws Exception {
        // given
        // A recusa vinha depois de o tratador rodar: o cliente recebia 406 com um cookie de sessao
        // valido, a auditoria registrava a entrada como concedida e a contencao era zerada. A falha
        // anterior e o que torna o zerar visivel.
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        String email = caretaker.email().value();
        mockMvc.perform(signInRequest(email, WRONG_PASSWORD));

        // when
        MvcResult response = mockMvc.perform(signInRequest(email, PASSWORD).accept(MediaType.APPLICATION_XML))
                .andReturn();

        // then
        assertThat(response.getResponse().getStatus()).isEqualTo(406);
        assertThat(response.getResponse().getCookie("SESSION")).as("session cookie").isNull();
        assertThat(count(EVENTS_FOR_IDENTIFIER, email)).as("only the earlier failure is audited").isEqualTo(1);
        assertThat(count(FAILURES_FROM_ORIGIN, email, "127.0.0.1")).as("contention kept").isEqualTo(1);
    }

    @Test
    @DisplayName("refuses an unavailable response format when asking who is authenticated")
    void givenOpenSessionAskingForXml_whenAskingWhoIsAuthenticated_thenAnswer406() throws Exception {
        // given
        Cookie[] cookies = signInAndKeepSession(EMAIL, PASSWORD);

        // when
        ResultActions response =
                mockMvc.perform(get("/api/v1/auth/me").cookie(cookies).accept(MediaType.APPLICATION_XML));

        // then
        response.andExpect(status().isNotAcceptable());
    }

    @Test
    @DisplayName("refuses to label the identity as an error body when only problem+json is accepted")
    void givenOpenSessionAcceptingOnlyProblemJson_whenAskingWhoIsAuthenticated_thenAnswer406() throws Exception {
        // given
        // Sem o produces na rota, o corpo de sucesso saia rotulado como problem+json, o formato dos
        // erros: o cliente leria a identidade como se fosse uma recusa.
        Cookie[] cookies = signInAndKeepSession(EMAIL, PASSWORD);

        // when
        ResultActions response =
                mockMvc.perform(get("/api/v1/auth/me").cookie(cookies).accept(MediaType.APPLICATION_PROBLEM_JSON));

        // then
        response.andExpect(status().isNotAcceptable());
    }

    @Test
    @DisplayName("responds 400 to a malformed body, as the contract states")
    void givenMalformedJson_whenSigningIn_thenAnswer400() throws Exception {
        // given
        MockHttpServletRequestBuilder malformed = post("/api/v1/auth/sign-in")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{");

        // when
        ResultActions response = mockMvc.perform(malformed);

        // then
        response.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_NOT_ACCEPTABLE"));
    }

    // ---------------------------------------------------------------------------------- CSRF

    @Test
    @DisplayName("refuses a state-changing request without CSRF token as CSRF_TOKEN_INVALID, not FORBIDDEN")
    void givenSignInWithoutCsrfToken_whenSubmitting_thenRefuseWithItsOwnCode() throws Exception {
        // given
        // Antes, esta resposta era "Acesso negado", e o cliente mandava o usuario para a tela de
        // permissao ja na primeira tentativa de entrar.
        MockHttpServletRequestBuilder withoutToken = post("/api/v1/auth/sign-in")
            .contentType(MediaType.APPLICATION_JSON)
            .content(credentials(EMAIL, PASSWORD));

        // when
        ResultActions response = mockMvc.perform(withoutToken);

        // then
        response.andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"))
            // A borda preenche o caminho, como o controller: o corpo de erro e um so.
            .andExpect(jsonPath("$.instance").value("/api/v1/auth/sign-in"));
    }

    @Test
    @DisplayName("refuses signing out without CSRF token, keeping the session")
    void givenOpenSession_whenSigningOutWithoutCsrfToken_thenRefuseAndKeepTheSession() throws Exception {
        // given
        // O sign-in ja tinha esta prova; as duas escritas autenticadas nao tinham (A13).
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);

        // when
        ResultActions response = mockMvc.perform(post("/api/v1/auth/sign-out").cookie(cookies));

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("refuses changing the password without CSRF token, keeping the password")
    void givenOpenSession_whenChangingThePasswordWithoutCsrfToken_thenRefuseAndKeepThePassword() throws Exception {
        // given
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);
        MockHttpServletRequestBuilder withoutToken = put("/api/v1/me/password")
            .cookie(cookies)
            .contentType(MediaType.APPLICATION_JSON)
            .content(passwordChange(PASSWORD, NEW_PASSWORD));

        // when
        ResultActions response = mockMvc.perform(withoutToken);

        // then
        response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
        assertThat(signInStatus(caretaker.email().value(), PASSWORD)).isEqualTo(200);
    }

    @Test
    @DisplayName("signing in replaces the CSRF token, and the new one is accepted right away")
    void givenTokenObtainedBeforeSigningIn_whenSigningIn_thenIssueANewTokenThatIsAcceptedRightAway()
        throws Exception {
        // given
        // Mesmo cuidado que o Spring Security tem no fluxo padrao: um token obtido antes do login —
        // possivelmente plantado por outro — nao atravessa a autenticacao.
        Cookie before = mockMvc.perform(get("/api/v1/auth/me")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(before).as("precondition: a token before signing in").isNotNull();

        // when
        MvcResult signedIn = mockMvc.perform(post("/api/v1/auth/sign-in")
                .cookie(before)
                .header("X-XSRF-TOKEN", before.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials(EMAIL, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();

        // then
        List<Cookie> issued = Arrays.stream(signedIn.getResponse().getCookies())
            .filter(cookie -> "XSRF-TOKEN".equals(cookie.getName()))
            .toList();
        assertThat(issued).as("o login emite um token novo").hasSize(1);
        Cookie after = issued.getFirst();
        assertThat(after.getValue()).isNotBlank().isNotEqualTo(before.getValue());
        Cookie session = signedIn.getResponse().getCookie("SESSION");
        mockMvc.perform(post("/api/v1/auth/sign-out").cookie(session, after).header("X-XSRF-TOKEN", after.getValue()))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("signs in through the real CSRF handshake a browser client performs")
    void givenTokenFromTheFirstAnonymousResponse_whenSigningIn_thenAccept() throws Exception {
        // given
        // Nenhum .with(csrf()) aqui: e o fluxo que a SPA executa de verdade. Qualquer resposta —
        // inclusive o 401 da consulta de identidade — ja entrega o cookie XSRF-TOKEN.
        MvcResult probe = mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized()).andReturn();
        Cookie xsrf = probe.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf)
            .as(
                "o cookie XSRF-TOKEN precisa chegar antes da primeira escrita; Set-Cookie recebidos: %s",
                probe.getResponse().getHeaders("Set-Cookie"))
            .isNotNull();

        // when
        ResultActions response = mockMvc.perform(post("/api/v1/auth/sign-in")
            .cookie(xsrf)
            .header("X-XSRF-TOKEN", xsrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(credentials(EMAIL, PASSWORD)));

        // then
        response.andExpect(status().isOk());
    }

    // ------------------------------------------------------------ origem e contencao (V-13)

    @Test
    @DisplayName("a forged X-Forwarded-For does not buy extra attempts")
    void givenFiveFailuresWithForgedOrigins_whenSigningInWithTheRightPassword_thenRefuseAndCountThemAll()
        throws Exception {
        // given
        // Defeito confirmado em execucao: cada valor novo do cabecalho abria uma chave de contencao
        // nova, e as tentativas contra a mesma conta ficavam ilimitadas.
        failFiveTimesWithForgedOrigins();

        // when
        int sixth = mockMvc.perform(signInRequest(EMAIL, PASSWORD).header("X-Forwarded-For", "198.51.100.99"))
            .andReturn()
            .getResponse()
            .getStatus();

        // then
        assertThat(sixth).as("a sexta tentativa, mesmo com a senha certa").isEqualTo(401);
        assertThat(count(FAILURES_FROM_ORIGIN, EMAIL, "127.0.0.1")).isEqualTo(5);
    }

    @Test
    @DisplayName("an oversized X-Forwarded-For does not break the counting")
    void givenOversizedForwardedFor_whenSigningInWithAWrongPassword_thenCountTheAttemptUnderTheRealOrigin()
        throws Exception {
        // given
        // Antes, 70 caracteres estouravam a coluna origin: o INSERT falhava, a tentativa nao era
        // contada nem auditada, e o 500 chegava ao cliente disfarcado de 401.
        String oversized = "a".repeat(70);

        // when
        ResultActions response =
            mockMvc.perform(signInRequest(EMAIL, WRONG_PASSWORD).header("X-Forwarded-For", oversized));

        // then
        response.andExpect(status().isUnauthorized());
        assertThat(count(ATTEMPTS_FROM_LOCALHOST, EMAIL)).isEqualTo(1);
    }

    @Test
    @DisplayName("an oversized X-Forwarded-For does not forge the audited origin")
    void givenOversizedForwardedFor_whenSigningIn_thenNeverAuditTheForgedOrigin() throws Exception {
        // given
        String oversized = "a".repeat(70);

        // when
        mockMvc.perform(signInRequest(EMAIL, WRONG_PASSWORD).header("X-Forwarded-For", oversized));

        // then
        assertThat(count(AUDITED_ORIGINS_TOO_LONG)).isZero();
    }

    // ------------------------------------------------------------------------ sessao (V-04)

    @Test
    @DisplayName("an authenticated session can query its own identity")
    void givenOpenSession_whenAskingWhoIsAuthenticated_thenReturnTheCaretaker() throws Exception {
        // given
        Cookie[] cookies = signInAndKeepSession(EMAIL, PASSWORD);

        // when
        ResultActions response = mockMvc.perform(get("/api/v1/auth/me").cookie(cookies));

        // then
        response.andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Maria Silva"));
    }

    @Test
    @DisplayName("signing in over an existing session issues a new session id and kills the old one")
    void givenExistingSession_whenSigningInAsAnotherCaretaker_thenIssueANewSessionIdAndKillTheOldOne()
        throws Exception {
        // given
        // Defesa contra session fixation: um identificador conhecido antes da autenticacao nao pode
        // continuar valendo depois dela.
        Cookie previous = sessionCookie(signInAndKeepSession(EMAIL, PASSWORD));
        Caretaker other = saved(aCaretakerForThisDatabase());

        // when
        MvcResult signedIn = mockMvc.perform(signInRequest(other.email().value(), PASSWORD).cookie(previous))
            .andExpect(status().isOk())
            .andReturn();

        // then
        Cookie current = signedIn.getResponse().getCookie("SESSION");
        assertThat(current).isNotNull();
        assertThat(current.getValue()).isNotEqualTo(previous.getValue());
        mockMvc.perform(get("/api/v1/auth/me").cookie(previous)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").cookie(current))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value(other.fullName().value()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/auth/me", "/api/v1/rota-que-ninguem-declarou"})
    @DisplayName("an anonymous request does not create a server-side session")
    void givenAnonymousRequest_whenRefused_thenCreateNoServerSideSession(String path) throws Exception {
        // given
        // O cache de requisicao do Spring Security guardava na sessao cada requisicao anonima
        // recusada: qualquer um enchia a tabela de sessoes sem nem tentar entrar.
        long before = count(SESSIONS);

        // when
        MvcResult response = mockMvc.perform(get(path)).andExpect(status().isUnauthorized()).andReturn();

        // then
        assertThat(response.getResponse().getCookie("SESSION")).isNull();
        assertThat(count(SESSIONS)).isLessThanOrEqualTo(before);
    }

    @Test
    @DisplayName("querying the identity without a session is unauthorized")
    void givenNoSession_whenAskingWhoIsAuthenticated_thenAnswer401() throws Exception {
        // given — nenhum cookie

        // when
        ResultActions response = mockMvc.perform(get("/api/v1/auth/me"));

        // then
        response.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("after signing out, the previous session is rejected immediately")
    void givenSessionThatWorks_whenSigningOut_thenRejectItImmediately() throws Exception {
        // given
        // FR-004. Com JWT stateless, a ultima requisicao abaixo continuaria valendo.
        Cookie[] cookies = signInAndKeepSession(EMAIL, PASSWORD);
        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies)).andExpect(status().isOk());

        // when
        mockMvc.perform(post("/api/v1/auth/sign-out").with(csrf()).cookie(cookies)).andExpect(status().isNoContent());

        // then
        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("signing out without a session is unauthorized")
    void givenNoSession_whenSigningOut_thenAnswer401() throws Exception {
        // given — nenhum cookie

        // when
        ResultActions response = mockMvc.perform(post("/api/v1/auth/sign-out").with(csrf()));

        // then
        response.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the identity query refuses a caretaker inactivated during the session")
    void givenCaretakerInactivatedDuringTheSession_whenAskingWhoIsAuthenticated_thenRefuseAsUnavailable()
        throws Exception {
        // given
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);
        deactivate(caretaker);

        // when
        ResultActions response = mockMvc.perform(get("/api/v1/auth/me").cookie(cookies));

        // then
        response.andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("CARETAKER_UNAVAILABLE"));
    }

    @Test
    @DisplayName("the refused session stays dead even after the caretaker is reactivated")
    void givenSessionRefusedForInactivation_whenTheCaretakerIsReactivated_thenKeepTheSessionDead()
        throws Exception {
        // given
        // Reativado, o responsavel volta a poder entrar — mas a sessao antiga foi destruida, e nao
        // apenas recusada uma vez.
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);
        deactivate(caretaker);
        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies)).andExpect(status().isUnauthorized());

        // when
        reactivate(caretaker);

        // then
        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies)).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------- troca de senha (V-12)

    @Test
    @DisplayName("changes the own password, after which only the new one works")
    void givenOpenSession_whenChangingThePassword_thenAcceptOnlyTheNewOne() throws Exception {
        // given
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);

        // when
        ResultActions response = mockMvc.perform(passwordChangeRequest(cookies, PASSWORD, NEW_PASSWORD));

        // then
        response.andExpect(status().isNoContent());
        assertThat(signInStatus(caretaker.email().value(), PASSWORD)).isEqualTo(401);
        assertThat(signInStatus(caretaker.email().value(), NEW_PASSWORD)).isEqualTo(200);
    }

    @Test
    @DisplayName("changing the password without a session is unauthorized")
    void givenNoSession_whenChangingThePassword_thenAnswer401() throws Exception {
        // given
        MockHttpServletRequestBuilder withoutSession = put("/api/v1/me/password")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(passwordChange(PASSWORD, NEW_PASSWORD));

        // when
        ResultActions response = mockMvc.perform(withoutSession);

        // then
        response.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refuses the password change when the current password is wrong")
    void givenWrongCurrentPassword_whenChangingThePassword_thenAnswer400OnTheCurrentPassword() throws Exception {
        // given
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);

        // when
        ResultActions response = mockMvc.perform(passwordChangeRequest(cookies, WRONG_PASSWORD, NEW_PASSWORD));

        // then
        response.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.currentPassword").exists());
    }

    @Test
    @DisplayName("reports every password policy violation at once")
    void givenNewPasswordOutsidePolicy_whenChangingThePassword_thenAnswer400OnTheNewPassword() throws Exception {
        // given
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);

        // when
        ResultActions response = mockMvc.perform(passwordChangeRequest(cookies, PASSWORD, "abc"));

        // then
        response.andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.newPassword").exists())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("reports a missing current password together with the new password violations")
    void givenMissingCurrentAndInvalidNewPassword_whenChangingThePassword_thenReportBothInOneRound()
        throws Exception {
        // given
        // Antes, a borda cobrava a senha atual e o dominio a politica da nova, em duas rodadas (A1).
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);

        // when
        ResultActions response = mockMvc.perform(passwordChangeRequest(cookies, "", "abc"));

        // then
        response.andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.currentPassword").exists())
            .andExpect(jsonPath("$.details.newPassword").exists())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("an inactivated caretaker cannot change the password through an old session")
    void givenCaretakerInactivatedDuringTheSession_whenChangingThePassword_thenRefuseAsUnavailable()
        throws Exception {
        // given
        // Mesma regra do /me: a sessao que sobreviveu a inativacao e encerrada, e nao usada (A12).
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);
        deactivate(caretaker);

        // when
        ResultActions response = mockMvc.perform(passwordChangeRequest(cookies, PASSWORD, NEW_PASSWORD));

        // then
        response.andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("CARETAKER_UNAVAILABLE"));
    }

    @Test
    @DisplayName("a password change refused for inactivation leaves the old password and ends the session")
    void givenPasswordChangeRefusedForInactivation_whenTheCaretakerIsReactivated_thenKeepTheOldPasswordOnly()
        throws Exception {
        // given
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);
        deactivate(caretaker);
        mockMvc.perform(passwordChangeRequest(cookies, PASSWORD, NEW_PASSWORD)).andExpect(status().isUnauthorized());

        // when
        reactivate(caretaker);

        // then
        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies)).andExpect(status().isUnauthorized());
        assertThat(signInStatus(caretaker.email().value(), PASSWORD))
            .as("a senha anterior continua valendo")
            .isEqualTo(200);
    }

    // ------------------------------------------------- administrador semeado (V-05, FR-025)

    @Test
    @DisplayName("a pending password change does not block signing in again")
    void givenSessionWithPendingPasswordChange_whenSigningInAsAnotherCaretaker_thenAccept() throws Exception {
        // given
        // Antes, o bloqueio de troca pendente barrava tambem o sign-in: quem tinha uma sessao nessa
        // situacao nao conseguia nem entrar com outra conta no mesmo navegador (A6).
        Caretaker pending = saved(aCaretakerForThisDatabase().withPendingPasswordChange());
        Cookie[] cookies = signInAndKeepSession(pending.email().value(), PASSWORD);

        // when
        ResultActions response = mockMvc.perform(signInRequest(EMAIL, PASSWORD).cookie(cookies));

        // then
        response.andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Maria Silva"));
    }

    @Test
    @DisplayName("the seeded administrator must change the password and is released in the same session")
    void givenSeededAdministratorOwingThePasswordChange_whenChangingIt_thenReleaseTheSameSession()
        throws Exception {
        // given
        // Um teste so, de proposito: a troca e de mao unica e o administrador semeado e um so no
        // banco compartilhado. Dividir em dois criaria dependencia de ordem entre eles.
        MvcResult signedIn = mockMvc.perform(signInRequest(SEEDED_ADMIN_EMAIL, SEEDED_ADMIN_PASSWORD))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMINISTRATOR"))
            .andExpect(jsonPath("$.mustChangePassword").value(true))
            .andReturn();
        Cookie[] cookies = signedIn.getResponse().getCookies();

        // when
        ResultActions response =
            mockMvc.perform(passwordChangeRequest(cookies, SEEDED_ADMIN_PASSWORD, NEW_PASSWORD));

        // then
        response.andExpect(status().isNoContent());
        // A mesma sessao segue valendo e ja nao carrega a obrigacao: a troca nao exige novo login.
        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    // -------------------------------------------------------------- auditoria e log (V-14)

    @Test
    @DisplayName("the audit trail records the typed identifier of granted and refused attempts")
    void givenOneGrantedAndOneRefusedAttempt_whenReadingTheAuditTrail_thenFindBothUnderTheTypedIdentifier()
        throws Exception {
        // given
        signIn(EMAIL, PASSWORD);
        signIn(EMAIL, WRONG_PASSWORD);

        // when
        long granted = count(GRANTED_FOR_IDENTIFIER, EMAIL);
        long refused = count(INVALID_CREDENTIALS_FOR_IDENTIFIER, EMAIL);

        // then
        assertThat(granted).isPositive();
        assertThat(refused).isPositive();
    }

    @Test
    @DisplayName("neither the audit trail nor the caretaker table ever holds the raw password")
    void givenGrantedAndRefusedAttempts_whenSearchingThePersistedData_thenNeverFindTheRawPassword()
        throws Exception {
        // given
        signIn(EMAIL, PASSWORD);
        signIn(EMAIL, WRONG_PASSWORD);
        String containingThePassword = "%" + PASSWORD + "%";

        // when
        long inAudit = count(AUDIT_ROWS_CONTAINING, containingThePassword, containingThePassword);
        long inHashes = count(HASHES_CONTAINING, containingThePassword);

        // then
        assertThat(inAudit).isZero();
        assertThat(inHashes).isZero();
    }

    @Test
    @DisplayName("the audit trail links a failed attempt to the targeted account")
    void givenWrongPasswordForAnExistingAccount_whenReadingTheAuditTrail_thenLinkTheFailureToThatAccount()
        throws Exception {
        // given
        // Sem o responsavel nas falhas, nao dava para investigar um ataque dirigido a uma conta.
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        signIn(caretaker.email().value(), WRONG_PASSWORD);

        // when
        long linked = count(INVALID_CREDENTIALS_FOR_CARETAKER, caretaker.id().value());

        // then
        assertThat(linked).isEqualTo(1);
    }

    @Test
    @DisplayName("the audit trail records the sign-out by email, like every other event")
    void givenSignOut_whenReadingTheAuditTrail_thenRecordItUnderTheEmail() throws Exception {
        // given
        // O encerramento guardava o UUID onde todos os outros eventos guardam e-mail ou celular.
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        String email = caretaker.email().value();
        Cookie[] cookies = signInAndKeepSession(email, PASSWORD);
        mockMvc.perform(post("/api/v1/auth/sign-out").with(csrf()).cookie(cookies)).andExpect(status().isNoContent());

        // when
        long signOuts = count(SIGN_OUTS_BY_EMAIL, email, caretaker.id().value());

        // then
        assertThat(signOuts).isEqualTo(1);
    }

    @Test
    @DisplayName("the password never reaches the log, even with web DEBUG logging enabled")
    void givenWebDebugLogging_whenSendingEveryKindOfPasswordBody_thenNeverLogThePassword(CapturedOutput output)
        throws Exception {
        // given
        // O Spring MVC registra o corpo desserializado em DEBUG. Com o toString gerado para records,
        // ligar DEBUG para investigar um incidente gravava a senha no log. Depois, as senhas longas
        // demais apareciam como "valor rejeitado" da validacao de tamanho (N1). O nivel volta ao
        // normal em restoreWebLogLevel, mesmo se o teste falhar.
        String oversizedSignIn = "SenhaLonga" + "x".repeat(200) + "2026";
        String oversizedCurrent = "AtualLonga" + "y".repeat(200) + "2026";
        String oversizedNew = "NovaLonga" + "z".repeat(200) + "2027";
        Caretaker caretaker = saved(aCaretakerForThisDatabase());
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);
        loggingSystem.setLogLevel(WEB_LOGGER, LogLevel.DEBUG);

        // when
        signIn(EMAIL, PASSWORD);
        signIn(EMAIL, WRONG_PASSWORD);
        int oversizedStatus = signInStatus(EMAIL, oversizedSignIn);
        mockMvc.perform(passwordChangeRequest(cookies, oversizedCurrent, oversizedNew))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.newPassword").exists());
        // Senha sem aspas: o JSON e invalido, e a mensagem de erro do parser trazia o valor, que o
        // Spring MVC registra em DEBUG (B1 da terceira revisao).
        mockMvc.perform(post("/api/v1/auth/sign-in")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"identifier": "%s", "password": SemAspasEntrada2026}
                    """.formatted(EMAIL)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("REQUEST_NOT_ACCEPTABLE"));
        mockMvc.perform(put("/api/v1/me/password")
                .with(csrf())
                .cookie(cookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword": SemAspasAtual2026, "newPassword": "x"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("REQUEST_NOT_ACCEPTABLE"));

        // then
        assertThat(oversizedStatus).as("senha longa demais e so uma senha errada").isEqualTo(401);
        assertThat(output.getAll())
            .as("o corpo foi de fato registrado, entao a mascara foi exercitada")
            .contains("password=****");
        assertThat(output.getAll())
            .as("o erro de leitura foi de fato registrado, entao a redacao foi exercitada")
            .contains("(linha 1, coluna");
        assertThat(output.getAll())
            .doesNotContain(PASSWORD)
            .doesNotContain(WRONG_PASSWORD)
            .doesNotContain(oversizedSignIn)
            .doesNotContain(oversizedCurrent)
            .doesNotContain(oversizedNew)
            .doesNotContain("SemAspasEntrada")
            .doesNotContain("SemAspasAtual");
    }
}
