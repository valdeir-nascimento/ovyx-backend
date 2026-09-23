package io.github.ovyx.identity.integration;

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
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;

import java.time.Clock;

import jakarta.servlet.http.Cookie;

import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Contrato dos endpoints de acesso, contra a aplicacao em execucao e PostgreSQL real.
 *
 * <p>Cobre os cenarios V-01, V-02, V-04, V-05, V-12, V-13 e V-14 do quickstart, e cada defeito que
 * os portoes de qualidade encontraram na primeira rodada. Os testes que alteram senha ou situacao
 * usam um responsavel proprio, criado por {@link TestCaretakers}, para nao depender da ordem.
 */
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("Authentication contract")
class AuthenticationContractIT extends IntegrationTestSupport {

    private static final String EMAIL = "maria.silva@ovyx.com.br";
    private static final String MOBILE = "91988887777";
    private static final String PASSWORD = "GranjaNorte2026";
    private static final String NEW_PASSWORD = "PosturaAviario2027";

    /**
     * Administrador semeado pelo perfil de teste (application-test.yml).
     */
    private static final String SEEDED_ADMIN_EMAIL = "admin.teste@ovyx.com.br";
    private static final String SEEDED_ADMIN_PASSWORD = "SenhaDeTesteOvyx2026";

    @Autowired
    private MockMvc mockMvc;

    /**
     * Handshake CSRF real; ver {@link CsrfHandshake} sobre por que nao se usa o atalho do spring-security-test.
     */
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
    private DataSource dataSource;

    @Autowired
    private LoggingSystem loggingSystem;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        // O banco e compartilhado: a contagem de falhas de um teste bloquearia o seguinte sem motivo.
        jdbc.update("delete from sign_in_attempt");

