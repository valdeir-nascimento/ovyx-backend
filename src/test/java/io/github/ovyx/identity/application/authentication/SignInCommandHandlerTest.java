package io.github.ovyx.identity.application.authentication;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.application.CountingSignInThrottle;
import io.github.ovyx.identity.application.InMemoryCaretakerRepository;
import io.github.ovyx.identity.application.RecordingAccessEventRecorder;
import io.github.ovyx.identity.domain.CountingPasswordHasher;
import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.model.AccessOutcome;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
    private static final String WRONG_PASSWORD = "SenhaErrada2026";
    private static final String UNKNOWN = "nao.existe@ovyx.com.br";
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

        maria = aCaretaker()
                .withEmail(EMAIL)
                .withMobilePhone(MOBILE)
                .withPassword(PASSWORD)
                .withHasher(hasher)
                .withClock(clock)
                .build();
        repository.save(maria);
        hasher.reset();
    }

    private Result<CaretakerId> signIn(String identifier, String password) {
        return handler.handle(new SignInCommand(identifier, password, ORIGIN));
    }

    /** Leva a conta de Maria ao limite de tentativas, a partir desta origem. */
    private void blockMaria() {
        for (int attempt = 0; attempt < 5; attempt++) {
            throttle.registerFailure(EMAIL, ORIGIN);
        }
    }

    private void deactivateMaria() {
        maria.deactivate(clock);
        repository.save(maria);
    }

    private void reactivateMaria() {
        maria.reactivate(clock);
        repository.save(maria);
    }

    @Nested
    @DisplayName("success")
    class Success {

        @Test
        @DisplayName("signs in by email and returns only the caretaker identity")
        void givenCorrectEmailAndPassword_whenSigningIn_thenReturnTheCaretakerIdentity() {
            // given — Maria is registered in setUp

            // when
            Result<CaretakerId> result = signIn(EMAIL, PASSWORD);

            // then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.value()).isEqualTo(maria.id());
            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.GRANTED);
        }

        @ParameterizedTest
        @ValueSource(strings = {MOBILE, "(91) 98888-7777"})
        @DisplayName("signs in by mobile phone, formatted or not, as the same account")
        void givenMobilePhoneInAnySpelling_whenSigningIn_thenReturnTheSameCaretaker(String mobilePhone) {
            // given — mobilePhone from @ValueSource

            // when
            Result<CaretakerId> result = signIn(mobilePhone, PASSWORD);

            // then
            assertThat(result.value()).isEqualTo(maria.id());
        }

        @Test
        @DisplayName("records the identifier as typed in the audit trail, never the password")
        void givenFormattedMobilePhone_whenSigningIn_thenAuditTheTypedIdentifierAndNeverThePassword() {
            // given
            String typed = "(91) 98888-7777";

            // when
            signIn(typed, PASSWORD);

            // then
            AccessEvent recorded = recorder.recorded().getFirst();
            assertThat(recorded.attemptedIdentifier()).isEqualTo("(91) 98888-7777");
            assertThat(recorded.toString()).doesNotContain(PASSWORD);
        }
    }

    @Nested
    @DisplayName("audit trail")
    class AuditTrail {

        @Test
        @DisplayName("identifies the caretaker when a wrong password targets an existing account")
        void givenWrongPasswordForExistingAccount_whenSigningIn_thenAuditWhoseAccountItWas() {
            // given — Maria is registered in setUp

            // when
            signIn(EMAIL, WRONG_PASSWORD);

            // then
            assertThat(recorder.recorded().getLast().caretakerId()).isEqualTo(maria.id());
        }

        @Test
        @DisplayName("identifies the caretaker when a throttled attempt targets an existing account")
        void givenThrottledAccount_whenSigningIn_thenAuditWhoseAccountItWas() {
            // given
            blockMaria();

            // when
            signIn(EMAIL, PASSWORD);

            // then
            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.THROTTLED);
            assertThat(recorder.recorded().getLast().caretakerId()).isEqualTo(maria.id());
        }

        @Test
        @DisplayName("leaves the caretaker empty when the identifier matches nobody")
        void givenUnknownIdentifier_whenSigningIn_thenAuditWithoutACaretaker() {
            // given — no account uses UNKNOWN

            // when
            signIn(UNKNOWN, PASSWORD);

            // then
            assertThat(recorder.recorded().getLast().caretakerId()).isNull();
        }
    }

    @Nested
    @DisplayName("failure paths")
    class FailurePaths {

        @Test
        @DisplayName("fails when the identifier does not exist")
        void givenUnknownIdentifier_whenSigningIn_thenFailAsInvalidCredentials() {
            // given — no account uses UNKNOWN

            // when
            Result<CaretakerId> result = signIn(UNKNOWN, PASSWORD);

            // then
            assertThat(result.isFailure()).isTrue();
            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("fails when the password is wrong")
        void givenWrongPassword_whenSigningIn_thenFailAsInvalidCredentials() {
            // given — Maria is registered in setUp

            // when
            Result<CaretakerId> result = signIn(EMAIL, WRONG_PASSWORD);

            // then
            assertThat(result.isFailure()).isTrue();
            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("fails when the caretaker is inactive, even with the right password")
        void givenInactiveCaretaker_whenSigningInWithCorrectPassword_thenFailAndAuditTheRealCause() {
            // given
            deactivateMaria();

            // when
            Result<CaretakerId> result = signIn(EMAIL, PASSWORD);

            // then
            assertThat(result.isFailure()).isTrue();
            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.INACTIVE_CARETAKER);
        }

        @Test
        @DisplayName("fails when the origin is throttled, even with the right password")
        void givenThrottledOrigin_whenSigningInWithCorrectPassword_thenFailAndAuditTheRealCause() {
            // given
            blockMaria();

            // when
            Result<CaretakerId> result = signIn(EMAIL, PASSWORD);

            // then
            assertThat(result.isFailure()).isTrue();
            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.THROTTLED);
        }

        @ParameterizedTest
        @ValueSource(strings = {"x91988887777", "91988887777@qualquer.com"})
        @DisplayName("does not reach an account through an identifier with extra characters")
        void givenIdentifierWithExtraCharacters_whenSigningIn_thenDoNotReachTheAccount(String garbled) {
            // given — garbled from @ValueSource
            // Antes da correcao, a busca descartava tudo que nao fosse digito e x91988887777 entrava
            // na conta de Maria, com uma chave de contencao propria.

            // when
            Result<CaretakerId> result = signIn(garbled, PASSWORD);

            // then
            assertThat(result.isFailure()).isTrue();
        }

        @Test
        @DisplayName("fails without throwing when the identifier is blank")
        void givenBlankIdentifier_whenSigningIn_thenFailWithoutThrowingAndPayOneVerification() {
            // given
            // A borda HTTP ja exige o campo, mas o tratador nao pode depender disso: identificador
            // vazio fazia o AccessEvent lancar excecao, e excecao nao e canal de negocio (principio IV).
            String blank = "   ";

            // when
            Result<CaretakerId> result = signIn(blank, PASSWORD);

            // then
            assertThat(result.isFailure()).isTrue();
            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.INVALID_CREDENTIALS);
            assertThat(hasher.matchesCalls()).isEqualTo(1);
        }

        @Test
        @DisplayName("every failure path produces exactly the same error")
        void givenEveryFailurePath_whenSigningIn_thenReturnExactlyTheSameError() {
            // given
            // FR-002 e SC-002: comparar respostas nao pode revelar quais contas existem.
            ApplicationError inexistent = signIn(UNKNOWN, PASSWORD).error();

            // when
            ApplicationError wrongPassword = signIn(EMAIL, WRONG_PASSWORD).error();
            deactivateMaria();
            ApplicationError inactive = signIn(EMAIL, PASSWORD).error();
            reactivateMaria();
            blockMaria();
            ApplicationError blocked = signIn(EMAIL, PASSWORD).error();

            // then
            assertThat(List.of(wrongPassword, inactive, blocked))
                    .allSatisfy(error -> assertThat(error).isEqualTo(inexistent));
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
        void givenUnknownIdentifier_whenSigningIn_thenPayExactlyOneVerification() {
            // given — no account uses UNKNOWN

            // when
            signIn(UNKNOWN, PASSWORD);

            // then
            assertThat(hasher.matchesCalls()).isEqualTo(1);
            assertThat(hasher.hashCalls()).isZero();
        }

        @Test
        @DisplayName("an inactive caretaker pays exactly one hash verification")
        void givenInactiveCaretaker_whenSigningIn_thenPayExactlyOneVerification() {
            // given
            deactivateMaria();
            hasher.reset();

            // when
            signIn(EMAIL, PASSWORD);

            // then
            assertThat(hasher.matchesCalls()).isEqualTo(1);
        }

        @Test
        @DisplayName("a throttled origin pays exactly one hash verification")
        void givenThrottledOrigin_whenSigningIn_thenPayExactlyOneVerification() {
            // given
            blockMaria();

            // when
            signIn(EMAIL, PASSWORD);

            // then
            assertThat(hasher.matchesCalls()).isEqualTo(1);
        }

        @Test
        @DisplayName("a wrong password pays exactly one hash verification")
        void givenWrongPassword_whenSigningIn_thenPayExactlyOneVerification() {
            // given — Maria is registered in setUp

            // when
            signIn(EMAIL, WRONG_PASSWORD);

            // then
            assertThat(hasher.matchesCalls()).isEqualTo(1);
        }

        @Test
        @DisplayName("a successful sign-in pays exactly one hash verification")
        void givenCorrectCredentials_whenSigningIn_thenPayExactlyOneVerification() {
            // given — Maria is registered in setUp

            // when
            signIn(EMAIL, PASSWORD);

            // then
            assertThat(hasher.matchesCalls()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("throttling")
    class Throttling {

        @Test
        @DisplayName("counts failures and clears them after success")
        void givenTwoFailedAttempts_whenSigningInSuccessfully_thenClearTheFailureCount() {
            // given
            signIn(EMAIL, WRONG_PASSWORD);
            signIn(EMAIL, "OutraErrada2026");
            assertThat(throttle.failureCount(EMAIL, ORIGIN))
                    .as("precondition: both failures counted")
                    .isEqualTo(2);

            // when
            signIn(EMAIL, PASSWORD);

            // then
            assertThat(throttle.failureCount(EMAIL, ORIGIN)).isZero();
        }

        @Test
        @DisplayName("counts every spelling of the same mobile phone under a single key")
        void givenFiveFailuresInDifferentSpellings_whenSigningInAgain_thenRefuseAsThrottled() {
            // given
            // A contencao usa a mesma forma canonica da busca. Cinco grafias do mesmo celular somam
            // cinco falhas contra uma unica chave, e a sexta tentativa e recusada.
            signIn("91988887777", "Errada2026aaa");
            signIn("(91) 98888-7777", "Errada2026bbb");
            signIn(" 91 98888 7777 ", "Errada2026ccc");
            signIn("91.98888.7777", "Errada2026ddd");
            signIn("(91)98888-7777", "Errada2026eee");

            // when
            Result<CaretakerId> sixth = signIn(MOBILE, PASSWORD);

            // then
            assertThat(throttle.failureCount(MOBILE, ORIGIN)).isEqualTo(5);
            assertThat(sixth.isFailure()).isTrue();
            assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.THROTTLED);
        }

        @Test
        @DisplayName("a blocked attempt does not count again")
        void givenBlockedAccount_whenSigningIn_thenDoNotCountTheAttemptAgain() {
            // given
            blockMaria();

            // when
            signIn(EMAIL, PASSWORD);

            // then
            assertThat(throttle.failureCount(EMAIL, ORIGIN)).isEqualTo(5);
        }
    }
}
