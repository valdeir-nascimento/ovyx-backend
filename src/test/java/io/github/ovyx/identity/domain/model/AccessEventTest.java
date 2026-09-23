package io.github.ovyx.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.shared.domain.FixedClock;
import java.time.Clock;
import java.time.Instant;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/** Testes da trilha de auditoria de acesso (FR-006). */
@DisplayName("AccessEvent")
class AccessEventTest {

    private static final String IDENTIFIER = "maria.silva@ovyx.com.br";
    private static final String ORIGIN = "203.0.113.42";
    private final FixedClock clock = FixedClock.at("2026-09-19T12:00:00Z");

    /** Assinatura comum as fabricas de evento, para percorrer as causas num unico teste. */
    @FunctionalInterface
    private interface EventFactory {
        AccessEvent create(String identifier, CaretakerId caretakerId, String origin, Clock clock);
    }

    private static Stream<Arguments> causesOtherThanGranted() {
        return Stream.of(
                Arguments.of((EventFactory) AccessEvent::invalidCredentials, AccessOutcome.INVALID_CREDENTIALS),
                Arguments.of((EventFactory) AccessEvent::inactiveCaretaker, AccessOutcome.INACTIVE_CARETAKER),
                Arguments.of((EventFactory) AccessEvent::throttled, AccessOutcome.THROTTLED),
                Arguments.of((EventFactory) AccessEvent::signedOut, AccessOutcome.SIGNED_OUT));
    }

    @Test
    @DisplayName("records granted access with the identified caretaker")
    void givenIdentifiedCaretaker_whenRecordingGrantedAccess_thenKeepWhoWhereAndWhen() {
        // given
        CaretakerId caretakerId = CaretakerId.generate();

        // when
        AccessEvent event = AccessEvent.granted(IDENTIFIER, caretakerId, ORIGIN, clock);

        // then
        assertThat(event.outcome()).isEqualTo(AccessOutcome.GRANTED);
        assertThat(event.caretakerId()).isEqualTo(caretakerId);
        assertThat(event.attemptedIdentifier()).isEqualTo(IDENTIFIER);
        assertThat(event.origin()).isEqualTo(ORIGIN);
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-09-19T12:00:00Z"));
    }

    @Test
    @DisplayName("records invalid credentials without an associated caretaker")
    void givenUnknownIdentifier_whenRecordingInvalidCredentials_thenLeaveCaretakerEmpty() {
        // given
        String unknown = "nao.existe@ovyx.com.br";

        // when
        AccessEvent event = AccessEvent.invalidCredentials(unknown, null, ORIGIN, clock);

        // then
        assertThat(event.outcome()).isEqualTo(AccessOutcome.INVALID_CREDENTIALS);
        assertThat(event.caretakerId()).isNull();
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("causesOtherThanGranted")
    @DisplayName("distinguishes causes only in the audit trail")
    void givenEachRefusalCause_whenRecording_thenKeepTheRealCauseInTheTrail(
            EventFactory factory, AccessOutcome expected) {
        // given — factory and expected cause from @MethodSource
        // A resposta ao cliente e sempre a mesma (FR-002). Aqui, porem, precisamos saber o que
        // realmente aconteceu — senao a trilha de auditoria nao serve para investigar nada.

        // when
        AccessEvent event = factory.create(IDENTIFIER, CaretakerId.generate(), ORIGIN, clock);

        // then
        assertThat(event.outcome()).isEqualTo(expected);
    }

    @Test
    @DisplayName("cannot record the password because no factory accepts it")
    void givenFailedAttempt_whenRecording_thenKeepTheIdentifierAndNeverThePassword() {
        // given
        // A invariante e estrutural, nao uma verificacao em tempo de execucao: o tipo simplesmente
        // nao tem por onde receber a credencial.
        String attemptedIdentifier = IDENTIFIER;

        // when
        AccessEvent event = AccessEvent.invalidCredentials(attemptedIdentifier, null, ORIGIN, clock);

        // then
        assertThat(event.toString()).doesNotContain("GranjaNorte2026");
        assertThat(event.attemptedIdentifier()).isEqualTo(IDENTIFIER);
    }

    @ParameterizedTest(name = "identifier [{0}], origin [{1}]")
    @CsvSource({", 203.0.113.42", "maria.silva@ovyx.com.br, '  '"})
    @DisplayName("requires identifier and origin")
    void givenMissingIdentifierOrOrigin_whenRecording_thenRejectTheEvent(String identifier, String origin) {
        // given — identifier and origin from @CsvSource

        // when
        ThrowingCallable recording = () -> AccessEvent.invalidCredentials(identifier, null, origin, clock);

        // then
        assertThatThrownBy(recording).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("each event has its own identity")
    void givenTwoIdenticalAttempts_whenRecording_thenGiveEachEventItsOwnIdentity() {
        // given
        AccessEvent first = AccessEvent.invalidCredentials(IDENTIFIER, null, ORIGIN, clock);

        // when
        AccessEvent second = AccessEvent.invalidCredentials(IDENTIFIER, null, ORIGIN, clock);

        // then
        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    @DisplayName("records the caretaker on invalid credentials when the account exists")
    void givenExistingAccount_whenRecordingInvalidCredentials_thenKeepWhoseAccountItWas() {
        // given
        // Senha errada numa conta que existe: a auditoria precisa dizer de quem e a conta, senao um
        // ataque dirigido a uma pessoa fica invisivel entre as tentativas contra contas inexistentes.
        CaretakerId caretakerId = CaretakerId.generate();

        // when
        AccessEvent event = AccessEvent.invalidCredentials(IDENTIFIER, caretakerId, ORIGIN, clock);

        // then
        assertThat(event.caretakerId()).isEqualTo(caretakerId);
        assertThat(event.outcome()).isEqualTo(AccessOutcome.INVALID_CREDENTIALS);
    }
}
