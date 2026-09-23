package io.github.ovyx.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.shared.domain.FixedClock;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Testes da trilha de auditoria de acesso (FR-006). */
@DisplayName("AccessEvent")
class AccessEventTest {

    private static final String IDENTIFIER = "maria.silva@ovyx.com.br";
    private static final String ORIGIN = "203.0.113.42";
    private final FixedClock clock = FixedClock.at("2026-09-19T12:00:00Z");

    @Test
    @DisplayName("records granted access with the identified caretaker")
    void recordsGrantedAccess() {
        CaretakerId caretakerId = CaretakerId.generate();

        AccessEvent event = AccessEvent.granted(IDENTIFIER, caretakerId, ORIGIN, clock);

        assertThat(event.outcome()).isEqualTo(AccessOutcome.GRANTED);
        assertThat(event.caretakerId()).isEqualTo(caretakerId);
        assertThat(event.attemptedIdentifier()).isEqualTo(IDENTIFIER);
        assertThat(event.origin()).isEqualTo(ORIGIN);
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-09-19T12:00:00Z"));
    }

    @Test
    @DisplayName("records invalid credentials without an associated caretaker")
    void recordsInvalidCredentialsWithoutCaretaker() {
        AccessEvent event = AccessEvent.invalidCredentials("nao.existe@ovyx.com.br", null, ORIGIN, clock);

        assertThat(event.outcome()).isEqualTo(AccessOutcome.INVALID_CREDENTIALS);
        assertThat(event.caretakerId()).isNull();
    }

    @Test
    @DisplayName("distinguishes causes only in the audit trail")
    void distinguishesCausesOnlyInTheAuditTrail() {
        // A resposta ao cliente e sempre a mesma (FR-002). Aqui, porem, precisamos saber o que
        // realmente aconteceu — senao a trilha de auditoria nao serve para investigar nada.
        CaretakerId caretakerId = CaretakerId.generate();

        assertThat(AccessEvent.invalidCredentials(IDENTIFIER, null, ORIGIN, clock).outcome())
                .isEqualTo(AccessOutcome.INVALID_CREDENTIALS);
        assertThat(AccessEvent.inactiveCaretaker(IDENTIFIER, caretakerId, ORIGIN, clock).outcome())
                .isEqualTo(AccessOutcome.INACTIVE_CARETAKER);
        assertThat(AccessEvent.throttled(IDENTIFIER, caretakerId, ORIGIN, clock).outcome()).isEqualTo(AccessOutcome.THROTTLED);
        assertThat(AccessEvent.signedOut(IDENTIFIER, caretakerId, ORIGIN, clock).outcome())
                .isEqualTo(AccessOutcome.SIGNED_OUT);
    }

    @Test
    @DisplayName("cannot record the password because no factory accepts it")
    void thereIsNoWayToRecordThePassword() {
        // A invariante e estrutural, nao uma verificacao em tempo de execucao: o tipo simplesmente
        // nao tem por onde receber a credencial.
        AccessEvent event = AccessEvent.invalidCredentials(IDENTIFIER, null, ORIGIN, clock);

        assertThat(event.toString()).doesNotContain("GranjaNorte2026");
        assertThat(event.attemptedIdentifier()).isEqualTo(IDENTIFIER);
    }

    @Test
    @DisplayName("requires identifier and origin")
    void requiresIdentifierAndOrigin() {
        assertThatThrownBy(() -> AccessEvent.invalidCredentials(null, null, ORIGIN, clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AccessEvent.invalidCredentials(IDENTIFIER, null, "  ", clock))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("each event has its own identity")
    void eachEventHasItsOwnIdentity() {
        AccessEvent first = AccessEvent.invalidCredentials(IDENTIFIER, null, ORIGIN, clock);
        AccessEvent second = AccessEvent.invalidCredentials(IDENTIFIER, null, ORIGIN, clock);

        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    @DisplayName("records the caretaker on invalid credentials when the account exists")
    void recordsCaretakerOnInvalidCredentialsForExistingAccount() {
        // Senha errada numa conta que existe: a auditoria precisa dizer de quem e a conta, senao um
        // ataque dirigido a uma pessoa fica invisivel entre as tentativas contra contas inexistentes.
        CaretakerId caretakerId = CaretakerId.generate();

        AccessEvent event = AccessEvent.invalidCredentials(IDENTIFIER, caretakerId, ORIGIN, clock);

        assertThat(event.caretakerId()).isEqualTo(caretakerId);
        assertThat(event.outcome()).isEqualTo(AccessOutcome.INVALID_CREDENTIALS);
    }
}
