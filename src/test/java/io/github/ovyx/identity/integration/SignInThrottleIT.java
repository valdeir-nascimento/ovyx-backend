package io.github.ovyx.identity.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.github.ovyx.CsrfHandshake;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.domain.port.SignInThrottle;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contencao de tentativas de acesso contra PostgreSQL real, com o tempo controlado (T048, FR-023,
 * cenario V-13).
 *
 * <p>A janela e o bloqueio dependem da passagem do tempo, e so o relogio do teste os exercita sem
 * esperar quinze minutos. Por isso este contexto troca o relogio do sistema por um {@link FixedClock}:
 * o mesmo relogio vale para o adaptador de contencao e para o tratador de entrada.
 *
 * <p>Cada teste usa identificador e origem proprios, porque o relogio so anda para a frente e e
 * compartilhado por todos os testes desta classe.
 */
@AutoConfigureMockMvc
@DisplayName("Sign-in throttle")
class SignInThrottleIT extends IntegrationTestSupport {

    private static final String PASSWORD = "GranjaNorte2026";
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration BLOCK = Duration.ofMinutes(15);

    @TestConfiguration
    static class ControlledClock {

        @Bean
        @Primary
        FixedClock fixedClock() {
            return new FixedClock(Instant.now());
        }
    }

    @Autowired
    private FixedClock clock;

    @Autowired
    private SignInThrottle throttle;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaretakerRepository caretakerRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    private static String uniqueIdentifier() {
        return "tentativa." + UUID.randomUUID().toString().substring(0, 8) + "@ovyx.com.br";
    }

    private static String uniqueOrigin() {
        return "203.0.113." + UUID.randomUUID().toString().substring(0, 4);
    }

    private void fail(String identifier, String origin, int times) {
        for (int i = 0; i < times; i++) {
            throttle.registerFailure(identifier, origin);
        }
    }

    private MockHttpServletResponse signIn(String identifier, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/sign-in")
                        .with(CsrfHandshake.using(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"identifier": "%s", "password": "%s"}
                                 """.formatted(identifier, password)))
                .andReturn()
                .getResponse();
    }

    @Test
    @DisplayName("blocks on the fifth failure within the window, not before")
    void blocksOnTheFifthFailure() {
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();

        fail(identifier, origin, 4);
        assertThat(throttle.isBlocked(identifier, origin)).as("quatro falhas ainda sao toleradas").isFalse();

        fail(identifier, origin, 1);
        assertThat(throttle.isBlocked(identifier, origin)).isTrue();
    }

    @Test
    @DisplayName("failures spread across the fifteen-minute window still add up")
    void failuresSpreadAcrossTheWindowAddUp() {
        // Os demais testes registram as falhas no mesmo instante; este prova que a janela tem
        // mesmo quinze minutos, e nao menos.
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();
        fail(identifier, origin, 4);

        clock.advance(WINDOW.minusSeconds(1));
        fail(identifier, origin, 1);

        assertThat(throttle.isBlocked(identifier, origin)).isTrue();
    }

    @Test
    @DisplayName("the block lasts fifteen minutes and then lifts on its own")
    void blockLastsFifteenMinutes() {
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();
        fail(identifier, origin, 5);

        clock.advance(BLOCK.minusSeconds(1));
        assertThat(throttle.isBlocked(identifier, origin)).as("um segundo antes do fim").isTrue();

        clock.advance(Duration.ofSeconds(2));
        assertThat(throttle.isBlocked(identifier, origin)).as("um segundo depois do fim").isFalse();
    }

    @Test
    @DisplayName("failures older than the window are forgotten and a new window starts")
    void failuresOutsideTheWindowStartOver() {
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();
        fail(identifier, origin, 4);

        clock.advance(WINDOW.plusSeconds(1));
        fail(identifier, origin, 4);
        assertThat(throttle.isBlocked(identifier, origin))
                .as("as quatro falhas antigas nao somam com as quatro novas")
                .isFalse();

        fail(identifier, origin, 1);
        assertThat(throttle.isBlocked(identifier, origin)).as("cinco falhas na janela nova").isTrue();
    }

    @Test
    @DisplayName("counts each identifier and origin pair on its own")
    void countsEachPairOnItsOwn() {
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();
        fail(identifier, origin, 5);

        assertThat(throttle.isBlocked(identifier, origin)).isTrue();
        assertThat(throttle.isBlocked(identifier, uniqueOrigin())).isFalse();
        assertThat(throttle.isBlocked(uniqueIdentifier(), origin)).isFalse();
    }

    @Test
    @DisplayName("clearing the pair resets its count")
    void clearingResetsTheCount() {
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();
        fail(identifier, origin, 4);

        throttle.clear(identifier, origin);
        fail(identifier, origin, 4);

        assertThat(throttle.isBlocked(identifier, origin)).isFalse();
    }

    @Test
    @DisplayName("the sixth attempt, even with the right password, gets the generic answer until the block ends")
    void sixthAttemptLooksLikeAnyFailureUntilTheBlockEnds() throws Exception {
        // V-13 de ponta a ponta: nada na resposta anuncia o bloqueio, e ele termina sozinho.
        Caretaker caretaker =
                TestCaretakers.register(caretakerRepository, passwordHasher, clock, Role.USER, PASSWORD);
        String email = caretaker.email().value();

        MockHttpServletResponse wrong = null;
        for (int attempt = 1; attempt <= 5; attempt++) {
            wrong = signIn(email, "SenhaErrada2026");
        }
        MockHttpServletResponse sixth = signIn(email, PASSWORD);

        assertThat(sixth.getStatus()).isEqualTo(401);
        assertThat(sixth.getContentAsString()).isEqualTo(wrong.getContentAsString());
        assertThat(sixth.getContentType()).isEqualTo(wrong.getContentType());

        clock.advance(BLOCK.plusSeconds(1));

        assertThat(signIn(email, PASSWORD).getStatus()).as("o bloqueio terminou").isEqualTo(200);
    }
}
