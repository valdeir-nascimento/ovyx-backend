package io.github.ovyx.identity.application.account;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.application.InMemoryCaretakerRepository;
import io.github.ovyx.identity.domain.FakePasswordHasher;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes da troca da propria senha (FR-020).
 *
 * <p>Esta operacao pertence a Historia 4, mas e entregue junto com a Historia 1: o cenario 7 exige
 * que o administrador semeado troque a senha provisoria antes de qualquer outra coisa (FR-025), e
 * sem esta operacao o sistema recem-instalado ficaria inutilizavel.
 */
@DisplayName("ChangeOwnPasswordCommandHandler")
class ChangeOwnPasswordCommandHandlerTest {

    private static final String CURRENT = "GranjaNorte2026";
    private static final String NEW = "PosturaAviario2027";

    private final PasswordHasher hasher = new FakePasswordHasher();
    private final FixedClock clock = FixedClock.at("2026-09-19T12:00:00Z");
    private final InMemoryCaretakerRepository repository = new InMemoryCaretakerRepository();

    private ChangeOwnPasswordCommandHandler handler;
    private Caretaker maria;

    @BeforeEach
    void setUp() {
        handler = new ChangeOwnPasswordCommandHandler(repository, hasher, clock);
        maria = Caretaker.register(
                "Maria Silva",
                "52998224725",
                "maria.silva@ovyx.com.br",
                "91988887777",
                CURRENT,
                Role.USER,
                false,
                hasher,
                clock);
        repository.save(maria);
    }

    private Result<CaretakerId> change(String current, String updated) {
        return handler.handle(new ChangeOwnPasswordCommand(maria.id(), current, updated));
    }

    @Test
    @DisplayName("changes the password and the previous one stops working")
    void changesThePassword() {
        Result<CaretakerId> result = change(CURRENT, NEW);

        assertThat(result.isSuccess()).isTrue();
        Caretaker reloaded = repository.findById(maria.id()).orElseThrow();
        assertThat(reloaded.authenticate(CURRENT, hasher)).isFalse();
        assertThat(reloaded.authenticate(NEW, hasher)).isTrue();
    }

    @Test
    @DisplayName("fails when the current password is wrong and keeps the password")
    void failsWhenCurrentPasswordIsWrong() {
        Result<CaretakerId> result = change("SenhaErrada2026", NEW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().details())
                .hasEntrySatisfying("currentPassword", message -> assertThat(message).contains("incorreta"));
        assertThat(repository.findById(maria.id()).orElseThrow().authenticate(CURRENT, hasher))
                .isTrue();
    }

    @Test
    @DisplayName("fails with ALL policy violations at once")
    void failsWithEveryPolicyViolationAtOnce() {
        Result<CaretakerId> result = change(CURRENT, "abc");

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().details())
                .hasEntrySatisfying("newPassword", message -> assertThat(message)
                        .contains("ao menos 12 caracteres")
                        .contains("ao menos um dígito"));
    }

    @Test
    @DisplayName("fails as an unavailable caretaker when the caretaker does not exist")
    void failsWhenCaretakerDoesNotExist() {
        Result<CaretakerId> result =
                handler.handle(new ChangeOwnPasswordCommand(CaretakerId.generate(), CURRENT, NEW));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.CARETAKER_UNAVAILABLE.code());
    }

    @Test
    @DisplayName("refuses the change for an inactive caretaker and keeps the password")
    void refusesInactiveCaretaker() {
        // A sessao pode sobreviver a inativacao. Inativo nao entra no sistema (invariante 4), e
        // trocar a senha por uma sessao antiga seria um jeito de continuar agindo sobre a conta.
        maria.deactivate(clock);
        repository.save(maria);

        Result<CaretakerId> result = change(CURRENT, NEW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.CARETAKER_UNAVAILABLE.code());
        maria.reactivate(clock);
        assertThat(repository.findById(maria.id()).orElseThrow().authenticate(CURRENT, hasher))
                .as("a senha anterior continua valendo")
                .isTrue();
    }

    @Test
    @DisplayName("reports both missing passwords in a single round")
    void reportsBothMissingPasswordsAtOnce() {
        Result<CaretakerId> result = change(null, null);

        assertThat(result.error().details())
                .containsOnlyKeys("currentPassword", "newPassword")
                .containsEntry("currentPassword", "Informe a senha atual.")
                .containsEntry("newPassword", "Informe a senha.");
    }

    @Test
    @DisplayName("clears the obligation to change the password")
    void clearsTheObligationToChange() {
        Caretaker seeded = Caretaker.register(
                "Administrador do Sistema",
                "11144477735",
                "admin@ovyx.com.br",
                "91991110000",
                "TrocarNoPrimeiroAcesso2026",
                Role.ADMINISTRATOR,
                true,
                hasher,
                clock);
        repository.save(seeded);

        handler.handle(new ChangeOwnPasswordCommand(seeded.id(), "TrocarNoPrimeiroAcesso2026", NEW));

        assertThat(repository.findById(seeded.id()).orElseThrow().mustChangePassword())
                .isFalse();
    }
}
