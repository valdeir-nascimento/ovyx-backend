package io.github.ovyx.identity.application.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.application.InMemoryCaretakerRepository;
import io.github.ovyx.identity.application.RecordingAccessEventRecorder;
import io.github.ovyx.identity.domain.FakePasswordHasher;
import io.github.ovyx.identity.domain.model.AccessOutcome;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes do encerramento de sessao.
 *
 * <p>A destruicao da sessao cabe a apresentacao. Ao caso de uso cabe registrar o evento na
 * auditoria (FR-006) — e nunca devolver {@code Success(null)} (principio IV).
 */
@DisplayName("SignOutCommandHandler")
class SignOutCommandHandlerTest {

    private static final String ORIGIN = "203.0.113.42";

    private final FixedClock clock = FixedClock.at("2026-09-19T12:00:00Z");
    private final RecordingAccessEventRecorder recorder = new RecordingAccessEventRecorder();
    private final InMemoryCaretakerRepository repository = new InMemoryCaretakerRepository();
    private final SignOutCommandHandler handler = new SignOutCommandHandler(repository, recorder, clock);

    @Test
    @DisplayName("records the sign-out and returns the caretaker who left")
    void recordsTheSignOut() {
        CaretakerId caretakerId = registerMaria().id();

        Result<CaretakerId> result = handler.handle(new SignOutCommand(caretakerId, ORIGIN));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isEqualTo(caretakerId);
        assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.SIGNED_OUT);
        assertThat(recorder.recorded().getFirst().caretakerId()).isEqualTo(caretakerId);
    }

    @Test
    @DisplayName("records the caretaker email as the identifier, like every other audit event")
    void recordsTheEmailAsIdentifier() {
        // Os demais eventos guardam e-mail ou celular no campo identificador. Guardar o UUID aqui
        // obrigava quem consulta a auditoria a tratar o encerramento como caso a parte.
        CaretakerId caretakerId = registerMaria().id();

        handler.handle(new SignOutCommand(caretakerId, ORIGIN));

        assertThat(recorder.recorded().getFirst().attemptedIdentifier()).isEqualTo("maria.silva@ovyx.com.br");
    }

    @Test
    @DisplayName("still records the sign-out with the identity when the caretaker is no longer found")
    void fallsBackToIdentityWhenCaretakerIsGone() {
        // Sair nao pode depender de o cadastro ainda existir: o registro fica, com a identidade.
        CaretakerId unknown = CaretakerId.generate();

        Result<CaretakerId> result = handler.handle(new SignOutCommand(unknown, ORIGIN));

        assertThat(result.isSuccess()).isTrue();
        assertThat(recorder.recorded().getFirst().attemptedIdentifier()).isEqualTo(unknown.toString());
    }

    private Caretaker registerMaria() {
        Caretaker maria = Caretaker.register(
                "Maria Silva",
                "52998224725",
                "maria.silva@ovyx.com.br",
                "91988887777",
                "GranjaNorte2026",
                Role.USER,
                false,
                new FakePasswordHasher(),
                clock);
        repository.save(maria);
        return maria;
    }

    @Test
    @DisplayName("fails explicitly, instead of succeeding with null, when there is no identity")
    void failsWhenThereIsNoIdentity() {
        // O endpoint exige sessao autenticada, entao este caminho indica defeito, nao uso normal.
        // Ainda assim ele precisa virar Failure: Success(null) e o que o principio IV proibe.
        Result<CaretakerId> result = handler.handle(new SignOutCommand(null, ORIGIN));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().details()).containsKey("caretaker");
        assertThat(recorder.recorded()).isEmpty();
    }
}
