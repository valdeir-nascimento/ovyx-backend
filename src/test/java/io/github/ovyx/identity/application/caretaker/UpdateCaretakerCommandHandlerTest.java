package io.github.ovyx.identity.application.caretaker;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.fixtures.InMemoryCaretakerRepository;
import io.github.ovyx.identity.domain.FakePasswordHasher;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes da edicao de responsavel pelo administrador (FR-014, FR-019, cenario 7 da Historia 2).
 */
@DisplayName("UpdateCaretakerCommandHandler")
class UpdateCaretakerCommandHandlerTest {

    private static final String MARIA_CPF = "52998224725";
    private static final String MARIA_EMAIL = "maria.silva@ovyx.com.br";
    private static final String MARIA_MOBILE_PHONE = "91988887777";

    private final FixedClock clock = FixedClock.at("2026-09-19T12:00:00Z");
    private final InMemoryCaretakerRepository repository = new InMemoryCaretakerRepository();
    private final UpdateCaretakerCommandHandler handler = new UpdateCaretakerCommandHandler(repository, clock);

    private Caretaker saved(CaretakerTestDataBuilder builder) {
        Caretaker caretaker =
                builder.withHasher(new FakePasswordHasher()).withRoster(repository).withClock(clock).build();
        repository.save(caretaker);
        return caretaker;
    }

    private Caretaker joao() {
        return saved(aCaretaker()
                .withFullName("João Pereira de Souza")
                .withCpf("11144477735")
                .withEmail("joao.pereira@ovyx.com.br")
                .withMobilePhone("91991234567"));
    }

    private static UpdateCaretakerCommand promotion(Caretaker caretaker) {
        return new UpdateCaretakerCommand(
                caretaker.id(),
                caretaker.fullName().value(),
                caretaker.cpf().value(),
                caretaker.email().value(),
                caretaker.mobilePhone().value(),
                Role.ADMINISTRATOR);
    }

    @Test
    @DisplayName("updates the data and the role, and saves them")
    void givenValidData_whenUpdating_thenSaveTheNewDataAndRole() {
        // given
        Caretaker maria = saved(aCaretaker());
        clock.advance(Duration.ofHours(1));
        UpdateCaretakerCommand update = new UpdateCaretakerCommand(
                maria.id(), "Maria Silva Souza", MARIA_CPF, "maria.souza@ovyx.com.br", MARIA_MOBILE_PHONE, Role.ADMINISTRATOR);

        // when
        Result<CaretakerId> result = handler.handle(update);

        // then
        Caretaker updated = repository.findById(result.value()).orElseThrow();
        assertThat(updated.fullName().value()).isEqualTo("Maria Silva Souza");
        assertThat(updated.email().value()).isEqualTo("maria.souza@ovyx.com.br");
        assertThat(updated.role()).isEqualTo(Role.ADMINISTRATOR);
        assertThat(updated.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("fails as not found for an unknown caretaker")
    void givenUnknownCaretaker_whenUpdating_thenFailAsNotFound() {
        // given
        UpdateCaretakerCommand update = new UpdateCaretakerCommand(
                CaretakerId.generate(), "Maria Silva", MARIA_CPF, MARIA_EMAIL, MARIA_MOBILE_PHONE, Role.USER);

        // when
        Result<CaretakerId> result = handler.handle(update);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.CARETAKER_NOT_FOUND.code());
    }

    @Test
    @DisplayName("fails as a validation failure with every invalid field at once")
    void givenEveryFieldInvalid_whenUpdating_thenFailAsValidationWithEveryField() {
        // given
        Caretaker maria = saved(aCaretaker());
        UpdateCaretakerCommand update = new UpdateCaretakerCommand(maria.id(), "", "123", "sem-arroba", "12", null);

        // when
        Result<CaretakerId> result = handler.handle(update);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details().keySet())
                .containsExactly("fullName", "cpf", "email", "mobilePhone", "role");
    }

    @Test
    @DisplayName("fails as a conflict when the email belongs to another active caretaker")
    void givenEmailOfAnotherActiveCaretaker_whenUpdating_thenFailAsConflict() {
        // given
        Caretaker maria = saved(aCaretaker());
        Caretaker joao = joao();
        UpdateCaretakerCommand update = new UpdateCaretakerCommand(
                maria.id(), "Maria Silva", MARIA_CPF, joao.email().value(), MARIA_MOBILE_PHONE, Role.USER);

        // when
        Result<CaretakerId> result = handler.handle(update);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.EMAIL_ALREADY_IN_USE.code());
    }

    @Test
    @DisplayName("refuses demoting the last active administrator, naming the role")
    void givenTheLastActiveAdministrator_whenDemotingIt_thenFailAsConflictOnTheRole() {
        // given
        Caretaker administrator = saved(aCaretaker().withRole(Role.ADMINISTRATOR));
        UpdateCaretakerCommand demotion = new UpdateCaretakerCommand(
                administrator.id(), "Maria Silva", MARIA_CPF, MARIA_EMAIL, MARIA_MOBILE_PHONE, Role.USER);

        // when
        Result<CaretakerId> result = handler.handle(demotion);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.LAST_ADMINISTRATOR.code());
        assertThat(result.error().details()).containsOnlyKeys("role");
        assertThat(repository.findById(administrator.id()).orElseThrow().role()).isEqualTo(Role.ADMINISTRATOR);
    }

    @Test
    @DisplayName("promotes a common user to administrator")
    void givenCommonUser_whenPromoting_thenSaveTheAdministratorRole() {
        // given
        Caretaker joao = joao();

        // when
        handler.handle(promotion(joao));

        // then
        assertThat(repository.findById(joao.id()).orElseThrow().role()).isEqualTo(Role.ADMINISTRATOR);
    }
}
