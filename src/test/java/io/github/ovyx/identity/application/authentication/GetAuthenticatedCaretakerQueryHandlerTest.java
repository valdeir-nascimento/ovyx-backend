package io.github.ovyx.identity.application.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.IdentityErrorCode;
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
    void returnsTheAuthenticatedCaretaker() {
        CaretakerId id = CaretakerId.generate();
        readModels.put(new AuthenticatedCaretaker(id, "Maria Silva", Role.USER, false));

        Result<AuthenticatedCaretaker> result = handler.handle(new GetAuthenticatedCaretakerQuery(id));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().fullName()).isEqualTo("Maria Silva");
        assertThat(result.value().role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("fails when the session points to a caretaker that no longer exists")
    void failsWhenTheCaretakerNoLongerExists() {
        Result<AuthenticatedCaretaker> result =
                handler.handle(new GetAuthenticatedCaretakerQuery(CaretakerId.generate()));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.CARETAKER_UNAVAILABLE.code());
    }

    @Test
    @DisplayName("the read model has no way to carry the password")
    void theReadModelCannotCarryThePassword() {
        // Invariante estrutural: AuthenticatedCaretaker nao tem campo de senha nem de hash.
        // Reusar a entidade de dominio como resposta arrastaria o hash ate a borda HTTP.
        CaretakerId id = CaretakerId.generate();
        readModels.put(new AuthenticatedCaretaker(id, "Maria Silva", Role.USER, false));

        AuthenticatedCaretaker model = handler.handle(new GetAuthenticatedCaretakerQuery(id)).value();

        assertThat(model.toString()).doesNotContain("argon2");
        assertThat(AuthenticatedCaretaker.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("id", "fullName", "role", "mustChangePassword");
    }
}
