package io.github.ovyx.identity.application.caretaker;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.DEFAULT_PASSWORD;
import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.fixtures.InMemoryCaretakerRepository;
import io.github.ovyx.identity.domain.FakePasswordHasher;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes da inativacao de responsavel pelo administrador (FR-018, FR-019, cenario 6 da Historia 2).
 */
@DisplayName("DeactivateCaretakerCommandHandler")
class DeactivateCaretakerCommandHandlerTest {

    private final PasswordHasher hasher = new FakePasswordHasher();
    private final FixedClock clock = FixedClock.at("2026-09-19T12:00:00Z");
    private final InMemoryCaretakerRepository repository = new InMemoryCaretakerRepository();
    private final DeactivateCaretakerCommandHandler handler = new DeactivateCaretakerCommandHandler(repository, clock);

    private Caretaker saved(CaretakerTestDataBuilder builder) {
        Caretaker caretaker = builder.withHasher(hasher).withRoster(repository).withClock(clock).build();
        repository.save(caretaker);
        return caretaker;
    }

    @Test
    @DisplayName("deactivates the caretaker, who can no longer sign in, and keeps the record")
    void givenActiveCaretaker_whenDeactivating_thenKeepTheRecordButRefuseTheSignIn() {
        // given
        Caretaker maria = saved(aCaretaker());

        // when
        Result<CaretakerId> result = handler.handle(new DeactivateCaretakerCommand(maria.id()));

        // then
        Caretaker kept = repository.findById(result.value()).orElseThrow();
        assertThat(kept.status()).isEqualTo(CaretakerStatus.INACTIVE);
        assertThat(kept.authenticate(DEFAULT_PASSWORD, hasher)).isFalse();
    }

    @Test
    @DisplayName("fails as not found for an unknown caretaker")
    void givenUnknownCaretaker_whenDeactivating_thenFailAsNotFound() {
        // given
        DeactivateCaretakerCommand unknown = new DeactivateCaretakerCommand(CaretakerId.generate());

        // when
        Result<CaretakerId> result = handler.handle(unknown);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.CARETAKER_NOT_FOUND.code());
    }

    @Test
    @DisplayName("refuses deactivating the last active administrator, naming the status")
    void givenTheLastActiveAdministrator_whenDeactivating_thenFailAsConflictOnTheStatus() {
        // given
        Caretaker administrator = saved(aCaretaker().withRole(Role.ADMINISTRATOR));

        // when
        Result<CaretakerId> result = handler.handle(new DeactivateCaretakerCommand(administrator.id()));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.LAST_ADMINISTRATOR.code());
        assertThat(result.error().details()).containsOnlyKeys("status");
        assertThat(repository.findById(administrator.id()).orElseThrow().isActive()).isTrue();
    }
}
