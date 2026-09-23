package io.github.ovyx.identity.application.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.application.CountingSignInThrottle;
import io.github.ovyx.identity.application.InMemoryCaretakerRepository;
import io.github.ovyx.identity.application.RecordingAccessEventRecorder;
import io.github.ovyx.identity.domain.CountingPasswordHasher;
import io.github.ovyx.identity.domain.model.AccessOutcome;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Testes do caso de uso de entrada no sistema.
 *
 * <p>Cobrem o sucesso e cada caminho de falha do {@code Result} (principio VI), e as tres defesas
 * que os portoes de qualidade mostraram estar faltando: resposta identica (FR-002), custo identico
 * (canal de tempo) e chave de contencao unica por conta (FR-023).
 */
@DisplayName("SignInCommandHandler")
class SignInCommandHandlerTest {

    private static final String EMAIL = "maria.silva@ovyx.com.br";
    private static final String MOBILE = "91988887777";
    private static final String PASSWORD = "GranjaNorte2026";
    private static final String ORIGIN = "203.0.113.42";

    private final CountingPasswordHasher hasher = new CountingPasswordHasher();
    private final FixedClock clock = FixedClock.at("2026-09-19T12:00:00Z");
    private final InMemoryCaretakerRepository repository = new InMemoryCaretakerRepository();
    private final RecordingAccessEventRecorder recorder = new RecordingAccessEventRecorder();
    private final CountingSignInThrottle throttle = new CountingSignInThrottle(5);

    private SignInCommandHandler handler;
    private Caretaker maria;

    @BeforeEach
    void setUp() {
        handler = new SignInCommandHandler(repository, hasher, recorder, throttle, clock);

        maria = Caretaker.register(
                "Maria Silva", "52998224725", EMAIL, MOBILE, PASSWORD, Role.USER, false, hasher, clock);
        repository.save(maria);
        hasher.reset();
    }

    private Result<CaretakerId> signIn(String identifier, String password) {
        return handler.handle(new SignInCommand(identifier, password, ORIGIN));
    }

    private void blockMaria() {
        for (int attempt = 0; attempt < 5; attempt++) {
            throttle.registerFailure(EMAIL, ORIGIN);
        }
    }

    @Nested
    @DisplayName("success")
    class Success {

