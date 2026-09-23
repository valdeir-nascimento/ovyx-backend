package io.github.ovyx.identity.application.authentication;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.application.InMemoryCaretakerRepository;
import io.github.ovyx.identity.application.RecordingAccessEventRecorder;
import io.github.ovyx.identity.domain.model.AccessOutcome;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
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

    private Caretaker registerMaria() {
        Caretaker maria = aCaretaker().withEmail("maria.silva@ovyx.com.br").withClock(clock).build();
        repository.save(maria);
        return maria;
    }

    @Test
    @DisplayName("records the sign-out and returns the caretaker who left")
    void givenAuthenticatedCaretaker_whenSigningOut_thenAuditItAndReturnWhoLeft() {
        // given
        CaretakerId caretakerId = registerMaria().id();

        // when
        Result<CaretakerId> result = handler.handle(new SignOutCommand(caretakerId, ORIGIN));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isEqualTo(caretakerId);
        assertThat(recorder.lastOutcome()).isEqualTo(AccessOutcome.SIGNED_OUT);
        assertThat(recorder.recorded().getFirst().caretakerId()).isEqualTo(caretakerId);
    }

    @Test
    @DisplayName("records the caretaker email as the identifier, like every other audit event")
    void givenAuthenticatedCaretaker_whenSigningOut_thenAuditTheEmailAsIdentifier() {
        // given
        // Os demais eventos guardam e-mail ou celular no campo identificador. Guardar o UUID aqui
        // obrigava quem consulta a auditoria a tratar o encerramento como caso a parte.
        CaretakerId caretakerId = registerMaria().id();

        // when
        handler.handle(new SignOutCommand(caretakerId, ORIGIN));

        // then
        assertThat(recorder.recorded().getFirst().attemptedIdentifier()).isEqualTo("maria.silva@ovyx.com.br");
    }

    @Test
    @DisplayName("still records the sign-out with the identity when the caretaker is no longer found")
    void givenCaretakerNoLongerFound_whenSigningOut_thenAuditTheIdentityInstead() {
        // given
        // Sair nao pode depender de o cadastro ainda existir: o registro fica, com a identidade.
        CaretakerId unknown = CaretakerId.generate();

        // when
        Result<CaretakerId> result = handler.handle(new SignOutCommand(unknown, ORIGIN));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(recorder.recorded().getFirst().attemptedIdentifier()).isEqualTo(unknown.toString());
    }

    @Test
    @DisplayName("fails explicitly, instead of succeeding with null, when there is no identity")
    void givenNoIdentity_whenSigningOut_thenFailWithoutAuditing() {
        // given
        // O endpoint exige sessao autenticada, entao este caminho indica defeito, nao uso normal.
        // Ainda assim ele precisa virar Failure: Success(null) e o que o principio IV proibe.
        CaretakerId noIdentity = null;

        // when
        Result<CaretakerId> result = handler.handle(new SignOutCommand(noIdentity, ORIGIN));

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().details()).containsKey("caretaker");
        assertThat(recorder.recorded()).isEmpty();
    }
}
