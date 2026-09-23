package io.github.ovyx.identity.integration;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ovyx.CsrfHandshake;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Ciclo de vida da sessao no servidor, contra PostgreSQL real (T050, FR-003, FR-004).
 *
 * <p>A inatividade e simulada recuando o ultimo acesso da sessao na tabela do Spring Session, em vez
 * de esperar: assim o teste exercita o tempo limite configurado de verdade — os 30 minutos de
 * {@code application.yml} —, e nao um valor reduzido so para o teste.
 */
@AutoConfigureMockMvc
@DisplayName("Session lifecycle")
class SessionLifecycleIT extends IntegrationTestSupport {

    private static final String PASSWORD = "GranjaNorte2026";
    private static final Duration TIMEOUT = Duration.ofMinutes(30);

    private static final String COUNT_SESSION = """
            SELECT count(*)
              FROM spring_session
             WHERE session_id = ?
            """;

    private static final String LAST_ACCESS = """
            SELECT last_access_time
              FROM spring_session
             WHERE session_id = ?
            """;

    private static final String MAX_INACTIVE = """
            SELECT max_inactive_interval
              FROM spring_session
             WHERE session_id = ?
            """;

    private static final String MOVE_LAST_ACCESS = """
            UPDATE spring_session
               SET last_access_time = ?,
                   expiry_time = ? + max_inactive_interval * 1000
             WHERE session_id = ?
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

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
    }

    private Cookie[] signIn() throws Exception {
        Caretaker caretaker = aUniqueCaretaker()
                .withPassword(PASSWORD)
                .withHasher(passwordHasher)
                .withClock(clock)
                .build();
        caretakerRepository.save(caretaker);
        return mockMvc.perform(post("/api/v1/auth/sign-in")
                        .with(CsrfHandshake.using(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"identifier": "%s", "password": "%s"}
                                 """.formatted(caretaker.email().value(), PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }

    /** O cookie {@code SESSION} leva o identificador da sessao em base64. */
    private static String sessionId(Cookie[] cookies) {
        Cookie session = Arrays.stream(cookies)
                .filter(cookie -> "SESSION".equals(cookie.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("a resposta não trouxe o cookie SESSION"));
        return new String(Base64.getDecoder().decode(session.getValue()), StandardCharsets.UTF_8);
    }

    private long sessionRows(String sessionId) {
        Long rows = jdbc.queryForObject(COUNT_SESSION, Long.class, sessionId);
        return rows == null ? 0 : rows;
    }

    private long lastAccess(String sessionId) {
        Long lastAccess = jdbc.queryForObject(LAST_ACCESS, Long.class, sessionId);
        return lastAccess == null ? 0 : lastAccess;
    }

    /** Faz de conta que a sessao nao e usada ha {@code idle}. */
    private void idleFor(String sessionId, Duration idle) {
        long lastAccess = Instant.now().minus(idle).toEpochMilli();
        jdbc.update(MOVE_LAST_ACCESS, lastAccess, lastAccess, sessionId);
    }

    @Test
    @DisplayName("the session times out after thirty minutes of inactivity")
    void givenNewSession_whenReadingItsTimeout_thenFindThirtyMinutes() throws Exception {
        // given
        String sessionId = sessionId(signIn());

        // when
        Integer maxInactive = jdbc.queryForObject(MAX_INACTIVE, Integer.class, sessionId);

        // then
        assertThat(maxInactive).isEqualTo((int) TIMEOUT.toSeconds());
    }

    @Test
    @DisplayName("a session used within the timeout stays valid and its countdown restarts")
    void givenSessionIdleForAlmostTheTimeout_whenUsingIt_thenAcceptAndRestartTheCountdown() throws Exception {
        // given
        Cookie[] cookies = signIn();
        String sessionId = sessionId(cookies);
        idleFor(sessionId, TIMEOUT.minusMinutes(1));
        long beforeRequest = Instant.now().toEpochMilli();

        // when
        ResultActions response = mockMvc.perform(get("/api/v1/auth/me").cookie(cookies));

        // then
        response.andExpect(status().isOk());
        assertThat(lastAccess(sessionId))
                .as("a inatividade e contada a partir do ultimo uso, e nao da entrada")
                .isGreaterThanOrEqualTo(beforeRequest);
    }

    @Test
    @DisplayName("a session idle beyond the timeout is refused as expired and removed from the server")
    void givenSessionIdleBeyondTheTimeout_whenUsingIt_thenRefuseAsExpiredAndRemoveIt() throws Exception {
        // given
        // V-03.
        Cookie[] cookies = signIn();
        String sessionId = sessionId(cookies);
        idleFor(sessionId, TIMEOUT.plusMinutes(1));

        // when
        ResultActions response = mockMvc.perform(get("/api/v1/auth/me").cookie(cookies));

        // then
        response.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.detail").value("Sua sessão expirou. Entre novamente para continuar."));
        assertThat(sessionRows(sessionId)).isZero();
    }

    @Test
    @DisplayName("signing out removes the session from the server and refuses the old cookie at once")
    void givenOpenSession_whenSigningOut_thenRemoveItAndRefuseTheOldCookie() throws Exception {
        // given
        // V-04, olhando tambem o servidor: a sessao deixa de existir, e nao so de ser aceita.
        Cookie[] cookies = signIn();
        String sessionId = sessionId(cookies);
        assertThat(sessionRows(sessionId)).as("precondition: the session exists").isEqualTo(1);

        // when
        ResultActions response = mockMvc.perform(
                post("/api/v1/auth/sign-out").with(CsrfHandshake.using(mockMvc)).cookie(cookies));

        // then
        response.andExpect(status().isNoContent());
        assertThat(sessionRows(sessionId)).isZero();
        mockMvc.perform(get("/api/v1/auth/me").cookie(cookies)).andExpect(status().isUnauthorized());
    }
}
