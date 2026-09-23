package io.github.ovyx.identity.integration;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aUniqueCaretaker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.github.ovyx.CsrfHandshake;
import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.domain.port.SignInThrottle;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.IntStream;
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
    private static final String WRONG_PASSWORD = "SenhaErrada2026";
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
        IntStream.range(0, times).forEach(attempt -> throttle.registerFailure(identifier, origin));
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

    /** Erra a senha cinco vezes pela API e devolve a ultima resposta, para comparar com a seguinte. */
    private MockHttpServletResponse exhaustAttempts(String email) throws Exception {
        MockHttpServletResponse last = null;
        for (int attempt = 1; attempt <= 5; attempt++) {
            last = signIn(email, WRONG_PASSWORD);
        }
        return last;
    }

    private String registeredEmail() {
        Caretaker caretaker = aUniqueCaretaker()
                .withPassword(PASSWORD)
                .withHasher(passwordHasher)
                .withClock(clock)
                .build();
        caretakerRepository.save(caretaker);
        return caretaker.email().value();
    }

    @Test
    @DisplayName("tolerates four failures within the window")
    void givenFourFailuresWithinTheWindow_whenChecking_thenDoNotBlockYet() {
        // given
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();
        fail(identifier, origin, 4);

        // when
        boolean blocked = throttle.isBlocked(identifier, origin);

        // then
        assertThat(blocked).isFalse();
    }

    @Test
    @DisplayName("blocks on the fifth failure within the window")
    void givenFiveFailuresWithinTheWindow_whenChecking_thenBlock() {
        // given
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();
        fail(identifier, origin, 5);

        // when
        boolean blocked = throttle.isBlocked(identifier, origin);

        // then
        assertThat(blocked).isTrue();
    }

    @Test
    @DisplayName("failures spread across the fifteen-minute window still add up")
    void givenFailuresSpreadAcrossTheWindow_whenChecking_thenAddThemUpAndBlock() {
        // given
        // Os demais testes registram as falhas no mesmo instante; este prova que a janela tem
        // mesmo quinze minutos, e nao menos.
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();
        fail(identifier, origin, 4);
        clock.advance(WINDOW.minusSeconds(1));
        fail(identifier, origin, 1);

        // when
        boolean blocked = throttle.isBlocked(identifier, origin);

        // then
        assertThat(blocked).isTrue();
    }

    @Test
    @DisplayName("the block still holds one second before fifteen minutes")
    void givenBlockOneSecondShortOfFifteenMinutes_whenChecking_thenStillBlock() {
        // given
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();
        fail(identifier, origin, 5);
        clock.advance(BLOCK.minusSeconds(1));

        // when
        boolean blocked = throttle.isBlocked(identifier, origin);

        // then
        assertThat(blocked).isTrue();
    }

    @Test
    @DisplayName("the block lifts on its own after fifteen minutes")
    void givenBlockOlderThanFifteenMinutes_whenChecking_thenLiftIt() {
        // given
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();
        fail(identifier, origin, 5);
        clock.advance(BLOCK.plusSeconds(1));

        // when
        boolean blocked = throttle.isBlocked(identifier, origin);

        // then
        assertThat(blocked).isFalse();
    }

    @Test
    @DisplayName("failures older than the window do not add up with new ones")
    void givenFourOldAndFourNewFailures_whenChecking_thenDoNotAddThemUp() {
        // given
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();
        fail(identifier, origin, 4);
        clock.advance(WINDOW.plusSeconds(1));
        fail(identifier, origin, 4);

        // when
        boolean blocked = throttle.isBlocked(identifier, origin);

        // then
        assertThat(blocked).isFalse();
    }

    @Test
    @DisplayName("a new window starts after the old one expires, and five failures in it block")
    void givenFiveFailuresInANewWindow_whenChecking_thenBlock() {
        // given
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();
        fail(identifier, origin, 4);
        clock.advance(WINDOW.plusSeconds(1));
        fail(identifier, origin, 5);

        // when
        boolean blocked = throttle.isBlocked(identifier, origin);

        // then
        assertThat(blocked).isTrue();
    }

    @Test
    @DisplayName("a blocked identifier is not blocked from another origin")
    void givenBlockedPair_whenCheckingTheSameIdentifierFromAnotherOrigin_thenDoNotBlock() {
        // given
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();
        fail(identifier, origin, 5);
        assertThat(throttle.isBlocked(identifier, origin)).as("precondition: the pair is blocked").isTrue();

        // when
        boolean blocked = throttle.isBlocked(identifier, uniqueOrigin());

        // then
        assertThat(blocked).isFalse();
    }

    @Test
    @DisplayName("a blocked origin is not blocked for another identifier")
    void givenBlockedPair_whenCheckingAnotherIdentifierFromTheSameOrigin_thenDoNotBlock() {
        // given
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();
        fail(identifier, origin, 5);
        assertThat(throttle.isBlocked(identifier, origin)).as("precondition: the pair is blocked").isTrue();

        // when
        boolean blocked = throttle.isBlocked(uniqueIdentifier(), origin);

        // then
        assertThat(blocked).isFalse();
    }

    @Test
    @DisplayName("clearing the pair resets its count")
    void givenFourFailuresThenCleared_whenFailingFourMoreTimes_thenDoNotBlock() {
        // given
        String identifier = uniqueIdentifier();
        String origin = uniqueOrigin();
        fail(identifier, origin, 4);
        throttle.clear(identifier, origin);

        // when
        fail(identifier, origin, 4);

        // then
        assertThat(throttle.isBlocked(identifier, origin)).isFalse();
    }

    @Test
    @DisplayName("the sixth attempt, even with the right password, gets exactly the generic answer")
    void givenFiveWrongPasswords_whenSigningInWithTheRightOne_thenAnswerExactlyLikeAWrongPassword() throws Exception {
        // given
        // V-13 de ponta a ponta: nada na resposta anuncia o bloqueio.
        String email = registeredEmail();
        MockHttpServletResponse wrong = exhaustAttempts(email);

        // when
        MockHttpServletResponse sixth = signIn(email, PASSWORD);

        // then
        assertThat(sixth.getStatus()).isEqualTo(401);
        assertThat(sixth.getContentAsString()).isEqualTo(wrong.getContentAsString());
        assertThat(sixth.getContentType()).isEqualTo(wrong.getContentType());
    }

    @Test
    @DisplayName("the right password is accepted again once the block ends")
    void givenBlockedAccount_whenSigningInAfterTheBlockEnds_thenAcceptTheRightPassword() throws Exception {
        // given
        String email = registeredEmail();
        exhaustAttempts(email);
        clock.advance(BLOCK.plusSeconds(1));

        // when
        MockHttpServletResponse afterTheBlock = signIn(email, PASSWORD);

        // then
        assertThat(afterTheBlock.getStatus()).isEqualTo(200);
    }
}
