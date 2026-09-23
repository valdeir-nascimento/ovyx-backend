package io.github.ovyx.identity.application.account;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
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
        maria = aCaretaker().withPassword(CURRENT).withHasher(hasher).withClock(clock).build();
        repository.save(maria);
    }

    private Result<CaretakerId> change(String current, String updated) {
        return handler.handle(new ChangeOwnPasswordCommand(maria.id(), current, updated));
    }

    private Caretaker reloadedMaria() {
        return repository.findById(maria.id()).orElseThrow();
    }

    @Test
    @DisplayName("changes the password and the previous one stops working")
    void givenMatchingCurrentPassword_whenChanging_thenAcceptOnlyTheNewPassword() {
        // given — Maria is registered in setUp with CURRENT

        // when
        Result<CaretakerId> result = change(CURRENT, NEW);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(reloadedMaria().authenticate(CURRENT, hasher)).isFalse();
        assertThat(reloadedMaria().authenticate(NEW, hasher)).isTrue();
    }

    @Test
    @DisplayName("fails when the current password is wrong and keeps the password")
    void givenWrongCurrentPassword_whenChanging_thenFailAndKeepTheOldPassword() {
        // given
        String wrongCurrent = "SenhaErrada2026";

        // when
        Result<CaretakerId> result = change(wrongCurrent, NEW);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().details())
                .hasEntrySatisfying("currentPassword", message -> assertThat(message).contains("incorreta"));
        assertThat(reloadedMaria().authenticate(CURRENT, hasher)).isTrue();
    }

    @Test
    @DisplayName("fails with ALL policy violations at once")
    void givenNewPasswordBreakingTwoRules_whenChanging_thenReportBothViolations() {
        // given
        String shortWithoutDigit = "abc";

        // when
        Result<CaretakerId> result = change(CURRENT, shortWithoutDigit);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().details())
                .hasEntrySatisfying("newPassword", message -> assertThat(message)
                        .contains("ao menos 12 caracteres")
                        .contains("ao menos um dígito"));
    }

    @Test
    @DisplayName("fails as an unavailable caretaker when the caretaker does not exist")
    void givenUnknownCaretaker_whenChanging_thenFailAsUnavailable() {
        // given
        CaretakerId unknown = CaretakerId.generate();

        // when
        Result<CaretakerId> result = handler.handle(new ChangeOwnPasswordCommand(unknown, CURRENT, NEW));

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.CARETAKER_UNAVAILABLE.code());
    }

    @Test
    @DisplayName("refuses the change for an inactive caretaker and keeps the password")
    void givenInactiveCaretaker_whenChanging_thenFailAsUnavailableAndKeepThePassword() {
        // given
        // A sessao pode sobreviver a inativacao. Inativo nao entra no sistema (invariante 4), e
        // trocar a senha por uma sessao antiga seria um jeito de continuar agindo sobre a conta.
        maria.deactivate(clock);
        repository.save(maria);

        // when
        Result<CaretakerId> result = change(CURRENT, NEW);

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.CARETAKER_UNAVAILABLE.code());
        maria.reactivate(clock);
        assertThat(reloadedMaria().authenticate(CURRENT, hasher))
                .as("a senha anterior continua valendo")
                .isTrue();
    }

    @Test
    @DisplayName("reports both missing passwords in a single round")
    void givenBothPasswordsMissing_whenChanging_thenReportBothInASingleRound() {
        // given
        String missingCurrent = null;
        String missingNew = null;

        // when
        Result<CaretakerId> result = change(missingCurrent, missingNew);

        // then
        assertThat(result.error().details())
                .containsOnlyKeys("currentPassword", "newPassword")
                .containsEntry("currentPassword", "Informe a senha atual.")
                .containsEntry("newPassword", "Informe a senha.");
    }

    @Test
    @DisplayName("clears the obligation to change the password")
    void givenSeededAdministratorWithProvisionalPassword_whenChanging_thenClearTheObligation() {
        // given
        Caretaker seeded = aCaretaker()
                .withCpf("11144477735")
                .withEmail("admin@ovyx.com.br")
                .withMobilePhone("91991110000")
                .withPassword("TrocarNoPrimeiroAcesso2026")
                .withRole(Role.ADMINISTRATOR)
                .withPendingPasswordChange()
                .withHasher(hasher)
                .withClock(clock)
                .build();
        repository.save(seeded);

        // when
        handler.handle(new ChangeOwnPasswordCommand(seeded.id(), "TrocarNoPrimeiroAcesso2026", NEW));

        // then
        assertThat(repository.findById(seeded.id()).orElseThrow().mustChangePassword())
                .isFalse();
    }
}