        if (caretakerRepository.findByEmailOrMobilePhone(EMAIL).isEmpty()) {
            caretakerRepository.save(Caretaker.register("Maria Silva", "52998224725", EMAIL, MOBILE, PASSWORD, Role.USER, false, passwordHasher, clock));
        }
    }

    // ---------------------------------------------------------------------------------- apoio

    private static String credentials(String identifier, String password) {
        return """
            {"identifier": "%s", "password": "%s"}
            """.formatted(identifier, password);
    }

    private MvcResult signIn(String identifier, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/sign-in").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials(identifier, password))).andReturn();
    }

    /**
     * Entra e devolve os cookies. Com o Spring Session JDBC, a sessao vive na tabela e e resolvida
     * pelo cookie {@code SESSION} — e o cookie, e nao um objeto de sessao do teste, que a carrega.
     */
    private Cookie[] signInAndKeepSession(String identifier, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/sign-in").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials(identifier, password))).andExpect(status().isOk()).andReturn().getResponse().getCookies();
    }

    private static Cookie sessionCookie(Cookie[] cookies) {
        return Arrays.stream(cookies).filter(cookie -> "SESSION".equals(cookie.getName())).findFirst().orElseThrow(() -> new IllegalStateException("a resposta não trouxe o cookie SESSION"));
    }

    private Caretaker newCaretaker() {
        return TestCaretakers.register(caretakerRepository, passwordHasher, clock, Role.USER, PASSWORD);
    }

    private long count(String sql, Object... args) {
        Long result = jdbc.queryForObject(sql, Long.class, args);
        return result == null ? 0 : result;
    }

    // ------------------------------------------------------------------------------- entrada

    @Test
    @DisplayName("signs in by email and returns the authenticated identity without any credential")
    void signsInByEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-in").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials(EMAIL, PASSWORD))).andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Maria Silva")).andExpect(jsonPath("$.role").value("USER")).andExpect(jsonPath("$.mustChangePassword").value(false)).andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("signs in by mobile phone, plain or formatted")
    void signsInByMobilePhone() throws Exception {
        assertThat(signIn(MOBILE, PASSWORD).getResponse().getStatus()).isEqualTo(200);
        assertThat(signIn("(91) 98888-7777", PASSWORD).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("does not reach an account through an identifier with extra characters")
    void rejectsIdentifierWithExtraCharacters() throws Exception {
        // Defeito confirmado em execucao: "x11999999999" com a senha certa entrava na conta do
        // administrador, com uma chave de contencao diferente da conta real.
        assertThat(signIn("x" + MOBILE, PASSWORD).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("every credential failure produces a byte-identical response")
    void everyCredentialFailureLooksTheSame() throws Exception {
        // FR-002 e SC-002, verificados na borda HTTP, onde o vazamento aconteceria.
        Caretaker inactive = newCaretaker();
        inactive.deactivate(clock);
        caretakerRepository.save(inactive);

        String unknown = signIn("nao.existe@ovyx.com.br", PASSWORD).getResponse().getContentAsString();
        String wrongPassword = signIn(EMAIL, "SenhaErrada2026").getResponse().getContentAsString();
        String inactiveCaretaker = signIn(inactive.email().value(), PASSWORD).getResponse().getContentAsString();

        assertThat(wrongPassword).isEqualTo(unknown);
        assertThat(inactiveCaretaker).isEqualTo(unknown);
        assertThat(unknown).contains("E-mail, celular ou senha inválidos.").doesNotContain("\"errors\"");
    }

    @Test
    @DisplayName("responds 401 as problem+json on invalid credentials")
    void invalidCredentialsRespondWith401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-in").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials(EMAIL, "SenhaErrada2026"))).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
            // O titulo descreve a natureza da falha; o code carrega a regra violada.
            .andExpect(jsonPath("$.title").value("Não autenticado")).andExpect(jsonPath("$.detail").value("E-mail, celular ou senha inválidos."));
    }

    @Test
    @DisplayName("responds 400 with every missing field at once")
    void missingFieldsRespondWith400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-in").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials("", ""))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED")).andExpect(jsonPath("$.details.identifier").exists()).andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    @DisplayName("responds 400, never 500, to an identifier longer than the audit column")
    void oversizedIdentifierRespondsWith400() throws Exception {
        // Sem limite, 255 caracteres estouravam a coluna da auditoria, a tentativa nao era contada e
        // a resposta virava um erro distinguivel das demais.
        mockMvc.perform(post("/api/v1/auth/sign-in").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials("a".repeat(255), PASSWORD))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.identifier").exists());
    }

    @Test
    @DisplayName("answers an unsupported media type in the project problem format, in Portuguese")
    void unsupportedMediaTypeAnswersInProjectFormat() throws Exception {
        // Antes, o corpo do proprio framework passava adiante: type about:blank e texto em ingles.
        mockMvc.perform(post("/api/v1/auth/sign-in").with(csrf()).contentType(MediaType.TEXT_PLAIN).content("maria.silva@ovyx.com.br")).andExpect(status().isUnsupportedMediaType()).andExpect(content().contentTypeCompatibleWith("application/problem+json")).andExpect(jsonPath("$.code").value("REQUEST_NOT_ACCEPTABLE")).andExpect(jsonPath("$.title").value("Requisição não suportada")).andExpect(jsonPath("$.status").value(415));
    }

    @Test
    @DisplayName("responds 400 to a malformed body, as the contract states")
    void malformedBodyRespondsWith400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-in").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_NOT_ACCEPTABLE"));
    }

    // ---------------------------------------------------------------------------------- CSRF

    @Test
    @DisplayName("refuses a state-changing request without CSRF token as CSRF_TOKEN_INVALID, not FORBIDDEN")
    void refusesMissingCsrfTokenWithItsOwnProblemType() throws Exception {
        // Antes, esta resposta era "Acesso negado", e o cliente mandava o usuario para a tela de
        // permissao ja na primeira tentativa de entrar.
        mockMvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON).content(credentials(EMAIL, PASSWORD))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"))
            // A borda preenche o caminho, como o controller: o corpo de erro e um so.
            .andExpect(jsonPath("$.instance").value("/api/v1/auth/sign-in"));
    }

    @Test
    @DisplayName("refuses signing out and changing the password without CSRF token, keeping the session")
    void refusesAuthenticatedWritesWithoutCsrfToken() throws Exception {
        // O sign-in ja tinha esta prova; as duas escritas autenticadas nao tinham (A13).
        Caretaker caretaker = newCaretaker();
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);

        mockMvc.perform(post("/api/v1/auth/sign-out").cookie(cookies)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
        mockMvc.perform(put("/api/v1/me/password").cookie(cookies).contentType(MediaType.APPLICATION_JSON).content(passwordChange(PASSWORD, NEW_PASSWORD))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));

        // As recusas nao encerraram a sessao nem trocaram a senha.
        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies)).andExpect(status().isOk());
        assertThat(signIn(caretaker.email().value(), PASSWORD).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("signing in replaces the CSRF token, and the new one is accepted right away")
    void signingInReplacesTheCsrfToken() throws Exception {
        // Mesmo cuidado que o Spring Security tem no fluxo padrao: um token obtido antes do login —
        // possivelmente plantado por outro — nao atravessa a autenticacao.
        Cookie before = mockMvc.perform(get("/api/v1/auth/me")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(before).isNotNull();

        MvcResult signedIn = mockMvc.perform(post("/api/v1/auth/sign-in").cookie(before).header("X-XSRF-TOKEN", before.getValue()).contentType(MediaType.APPLICATION_JSON).content(credentials(EMAIL, PASSWORD))).andExpect(status().isOk()).andReturn();

        List<Cookie> issued = Arrays.stream(signedIn.getResponse().getCookies()).filter(cookie -> "XSRF-TOKEN".equals(cookie.getName())).toList();
        assertThat(issued).as("o login emite um token novo").hasSize(1);
        Cookie after = issued.getFirst();
        assertThat(after.getValue()).isNotBlank().isNotEqualTo(before.getValue());

        Cookie session = signedIn.getResponse().getCookie("SESSION");
        mockMvc.perform(post("/api/v1/auth/sign-out").cookie(session, after).header("X-XSRF-TOKEN", after.getValue())).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("signs in through the real CSRF handshake a browser client performs")
    void signsInThroughTheRealCsrfHandshake() throws Exception {
        // Nenhum .with(csrf()) aqui: e o fluxo que a SPA executa de verdade. Qualquer resposta —
        // inclusive o 401 da consulta de identidade — ja entrega o cookie XSRF-TOKEN.
        MvcResult probe = mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized()).andReturn();
        Cookie xsrf = probe.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).as("o cookie XSRF-TOKEN precisa chegar antes da primeira escrita; Set-Cookie recebidos: %s", probe.getResponse().getHeaders("Set-Cookie")).isNotNull();

        mockMvc.perform(post("/api/v1/auth/sign-in").cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue()).contentType(MediaType.APPLICATION_JSON).content(credentials(EMAIL, PASSWORD))).andExpect(status().isOk());
    }

    // ------------------------------------------------------------ origem e contencao (V-13)

    @Test
    @DisplayName("a forged X-Forwarded-For does not buy extra attempts")
    void forgedForwardedForDoesNotBypassThrottling() throws Exception {
        // Defeito confirmado em execucao: cada valor novo do cabecalho abria uma chave de contencao
        // nova, e as tentativas contra a mesma conta ficavam ilimitadas.
        for (int attempt = 1; attempt <= 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/sign-in").with(csrf()).header("X-Forwarded-For", "198.51.100." + attempt).contentType(MediaType.APPLICATION_JSON).content(credentials(EMAIL, "Errada2026" + attempt)));
        }

        MvcResult sixth = mockMvc.perform(post("/api/v1/auth/sign-in").with(csrf()).header("X-Forwarded-For", "198.51.100.99").contentType(MediaType.APPLICATION_JSON).content(credentials(EMAIL, PASSWORD))).andReturn();

        assertThat(sixth.getResponse().getStatus()).as("a sexta tentativa, mesmo com a senha certa").isEqualTo(401);
        assertThat(count("select failure_count from sign_in_attempt where attempted_identifier = ? and origin = ?", EMAIL, "127.0.0.1")).isEqualTo(5);
    }

    @Test
    @DisplayName("an oversized X-Forwarded-For neither breaks counting nor forges the audited origin")
    void oversizedForwardedForIsIgnored() throws Exception {
        // Antes, 70 caracteres estouravam a coluna origin: o INSERT falhava, a tentativa nao era
        // contada nem auditada, e o 500 chegava ao cliente disfarcado de 401.
        mockMvc.perform(post("/api/v1/auth/sign-in").with(csrf()).header("X-Forwarded-For", "a".repeat(70)).contentType(MediaType.APPLICATION_JSON).content(credentials(EMAIL, "SenhaErrada2026"))).andExpect(status().isUnauthorized());

        assertThat(count("select count(*) from sign_in_attempt where attempted_identifier = ? and origin = '127.0.0.1'", EMAIL)).isEqualTo(1);
        assertThat(count("select count(*) from access_event where length(origin) > 45")).isZero();
    }

    // ------------------------------------------------------------------------ sessao (V-04)

    @Test
    @DisplayName("an authenticated session can query its own identity")
    void authenticatedSessionCanQueryItsOwnIdentity() throws Exception {
        Cookie[] cookies = signInAndKeepSession(EMAIL, PASSWORD);

        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies)).andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Maria Silva"));
    }

    @Test
    @DisplayName("signing in over an existing session issues a new session id and kills the old one")
    void signingInRotatesTheSessionId() throws Exception {
        // Defesa contra session fixation: um identificador conhecido antes da autenticacao nao pode
        // continuar valendo depois dela.
        Cookie previous = sessionCookie(signInAndKeepSession(EMAIL, PASSWORD));
        Caretaker other = newCaretaker();

        MvcResult signedIn = mockMvc.perform(post("/api/v1/auth/sign-in").with(csrf()).cookie(previous).contentType(MediaType.APPLICATION_JSON).content(credentials(other.email().value(), PASSWORD))).andExpect(status().isOk()).andReturn();
        Cookie current = signedIn.getResponse().getCookie("SESSION");

        assertThat(current).isNotNull();
        assertThat(current.getValue()).isNotEqualTo(previous.getValue());
        mockMvc.perform(get("/api/v1/auth/me").cookie(previous)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").cookie(current)).andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value(other.fullName().value()));
    }

    @Test
    @DisplayName("an anonymous request does not create a server-side session")
    void anonymousRequestDoesNotCreateASession() throws Exception {
        // O cache de requisicao do Spring Security guardava na sessao cada requisicao anonima
        // recusada: qualquer um enchia a tabela de sessoes sem nem tentar entrar.
        long before = count("select count(*) from spring_session");

        MvcResult me = mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized()).andReturn();
        MvcResult undeclared = mockMvc.perform(get("/api/v1/rota-que-ninguem-declarou")).andExpect(status().isUnauthorized()).andReturn();

        assertThat(me.getResponse().getCookie("SESSION")).isNull();
        assertThat(undeclared.getResponse().getCookie("SESSION")).isNull();
        assertThat(count("select count(*) from spring_session")).isLessThanOrEqualTo(before);
    }

    @Test
    @DisplayName("querying the identity without a session is unauthorized")
    void withoutSessionIdentityQueryIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("after signing out, the previous session is rejected immediately")
    void afterSignOutThePreviousSessionIsRejected() throws Exception {
        // FR-004. Com JWT stateless, a ultima requisicao abaixo continuaria valendo.
        Cookie[] cookies = signInAndKeepSession(EMAIL, PASSWORD);

        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/sign-out").with(csrf()).cookie(cookies)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("signing out without a session is unauthorized")
    void signOutWithoutSessionIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-out").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the identity query ends the session of a caretaker inactivated meanwhile")
    void identityQueryEndsSessionOfInactivatedCaretaker() throws Exception {
        Caretaker caretaker = newCaretaker();
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);

        caretaker.deactivate(clock);
        caretakerRepository.save(caretaker);

        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("CARETAKER_UNAVAILABLE"));

        // Reativado, o responsavel volta a poder entrar — mas a sessao antiga foi destruida, e nao
        // apenas recusada uma vez.
        caretaker.reactivate(clock);
        caretakerRepository.save(caretaker);

        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies)).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------- troca de senha (V-12)

    private String passwordChange(String current, String updated) {
        return """
            {"currentPassword": "%s", "newPassword": "%s"}
            """.formatted(current, updated);
    }

    @Test
    @DisplayName("changes the own password, after which only the new one works")
    void changesOwnPassword() throws Exception {
        Caretaker caretaker = newCaretaker();
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);

        mockMvc.perform(put("/api/v1/me/password").with(csrf()).cookie(cookies).contentType(MediaType.APPLICATION_JSON).content(passwordChange(PASSWORD, NEW_PASSWORD))).andExpect(status().isNoContent());

        assertThat(signIn(caretaker.email().value(), PASSWORD).getResponse().getStatus()).isEqualTo(401);
        assertThat(signIn(caretaker.email().value(), NEW_PASSWORD).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("changing the password without a session is unauthorized")
    void passwordChangeWithoutSessionIsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/me/password").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(passwordChange(PASSWORD, NEW_PASSWORD))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refuses the password change when the current password is wrong")
    void passwordChangeWithWrongCurrentPassword() throws Exception {
        Caretaker caretaker = newCaretaker();
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);

        mockMvc.perform(put("/api/v1/me/password").with(csrf()).cookie(cookies).contentType(MediaType.APPLICATION_JSON).content(passwordChange("SenhaErrada2026", NEW_PASSWORD))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.currentPassword").exists());
    }

    @Test
    @DisplayName("reports every password policy violation at once")
    void passwordChangeOutsidePolicy() throws Exception {
        Caretaker caretaker = newCaretaker();
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);

        mockMvc.perform(put("/api/v1/me/password").with(csrf()).cookie(cookies).contentType(MediaType.APPLICATION_JSON).content(passwordChange(PASSWORD, "abc"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.newPassword").exists()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("reports a missing current password together with the new password violations")
    void passwordChangeReportsEverythingInOneRound() throws Exception {
        // Antes, a borda cobrava a senha atual e o dominio a politica da nova, em duas rodadas (A1).
        Caretaker caretaker = newCaretaker();
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);

        mockMvc.perform(put("/api/v1/me/password").with(csrf()).cookie(cookies).contentType(MediaType.APPLICATION_JSON).content(passwordChange("", "abc"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.currentPassword").exists()).andExpect(jsonPath("$.details.newPassword").exists()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("an inactivated caretaker cannot change the password through an old session")
    void inactivatedCaretakerCannotChangePassword() throws Exception {
        // Mesma regra do /me: a sessao que sobreviveu a inativacao e encerrada, e nao usada (A12).
        Caretaker caretaker = newCaretaker();
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);

        caretaker.deactivate(clock);
        caretakerRepository.save(caretaker);

        mockMvc.perform(put("/api/v1/me/password").with(csrf()).cookie(cookies).contentType(MediaType.APPLICATION_JSON).content(passwordChange(PASSWORD, NEW_PASSWORD))).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("CARETAKER_UNAVAILABLE"));

        caretaker.reactivate(clock);
        caretakerRepository.save(caretaker);

        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies)).andExpect(status().isUnauthorized());
        assertThat(signIn(caretaker.email().value(), PASSWORD).getResponse().getStatus()).as("a senha anterior continua valendo").isEqualTo(200);
    }

    // ------------------------------------------------- administrador semeado (V-05, FR-025)

    @Test
    @DisplayName("a pending password change does not block signing in again")
    void pendingPasswordChangeDoesNotBlockSignIn() throws Exception {
        // Antes, o bloqueio de troca pendente barrava tambem o sign-in: quem tinha uma sessao nessa
        // situacao nao conseguia nem entrar com outra conta no mesmo navegador (A6).
        Caretaker pending = TestCaretakers.register(caretakerRepository, passwordHasher, clock, Role.USER, PASSWORD, true);
        Cookie[] cookies = signInAndKeepSession(pending.email().value(), PASSWORD);

        mockMvc.perform(post("/api/v1/auth/sign-in").with(csrf()).cookie(cookies).contentType(MediaType.APPLICATION_JSON).content(credentials(EMAIL, PASSWORD))).andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Maria Silva"));
    }

    @Test
    @DisplayName("the seeded administrator must change the password and is released afterwards")
    void seededAdministratorMustChangePassword() throws Exception {
        MvcResult signedIn = mockMvc.perform(post("/api/v1/auth/sign-in").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials(SEEDED_ADMIN_EMAIL, SEEDED_ADMIN_PASSWORD))).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMINISTRATOR")).andExpect(jsonPath("$.mustChangePassword").value(true)).andReturn();
        Cookie[] cookies = signedIn.getResponse().getCookies();

        mockMvc.perform(put("/api/v1/me/password").with(csrf()).cookie(cookies).contentType(MediaType.APPLICATION_JSON).content(passwordChange(SEEDED_ADMIN_PASSWORD, NEW_PASSWORD))).andExpect(status().isNoContent());

        // A mesma sessao segue valendo e ja nao carrega a obrigacao: a troca nao exige novo login.
        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies)).andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    // -------------------------------------------------------------- auditoria e log (V-14)

    @Test
    @DisplayName("the audit trail records the typed identifier and never the password")
    void auditRecordsTheIdentifierNeverThePassword() throws Exception {
        signIn(EMAIL, PASSWORD);
        signIn(EMAIL, "SenhaErrada2026");

        assertThat(count("select count(*) from access_event where outcome = 'GRANTED' and attempted_identifier = ?", EMAIL)).isPositive();
        assertThat(count("select count(*) from access_event where outcome = 'INVALID_CREDENTIALS' and attempted_identifier = ?", EMAIL)).isPositive();
        assertThat(count("select count(*) from access_event where attempted_identifier like ? or origin like ?", "%" + PASSWORD + "%", "%" + PASSWORD + "%")).isZero();
        assertThat(count("select count(*) from caretaker where password_hash like ?", "%" + PASSWORD + "%")).isZero();
    }

    @Test
    @DisplayName("the audit trail links failed attempts to the targeted account and records sign-out by email")
    void auditLinksFailuresToTheAccountAndSignOutByEmail() throws Exception {
        // Sem o responsavel nas falhas, nao dava para investigar um ataque dirigido a uma conta; e o
        // encerramento guardava o UUID onde todos os outros eventos guardam e-mail ou celular.
        Caretaker caretaker = newCaretaker();
        String email = caretaker.email().value();

        signIn(email, "SenhaErrada2026");
        Cookie[] cookies = signInAndKeepSession(email, PASSWORD);
        mockMvc.perform(post("/api/v1/auth/sign-out").with(csrf()).cookie(cookies)).andExpect(status().isNoContent());

        assertThat(count("select count(*) from access_event where outcome = 'INVALID_CREDENTIALS' and caretaker_id = ?", caretaker.id().value())).isEqualTo(1);
        assertThat(count("select count(*) from access_event where outcome = 'SIGNED_OUT' and attempted_identifier = ?" + " and caretaker_id = ?", email, caretaker.id().value())).isEqualTo(1);
    }

    @Test
    @DisplayName("the password never reaches the log, even with web DEBUG logging enabled")
    void passwordNeverReachesTheLog(CapturedOutput output) throws Exception {
        // O Spring MVC registra o corpo desserializado em DEBUG. Com o toString gerado para records,
        // ligar DEBUG para investigar um incidente gravava a senha no log. Depois, as senhas longas
        // demais apareciam como "valor rejeitado" da validacao de tamanho (N1).
        String oversizedSignIn = "SenhaLonga" + "x".repeat(200) + "2026";
        String oversizedCurrent = "AtualLonga" + "y".repeat(200) + "2026";
        String oversizedNew = "NovaLonga" + "z".repeat(200) + "2027";
        Caretaker caretaker = newCaretaker();
        Cookie[] cookies = signInAndKeepSession(caretaker.email().value(), PASSWORD);

        loggingSystem.setLogLevel("org.springframework.web", LogLevel.DEBUG);
        try {
            signIn(EMAIL, PASSWORD);
            signIn(EMAIL, "SenhaErrada2026");
            assertThat(signIn(EMAIL, oversizedSignIn).getResponse().getStatus()).as("senha longa demais e so uma senha errada").isEqualTo(401);
            mockMvc.perform(put("/api/v1/me/password").with(csrf()).cookie(cookies).contentType(MediaType.APPLICATION_JSON).content(passwordChange(oversizedCurrent, oversizedNew))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.newPassword").exists());

            // Senha sem aspas: o JSON e invalido, e a mensagem de erro do parser trazia o valor, que
            // o Spring MVC registra em DEBUG (B1 da terceira revisao).
            mockMvc.perform(post("/api/v1/auth/sign-in").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"identifier\": \"" + EMAIL + "\", \"password\": SemAspasEntrada2026}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_NOT_ACCEPTABLE"));
            mockMvc.perform(put("/api/v1/me/password").with(csrf()).cookie(cookies).contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\": SemAspasAtual2026, \"newPassword\": \"x\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUEST_NOT_ACCEPTABLE"));
        } finally {
            loggingSystem.setLogLevel("org.springframework.web", null);
        }

        assertThat(output.getAll()).as("o corpo foi de fato registrado, entao a mascara foi exercitada").contains("password=****");
        assertThat(output.getAll()).as("o erro de leitura foi de fato registrado, entao a redacao foi exercitada").contains("(linha 1, coluna");
        assertThat(output.getAll()).doesNotContain(PASSWORD).doesNotContain("SenhaErrada2026").doesNotContain(oversizedSignIn).doesNotContain(oversizedCurrent).doesNotContain(oversizedNew).doesNotContain("SemAspasEntrada").doesNotContain("SemAspasAtual");
    }
}
