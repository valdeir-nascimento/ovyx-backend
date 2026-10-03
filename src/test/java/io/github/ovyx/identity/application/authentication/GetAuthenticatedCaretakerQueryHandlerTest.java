package io.github.ovyx.identity.application.authentication;

import io.github.ovyx.identity.domain.model.ThemePreference;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes da consulta "quem esta autenticado".
 *
 * <p>Lado de leitura do CQRS: devolve um modelo construido para a tela, sem passar por agregado e
 * sem carregar a senha (principio V).
 */
@DisplayName("GetAuthenticatedCaretakerQueryHandler")
class GetAuthenticatedCaretakerQueryHandlerTest {

    /** Dublê da porta de leitura. */
    private static final class InMemoryCaretakerReadModels implements CaretakerReadModels {
        private final Map<CaretakerId, AuthenticatedCaretaker> byId = new HashMap<>();

        void put(AuthenticatedCaretaker caretaker) {
            byId.put(caretaker.id(), caretaker);
        }

        @Override
        public Optional<AuthenticatedCaretaker> findAuthenticatedById(CaretakerId id) {
            return Optional.ofNullable(byId.get(id));
        }
    }

    private final InMemoryCaretakerReadModels readModels = new InMemoryCaretakerReadModels();
    private final GetAuthenticatedCaretakerQueryHandler handler =
            new GetAuthenticatedCaretakerQueryHandler(readModels);

    @Test
    @DisplayName("returns the read model of the authenticated caretaker")
    void givenExistingCaretaker_whenAskingWhoIsAuthenticated_thenReturnTheReadModel() {
        // given
        CaretakerId id = CaretakerId.generate();
        readModels.put(new AuthenticatedCaretaker(id, "Maria Silva", Role.USER, false, ThemePreference.SYSTEM, 0));

        // when
        Result<AuthenticatedCaretaker> result = handler.handle(new GetAuthenticatedCaretakerQuery(id));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().fullName()).isEqualTo("Maria Silva");
        assertThat(result.value().role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("fails when the session points to a caretaker that no longer exists")
    void givenSessionPointingToMissingCaretaker_whenAskingWhoIsAuthenticated_thenFailAsUnavailable() {
        // given
        CaretakerId noLongerExists = CaretakerId.generate();

        // when
        Result<AuthenticatedCaretaker> result = handler.handle(new GetAuthenticatedCaretakerQuery(noLongerExists));

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.CARETAKER_UNAVAILABLE.code());
    }

    @Test
    @DisplayName("the read model has no way to carry the password")
    void givenExistingCaretaker_whenReturningTheReadModel_thenCarryNoPasswordField() {
        // given
        // Invariante estrutural: AuthenticatedCaretaker nao tem campo de senha nem de hash.
        // Reusar a entidade de dominio como resposta arrastaria o hash ate a borda HTTP.
        CaretakerId id = CaretakerId.generate();
        readModels.put(new AuthenticatedCaretaker(id, "Maria Silva", Role.USER, false, ThemePreference.SYSTEM, 0));

        // when
        AuthenticatedCaretaker model = handler.handle(new GetAuthenticatedCaretakerQuery(id)).value();

        // then
        assertThat(model.toString()).doesNotContain("argon2");
        assertThat(AuthenticatedCaretaker.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("id", "fullName", "role", "mustChangePassword", "theme", "sessionGeneration");
    }

    @Test
    @DisplayName("carries the theme of the caretaker, for the first screen to open in it (011)")
    void givenCaretakerWithTheDarkTheme_whenAskingWhoIsAuthenticated_thenCarryTheTheme() {
        // given
        CaretakerId id = CaretakerId.generate();
        readModels.put(new AuthenticatedCaretaker(id, "Maria Silva", Role.USER, false, ThemePreference.DARK, 0));

        // when
        AuthenticatedCaretaker model = handler.handle(new GetAuthenticatedCaretakerQuery(id)).value();

        // then
        assertThat(model.theme()).isEqualTo(ThemePreference.DARK);
    }

    @Test
    @DisplayName("carries the session generation of the caretaker, for the revalidation of the session (012)")
    void givenCaretakerWhosePasswordWasRecovered_whenAskingWhoIsAuthenticated_thenCarryTheSessionGeneration() {
        // given
        CaretakerId id = CaretakerId.generate();
        readModels.put(new AuthenticatedCaretaker(id, "Maria Silva", Role.USER, false, ThemePreference.SYSTEM, 2));

        // when
        AuthenticatedCaretaker model = handler.handle(new GetAuthenticatedCaretakerQuery(id)).value();

        // then
        assertThat(model.sessionGeneration()).isEqualTo(2);
    }

    @Test
    @DisplayName("refuses a session opened before the password was recovered by the link (012)")
    void givenSessionWithAnOlderGeneration_whenAskingWhoIsAuthenticated_thenFailAsSessionRevoked() {
        // given
        CaretakerId id = CaretakerId.generate();
        readModels.put(new AuthenticatedCaretaker(id, "Maria Silva", Role.USER, false, ThemePreference.SYSTEM, 1));

        // when
        Result<AuthenticatedCaretaker> result = handler.handle(new GetAuthenticatedCaretakerQuery(id, 0));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.UNAUTHENTICATED);
        assertThat(result.error().code()).isEqualTo("SESSION_REVOKED");
        assertThat(result.error().message())
                .isEqualTo("Sua senha foi redefinida e esta sessão foi encerrada. Entre com a nova senha.");
    }

    @Test
    @DisplayName("accepts a session of the current generation, and a question without a session to check")
    void givenCurrentGenerationOrNoneToCheck_whenAskingWhoIsAuthenticated_thenReturnTheReadModel() {
        // given
        CaretakerId id = CaretakerId.generate();
        readModels.put(new AuthenticatedCaretaker(id, "Maria Silva", Role.USER, false, ThemePreference.SYSTEM, 1));

        // when / then
        assertThat(handler.handle(new GetAuthenticatedCaretakerQuery(id, 1)).isSuccess()).isTrue();
        assertThat(handler.handle(new GetAuthenticatedCaretakerQuery(id)).isSuccess()).isTrue();
    }
}