        @Test
        @DisplayName("signs in by email and returns only the caretaker identity")
        void signsInByEmail() {
            Result<CaretakerId> result = signIn(EMAIL, PASSWORD);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.value()).isEqualTo(maria.id());
            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.GRANTED);
        }

        @Test
        @DisplayName("signs in by mobile phone with the same result")
        void signsInByMobilePhone() {
            assertThat(signIn(MOBILE, PASSWORD).value()).isEqualTo(maria.id());
        }

        @Test
        @DisplayName("accepts a formatted mobile phone, which is the same account")
        void acceptsFormattedMobilePhone() {
            assertThat(signIn("(91) 98888-7777", PASSWORD).value()).isEqualTo(maria.id());
        }

        @Test
        @DisplayName("records the identifier as typed in the audit trail, never the password")
        void auditRecordsTypedIdentifierNeverPassword() {
            signIn("(91) 98888-7777", PASSWORD);

            assertThat(recorder.recorded().getFirst().attemptedIdentifier()).isEqualTo("(91) 98888-7777");
            assertThat(recorder.recorded().getFirst().toString()).doesNotContain(PASSWORD);
        }
    }

    @Nested
    @DisplayName("audit trail")
    class AuditTrail {

        @Test
        @DisplayName("identifies the caretaker when a wrong password targets an existing account")
        void identifiesCaretakerOnWrongPassword() {
            signIn(EMAIL, "SenhaErrada2026");

            assertThat(recorder.recorded().getLast().caretakerId()).isEqualTo(maria.id());
        }

        @Test
        @DisplayName("identifies the caretaker when a throttled attempt targets an existing account")
        void identifiesCaretakerOnThrottledAttempt() {
            blockMaria();

            signIn(EMAIL, PASSWORD);

            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.THROTTLED);
            assertThat(recorder.recorded().getLast().caretakerId()).isEqualTo(maria.id());
        }

        @Test
        @DisplayName("leaves the caretaker empty when the identifier matches nobody")
        void leavesCaretakerEmptyForUnknownIdentifier() {
            signIn("nao.existe@ovyx.com.br", PASSWORD);

            assertThat(recorder.recorded().getLast().caretakerId()).isNull();
        }
    }

    @Nested
    @DisplayName("failure paths")
    class FailurePaths {

        @Test
        @DisplayName("fails when the identifier does not exist")
        void failsWhenIdentifierDoesNotExist() {
            assertThat(signIn("nao.existe@ovyx.com.br", PASSWORD).isFailure()).isTrue();
            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("fails when the password is wrong")
        void failsWhenPasswordIsWrong() {
            assertThat(signIn(EMAIL, "SenhaErrada2026").isFailure()).isTrue();
            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("fails when the caretaker is inactive, even with the right password")
        void failsWhenCaretakerIsInactive() {
            maria.deactivate(clock);
            repository.save(maria);

            assertThat(signIn(EMAIL, PASSWORD).isFailure()).isTrue();
            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.INACTIVE_CARETAKER);
        }

        @Test
        @DisplayName("fails when the origin is throttled, even with the right password")
        void failsWhenThrottled() {
            blockMaria();

            assertThat(signIn(EMAIL, PASSWORD).isFailure()).isTrue();
            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.THROTTLED);
        }

        @Test
        @DisplayName("does not reach an account through an identifier with extra characters")
        void doesNotReachAccountThroughGarbledIdentifier() {
            // Antes da correcao, a busca descartava tudo que nao fosse digito e x91988887777 entrava
            // na conta de Maria, com uma chave de contencao propria.
            assertThat(signIn("x91988887777", PASSWORD).isFailure()).isTrue();
            assertThat(signIn("91988887777@qualquer.com", PASSWORD).isFailure()).isTrue();
        }

        @Test
        @DisplayName("fails without throwing when the identifier is blank")
        void failsWithoutThrowingOnBlankIdentifier() {
            // A borda HTTP ja exige o campo, mas o tratador nao pode depender disso: identificador
            // vazio fazia o AccessEvent lancar excecao, e excecao nao e canal de negocio (principio IV).
            Result<CaretakerId> result = signIn("   ", PASSWORD);

            assertThat(result.isFailure()).isTrue();
            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.INVALID_CREDENTIALS);
            assertThat(hasher.matchesCalls()).isEqualTo(1);
        }

        @Test
        @DisplayName("every failure path produces exactly the same error")
        void everyFailurePathProducesTheSameError() {
            // FR-002 e SC-002: comparar respostas nao pode revelar quais contas existem.
            ApplicationError inexistent = signIn("nao.existe@ovyx.com.br", PASSWORD).error();
            ApplicationError wrongPassword = signIn(EMAIL, "SenhaErrada2026").error();

            maria.deactivate(clock);
            repository.save(maria);
            ApplicationError inactive = signIn(EMAIL, PASSWORD).error();

            maria.reactivate(clock);
            repository.save(maria);
            blockMaria();
            ApplicationError blocked = signIn(EMAIL, PASSWORD).error();

            List<ApplicationError> everyFailure = List.of(inexistent, wrongPassword, inactive, blocked);

            assertThat(everyFailure).allSatisfy(error -> assertThat(error).isEqualTo(inexistent));
        }
    }

    @Nested
    @DisplayName("timing channel")
    class TimingChannel {

        // Cada caminho precisa executar exatamente uma verificacao de hash. Com Argon2 calibrado em
        // centenas de milissegundos, um caminho que pula o hash responde ~20 vezes mais rapido e
        // denuncia a situacao da conta a quem mede o tempo de resposta.

        @Test
        @DisplayName("an unknown identifier pays exactly one hash verification")
        void unknownIdentifierPaysOneVerification() {
            signIn("nao.existe@ovyx.com.br", PASSWORD);

            assertThat(hasher.matchesCalls()).isEqualTo(1);
            assertThat(hasher.hashCalls()).isZero();
        }

        @Test
        @DisplayName("an inactive caretaker pays exactly one hash verification")
        void inactiveCaretakerPaysOneVerification() {
            maria.deactivate(clock);
            repository.save(maria);
            hasher.reset();

            signIn(EMAIL, PASSWORD);

            assertThat(hasher.matchesCalls()).isEqualTo(1);
        }

        @Test
        @DisplayName("a throttled origin pays exactly one hash verification")
        void throttledOriginPaysOneVerification() {
            blockMaria();

            signIn(EMAIL, PASSWORD);

            assertThat(hasher.matchesCalls()).isEqualTo(1);
        }

        @Test
        @DisplayName("a wrong password pays exactly one hash verification")
        void wrongPasswordPaysOneVerification() {
            signIn(EMAIL, "SenhaErrada2026");

            assertThat(hasher.matchesCalls()).isEqualTo(1);
        }

        @Test
        @DisplayName("a successful sign-in pays exactly one hash verification")
        void successPaysOneVerification() {
            signIn(EMAIL, PASSWORD);

            assertThat(hasher.matchesCalls()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("throttling")
    class Throttling {

        @Test
        @DisplayName("counts failures and clears them after success")
        void countsFailuresAndClearsAfterSuccess() {
            signIn(EMAIL, "SenhaErrada2026");
            signIn(EMAIL, "OutraErrada2026");
            assertThat(throttle.failureCount(EMAIL, ORIGIN)).isEqualTo(2);

            signIn(EMAIL, PASSWORD);

            assertThat(throttle.failureCount(EMAIL, ORIGIN)).isZero();
        }

        @Test
        @DisplayName("counts every spelling of the same mobile phone under a single key")
        void countsEverySpellingUnderOneKey() {
            // A contencao usa a mesma forma canonica da busca. Cinco grafias do mesmo celular somam
            // cinco falhas contra uma unica chave, e a sexta tentativa e recusada.
            signIn("91988887777", "Errada2026aaa");
            signIn("(91) 98888-7777", "Errada2026bbb");
            signIn(" 91 98888 7777 ", "Errada2026ccc");
            signIn("91.98888.7777", "Errada2026ddd");
            signIn("(91)98888-7777", "Errada2026eee");

            Result<CaretakerId> sixth = signIn(MOBILE, PASSWORD);

            assertThat(throttle.failureCount(MOBILE, ORIGIN)).isEqualTo(5);
            assertThat(sixth.isFailure()).isTrue();
            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.THROTTLED);
        }

        @Test
        @DisplayName("a blocked attempt does not count again")
        void blockedAttemptDoesNotCountAgain() {
            blockMaria();

            signIn(EMAIL, PASSWORD);

            assertThat(throttle.failureCount(EMAIL, ORIGIN)).isEqualTo(5);
        }
    }
}
